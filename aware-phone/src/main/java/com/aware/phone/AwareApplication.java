package com.aware.phone;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;

import java.util.Map;
import java.util.UUID;

public class AwareApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        initializeSettings(getSharedPreferences("com.aware.phone", Context.MODE_PRIVATE));
    }

    private void initializeSettings(SharedPreferences prefs) {
        if (prefs.getAll().isEmpty() && Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0) {
            PreferenceManager.setDefaultValues(getApplicationContext(), "com.aware.phone", Context.MODE_PRIVATE, com.aware.R.xml.aware_preferences, true);
            prefs.edit().commit();
        } else {
            PreferenceManager.setDefaultValues(getApplicationContext(), "com.aware.phone", Context.MODE_PRIVATE, R.xml.aware_preferences, false);
        }

        Map<String, ?> defaults = prefs.getAll();
        for (Map.Entry<String, ?> entry : defaults.entrySet()) {
            if (Aware.getSetting(getApplicationContext(), entry.getKey(), "com.aware.phone").length() == 0) {
                Aware.setSetting(getApplicationContext(), entry.getKey(), entry.getValue(), "com.aware.phone"); //default AWARE settings
            }
        }

        if (Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID).length() == 0) {
            UUID uuid = UUID.randomUUID();
            Aware.setSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID, uuid.toString(), "com.aware.phone");
        }

        if (Aware.getSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER).length() == 0) {
            Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SERVER, "https://api.awareframework.com/index.php");
        }

        try {
            PackageInfo awareInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_ACTIVITIES);
            Aware.setSetting(getApplicationContext(), Aware_Preferences.AWARE_VERSION, awareInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //Android 8 specific: create notification channels for AWARE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager not_manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel aware_channel = new NotificationChannel(Aware.AWARE_NOTIFICATION_CHANNEL_GENERAL, getResources().getString(com.aware.R.string.app_name), Aware.AWARE_NOTIFICATION_IMPORTANCE_GENERAL);
            aware_channel.setDescription(getResources().getString(com.aware.R.string.channel_general_description));
            aware_channel.enableLights(true);
            aware_channel.setLightColor(Color.BLUE);
            aware_channel.enableVibration(true);
            not_manager.createNotificationChannel(aware_channel);
        }
    }
}
