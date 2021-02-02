package com.aware.plugin.esm.json;

import android.content.Intent;
import android.util.Log;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Plugin extends Aware_Plugin {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Random random = new Random();

    private final long refreshIntervalMS = TimeUnit.HOURS.toMillis(12);
    private final long refreshRetryBackoffMS = TimeUnit.SECONDS.toMillis(30);
    private final long refreshMaxBackoffMS = TimeUnit.MINUTES.toMillis(5);
    private long lastRefresh = Long.MIN_VALUE;

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::" + getResources().getString(R.string.app_name);
        Log.i(Aware.TAG, "Created ESM JSON plugin");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {
            Log.i(Aware.TAG, "Starting ESM JSON plugin");
            if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_ESM_JSON).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_ESM_JSON, true);
            } else {
                if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_ESM_JSON).equalsIgnoreCase("false")) {
                    Aware.stopPlugin(getApplicationContext(), getPackageName());
                    Log.i(Aware.TAG, "ESM JSON plugin disabled");
                    return START_STICKY;
                }
            }
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    refreshSchedules();
                }
            });
        } else {
            Log.i(Aware.TAG, "Permissions not OK for ESM JSON plugin");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(Aware.TAG, "Shutting down background schedule refresh executor");
        executor.shutdownNow();
    }

    private void refreshSchedules() {
        long now = System.currentTimeMillis();
        if (now < lastRefresh + refreshIntervalMS) {
            long ageMS = now - lastRefresh;
            Log.d(Aware.TAG, "Skipping ESM JSON refresh; last refreshed " + ageMS + "ms ago");
            return;
        }

        String jsonURL = Aware.getSetting(getApplicationContext(), Settings.URL_PLUGIN_ESM_JSON);
        if (jsonURL == null || jsonURL.length() < 1) {
            Log.i(Aware.TAG, "JSON URL not provided; skipping schedule refresh");
            return;
        }

        long retryIntervalMS = refreshRetryBackoffMS;
        while (true) {
            try {
                doRefresh(jsonURL);
                lastRefresh = now;
                return;
            } catch (IOException | JSONException e) {
                Log.e(Aware.TAG, "Failed to download ESM JSON; retrying in " + retryIntervalMS + "ms: " + e, e);
                try {
                    Thread.sleep(retryIntervalMS);
                } catch (InterruptedException ex) {
                    Log.i(Aware.TAG, "ESM JSON thread interrupted; aborting refresh");
                    return;
                }
                retryIntervalMS = Math.min(retryIntervalMS + refreshRetryBackoffMS, refreshMaxBackoffMS);
            }
        }
    }

    private void doRefresh(String jsonURL) throws IOException, JSONException {
        Log.i(Aware.TAG, "Refreshing ESM JSON from " + jsonURL);

        String json = downloadURL(jsonURL);

        JSONArray arr = new JSONArray(json);
        for (int i = 0, n = arr.length(); i < n; ++i) {
            JSONObject entry = arr.getJSONObject(i);
            String scheduleID = entry.getString("schedule_id");
            Log.i(Aware.TAG, "Processing JSON schedule " + scheduleID);

            // The JSON format used by the iOS plugin is slightly different; translate what is
            // given with what the existing classes expect.
            Scheduler.Schedule schedule = new Scheduler.Schedule(scheduleID);

            // iOS supports "add X minutes of random jitter to each scheduled time" for ESM
            // schedules, which differs from the randomization semantics supported by the
            // Android scheduler. If this parameter is supplied, we mimic that behavior by
            // using a random offset for any "minutes" parameter in this schedule.
            int minuteOffset = 0;
            if (entry.has("randomize_schedule")) {
                minuteOffset = random.nextInt(entry.getInt("randomize_schedule"));
                Log.i(Aware.TAG, "Randomized schedule offset: " + minuteOffset);
            }

            if (entry.has("hours")) {
                JSONArray hours = entry.getJSONArray("hours");
                for (int j = 0, z = hours.length(); j < z; ++j) {
                    schedule.addHour(hours.getInt(j));
                }
            }

            if (entry.has("minutes")) {
                JSONArray minutes = entry.getJSONArray("minutes");
                for (int j = 0, z = minutes.length(); j < z; ++j) {
                    schedule.addMinute(minutes.getInt(j) + minuteOffset);
                }
            } else if (minuteOffset > 0) {
                schedule.addMinute(minuteOffset);
            }

            if (entry.has("start_date")) {
                String minDate = entry.getString("start_date");
                schedule.setMinDate(minDate);
            }

            if (entry.has("end_date")) {
                String maxDate = entry.getString("end_date");
                schedule.setMaxDate(maxDate);
            }

            if (entry.has("context")) {
                JSONArray contexts = entry.getJSONArray("context");
                for (int j = 0, z = contexts.length(); j < z; ++j) {
                    schedule.addContext(contexts.getString(j));
                }
            }

            // TODO support expiration
            // Each ESM question supports esm_expiration_threshold and esm_notification_timeout,
            // but ESMs in Android don't have overall timeouts.

            // TODO support notification_title, notification_body
            // These are currently hardcoded in ESM.notifyESM(Context, boolean) though it seems
            // most Android phones don't actually display these values.

            // set the schedule action to trigger the embedded ESM
            schedule.setActionType(Scheduler.ACTION_TYPE_BROADCAST);
            schedule.setActionIntentAction(ESM.ACTION_AWARE_QUEUE_ESM);
            if (entry.has("esms")) {
                JSONArray esms = entry.getJSONArray("esms");
                Log.d(Aware.TAG, "Configured ESMs: " + esms.toString());
                schedule.addActionExtra(ESM.EXTRA_ESM, esms.toString());
            }

            Scheduler.saveSchedule(getApplication(), schedule);
        }

        Log.i(Aware.TAG, "Pulled all schedules from " + jsonURL);
    }

    private static String downloadURL(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) >= 0) {
                    os.write(buf, 0, read);
                }
                return new String(os.toByteArray(), StandardCharsets.UTF_8);
            }
        }
    }
}
