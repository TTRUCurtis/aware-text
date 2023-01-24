package com.aware.plugin.google.auth;

import android.Manifest;
import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class Plugin extends Aware_Plugin {

    public static final String ACTION_AWARE_GOOGLE_LOGIN_COMPLETE = "ACTION_AWARE_GOOGLE_LOGIN_COMPLETE";
    public static final String EXTRA_ACCOUNT = "google_account";

    public static ContextProducer contextProducer;
    public static ContentValues accountDetails;

    private static final int GOOGLE_LOGIN_NOTIFICATION_ID = 5675687;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE: Google Login";

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                Intent logged = new Intent(ACTION_AWARE_GOOGLE_LOGIN_COMPLETE);
                logged.putExtra(EXTRA_ACCOUNT, accountDetails);
                sendBroadcast(logged);
            }
        };
        contextProducer = CONTEXT_PRODUCER;

        //REQUIRED_PERMISSIONS.add(Manifest.permission.READ_PHONE_STATE);

        if (!is_google_services_available()) {
            if (DEBUG) Log.e(TAG, "Google Services APIs are not available on this device");
            stopSelf();
        }
    }

    private boolean is_google_services_available() {
        GoogleApiAvailability googleApi = GoogleApiAvailability.getInstance();
        int result = googleApi.isGooglePlayServicesAvailable(this);
        return (result == ConnectionResult.SUCCESS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_LOGIN).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_LOGIN, true);
            } else {
                if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_GOOGLE_LOGIN).equalsIgnoreCase("false")) {
                    Aware.stopPlugin(getApplicationContext(), getPackageName());
                    return START_STICKY;
                }
            }

            String[] projection = new String[]{Provider.Google_Account.EMAIL, Provider.Google_Account.NAME};
            Cursor cursor = getContentResolver().query(Provider.Google_Account.CONTENT_URI, projection, null, null, null);
            if (cursor != null && !cursor.moveToLast()) {
                showGoogleLoginPopup();
                cursor.close();
            }

            if (Aware.isStudy(this)) {
                Account aware_account = Aware.getAWAREAccount(getApplicationContext());
                String authority = Provider.getAuthority(getApplicationContext());
                long frequency = Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;

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

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Provider.getAuthority(this),
                Bundle.EMPTY
        );

        if (notificationManager != null)
            notificationManager.cancel(GOOGLE_LOGIN_NOTIFICATION_ID);

        Aware.setSetting(this, Settings.STATUS_PLUGIN_GOOGLE_LOGIN, false);
    }

    private void showGoogleLoginPopup() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, Aware.AWARE_NOTIFICATION_CHANNEL_GENERAL)
                .setSmallIcon(R.drawable.ic_app_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.noti_desc))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notificationBuilder = Aware.setNotificationProperties(notificationBuilder, Aware.AWARE_NOTIFICATION_IMPORTANCE_GENERAL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationBuilder.setChannelId(Aware.AWARE_NOTIFICATION_CHANNEL_GENERAL);

        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notificationManager.notify(GOOGLE_LOGIN_NOTIFICATION_ID, notification);
    }
}
