package com.aware.phone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.preference.PreferenceManager
import com.aware.Aware
import com.aware.Aware_Preferences
import dagger.hilt.android.HiltAndroidApp
import java.util.*

/*
 * Required for Hilt dependency injection
 */
@HiltAndroidApp
class AwareApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeSettings(getSharedPreferences("com.aware.phone", MODE_PRIVATE))
    }

    private fun initializeSettings(prefs: SharedPreferences) {
        if (prefs.all.isEmpty() && Aware.getSetting(
                applicationContext,
                Aware_Preferences.DEVICE_ID
            ).isEmpty()
        ) {
            PreferenceManager.setDefaultValues(
                applicationContext,
                "com.aware.phone",
                MODE_PRIVATE,
                com.aware.R.xml.aware_preferences,
                true
            )
            prefs.edit().apply()
        } else {
            PreferenceManager.setDefaultValues(
                applicationContext,
                "com.aware.phone",
                MODE_PRIVATE,
                R.xml.aware_preferences,
                false
            )
        }
        val defaults = prefs.all
        for ((key, value) in defaults) {
            if (Aware.getSetting(applicationContext, key, "com.aware.phone").isEmpty()) {
                Aware.setSetting(
                    applicationContext,
                    key,
                    value,
                    "com.aware.phone"
                ) //default AWARE settings
            }
        }
        if (Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID).isEmpty()) {
            val uuid = UUID.randomUUID()
            Aware.setSetting(
                applicationContext,
                Aware_Preferences.DEVICE_ID,
                uuid.toString(),
                "com.aware.phone"
            )
        }
        if (Aware.getSetting(applicationContext, Aware_Preferences.WEBSERVICE_SERVER).isEmpty()) {
            Aware.setSetting(
                applicationContext,
                Aware_Preferences.WEBSERVICE_SERVER,
                "https://api.awareframework.com/index.php"
            )
        }
        try {
            val awareInfo = applicationContext.packageManager.getPackageInfo(
                applicationContext.packageName,
                PackageManager.GET_ACTIVITIES
            )
            Aware.setSetting(
                applicationContext,
                Aware_Preferences.AWARE_VERSION,
                awareInfo.versionName
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        //Android 8 specific: create notification channels for AWARE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val not_manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val aware_channel = NotificationChannel(
                Aware.AWARE_NOTIFICATION_CHANNEL_GENERAL,
                resources.getString(com.aware.R.string.app_name),
                Aware.AWARE_NOTIFICATION_IMPORTANCE_GENERAL
            )
            aware_channel.description =
                resources.getString(com.aware.R.string.channel_general_description)
            aware_channel.enableLights(true)
            aware_channel.lightColor = Color.BLUE
            aware_channel.enableVibration(true)
            not_manager.createNotificationChannel(aware_channel)
        }
    }
}