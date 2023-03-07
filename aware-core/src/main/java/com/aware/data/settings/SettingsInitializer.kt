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

class SettingsInitializer @Inject constructor(@ApplicationContext private val appContext: Context, private val settingsDao: SettingsDao) {

    fun initializeSettings(): MutableMap<String, Setting> {
        val settings = HashMap<String, Setting>()

        val sharedPrefs = appContext.getSharedPreferences(appContext.packageName, Application.MODE_PRIVATE)
        if (sharedPrefs.all.isEmpty() && settings[Aware_Preferences.DEVICE_ID] == null) {
            PreferenceManager.setDefaultValues( //TODO remove this once we switch to https://developer.android.com/develop/ui/views/components/settings
                appContext,
                appContext.packageName,
                Application.MODE_PRIVATE,
                com.aware.R.xml.aware_preferences,
                true
            )
            sharedPrefs.edit().apply()
        } else {
            PreferenceManager.setDefaultValues(
                appContext,
                appContext.packageName,
                Application.MODE_PRIVATE,
                R.xml.aware_preferences,
                false
            )
        }
        val defaults = sharedPrefs.all
        for ((key, value) in defaults) { //TODO we shouldn't need to do this everytime, right? Just use database, no need to use preferences except the first time
            if (settings[key] == null || settings[key]?.value.isNullOrEmpty()) {
                settings[key] = Setting(key, value.toString())
                settingsDao.setSettingInStorage(
                    Setting(key,
                    value.toString())
                ) //default AWARE settings
            }
        }
        if (settings[Aware_Preferences.DEVICE_ID] == null) {
            val uuid = UUID.randomUUID()
            settings[Aware_Preferences.DEVICE_ID] = Setting(Aware_Preferences.DEVICE_ID, uuid.toString())

            settingsDao.setSettingInStorage(
                Setting(Aware_Preferences.DEVICE_ID,
                uuid.toString())
            )
        }
        if (settings[Aware_Preferences.WEBSERVICE_SERVER] == null) {
            val awareFrameworkApi = "https://api.awareframework.com/index.php"
            settings[Aware_Preferences.WEBSERVICE_SERVER] = Setting(Aware_Preferences.WEBSERVICE_SERVER, awareFrameworkApi)
            settingsDao.setSettingInStorage(
                Setting(Aware_Preferences.WEBSERVICE_SERVER,
                awareFrameworkApi)
            )
        }
        try {
            val awareInfo = appContext.packageManager.getPackageInfo(
                appContext.packageName, PackageManager.GET_ACTIVITIES
            )
            settings[Aware_Preferences.AWARE_VERSION] = Setting(Aware_Preferences.AWARE_VERSION, awareInfo.versionName)
            settingsDao.setSettingInStorage(
                Setting(Aware_Preferences.AWARE_VERSION,
                awareInfo.versionName)
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return settings
    }
}