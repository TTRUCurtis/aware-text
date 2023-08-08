package com.aware.plugin.google.activity_recognition;

import android.Manifest;
import android.accounts.Account;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SyncRequest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.tasks.Task;

public class Plugin extends Aware_Plugin {

    public static String ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION =
            "ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION";
    public static String EXTRA_ACTIVITY = "activity";
    public static String EXTRA_CONFIDENCE = "confidence";

    private String permissionNeededErrorMsg = "ACTIVITY_RECOGNITION permission has not been " +
            "granted";
    private static PendingIntent gARPending;

    public static int current_activity = -1;
    public static int current_confidence = -1;

    @Override
    public void onCreate() {
        super.onCreate();

        AUTHORITY = Google_AR_Provider.getAuthority(this);

        TAG = "AWARE::Google Activity Recognition";

        contextBroadcaster.setProvider(AUTHORITY);
        contextBroadcaster.setTag(TAG);

        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                Intent context = new Intent(ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION);
                context.putExtra(EXTRA_ACTIVITY, current_activity);
                context.putExtra(EXTRA_CONFIDENCE, current_confidence);
                sendBroadcast(context);
            }
        };

        if (!is_google_services_available()) {
            if (DEBUG)
                Log.e(TAG, "Google Services is not available on this device.");
        } else {
            Intent gARIntent =
                    new Intent(getApplicationContext(), com.aware.plugin.google.activity_recognition.Algorithm.class);
            gARPending =
                    PendingIntent.getService(getApplicationContext(), 0, gARIntent,
                            PendingIntent.FLAG_MUTABLE);
        }
    }

    /**
     * Allow callback to other applications when data is stored in provider
     */
    private static AWARESensorObserver awareSensor;

    public static void setSensorObserver(AWARESensorObserver observer) {
        awareSensor = observer;
    }

    public static AWARESensorObserver getSensorObserver() {
        return awareSensor;
    }

    /**
     * Callbacks when activities are detected
     */
    public interface AWARESensorObserver {
        void onActivityChanged(ContentValues data);

        void isRunning(int confidence);

        void isWalking(int confidence);

        void isStill(int confidence);

        void isBycicle(int confidence);

        void isVehicle(int confidence);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION)
                    .length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, true);
            } else {
                if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION)
                        .equalsIgnoreCase("false")) {
                    Aware.stopPlugin(getApplicationContext(), getPackageName());
                    return START_STICKY;
                }
            }

            if (Aware.getSetting(this, Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION)
                    .length() == 0) {
                Aware.setSetting(this, Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, 60);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException(permissionNeededErrorMsg);
            }
            Task<Void> task =
                    ActivityRecognition.getClient(getApplicationContext())
                            .requestActivityUpdates(Long.parseLong(Aware.getSetting(getApplicationContext(), Settings.FREQUENCY_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION)) * 1000, gARPending);

            task.addOnSuccessListener(
                    result -> Log.i(TAG, "Activity Updates Api successfully registered"));
            task.addOnFailureListener(
                    e -> Log.e(TAG, "Activity Updates Api could NOT be registered: " + e));

            if (Aware.isStudy(this)) {
                Account aware_account = Aware.getAWAREAccount(getApplicationContext());
                String authority = Google_AR_Provider.getAuthority(getApplicationContext());
                long frequency =
                        Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;

                ContentResolver.setIsSyncable(aware_account, authority, 1);
                ContentResolver.setSyncAutomatically(aware_account, authority, true);
                SyncRequest request = new SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(aware_account, authority)
                        .setExtras(new Bundle()).build();
                ContentResolver.requestSync(request);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Google_AR_Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Google_AR_Provider.getAuthority(this),
                Bundle.EMPTY
        );

        Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_ACTIVITY_RECOGNITION, false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            throw new IllegalStateException(permissionNeededErrorMsg);
        }
        ActivityRecognition.getClient(getApplicationContext())
                .removeActivityUpdates(gARPending);
    }

    private boolean is_google_services_available() {
        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
        int result = googleApi.isGooglePlayServicesAvailable(this);
        return (result == ConnectionResult.SUCCESS);
    }
}
