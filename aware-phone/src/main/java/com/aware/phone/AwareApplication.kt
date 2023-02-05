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
import com.aware.utils.loadSettings
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
        val settingsLD = loadSettings(this, prefs)
        Aware.setSettingsLD(settingsLD)

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