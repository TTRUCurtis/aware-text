package com.aware.data.settings

import com.aware.Aware_Preferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsInitializer: SettingsInitializer,
    private val settingsDao: SettingsDao
) {

    fun getSetting(key: String): String {
        return settings[key] ?: ""
    }

    fun setSetting(key: String, value: String) {
        settings[key] = value
        setSettingInStorage(key, value)
    }

    private fun setSettingInStorage(key: String, value: String) {
        settingsDao.insert(key, value)
    }

    val settings by lazy {
        settingsDao.getSettingsFromStorage() ?: settingsInitializer.initializeSettings()
            .also { settingsDao.insertAll(it) }
    }

    fun reset() {
        val deviceId = settings[Aware_Preferences.DEVICE_ID]
        val deviceLabel = settings[Aware_Preferences.DEVICE_LABEL]
        clearAndReinitialize(deviceId!!, deviceLabel)
    }

    private fun clearAndReinitialize(deviceId: String, deviceLabel: String?) {
        settings.clear()
        settingsDao.clear()

        //return to default settings
        val defaultSettings = settingsInitializer.initializeSettings()
        defaultSettings[Aware_Preferences.DEVICE_ID] = deviceId
        if (deviceLabel != null) {
            defaultSettings[Aware_Preferences.DEVICE_LABEL] = deviceLabel
        }
        settings.putAll(defaultSettings)
        settingsDao.insertAll(settings)
    }
}