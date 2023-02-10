package com.aware.phone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import com.aware.Aware
import com.aware.data.Setting
import com.aware.data.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import java.util.*
import javax.inject.Inject

/*
 * Required for Hilt dependency injection
 */
@HiltAndroidApp
class AwareApplication : Application() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        initializeSettings()
    }

    private fun initializeSettings() {
        Aware.setSettingsRepository(settingsRepository)

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