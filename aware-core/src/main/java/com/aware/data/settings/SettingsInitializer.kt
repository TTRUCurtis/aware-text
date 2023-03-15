package com.aware.data.settings

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import com.aware.Aware_Preferences
import com.aware.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject

class SettingsInitializer @Inject constructor(@ApplicationContext private val appContext: Context) {

    fun initializeSettings(): MutableMap<String, String> {
        PreferenceManager.setDefaultValues( //TODO remove this once we switch to https://developer.android.com/develop/ui/views/components/settings
            appContext,
            appContext.packageName,
            Application.MODE_PRIVATE,
            R.xml.aware_preferences,
            true
        )

        val sharedPrefs =
            appContext.getSharedPreferences(appContext.packageName, Application.MODE_PRIVATE)
        val settings = sharedPrefs.all.mapValues { it.value.toString() }.toMutableMap()
        settings[Aware_Preferences.DEVICE_ID] = UUID.randomUUID().toString()
        try {
            val awareInfo = appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.GET_ACTIVITIES)
            settings[Aware_Preferences.AWARE_VERSION] = awareInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return settings
    }
}