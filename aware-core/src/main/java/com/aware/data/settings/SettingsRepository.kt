package com.aware.data.settings

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(settingsInitializer: SettingsInitializer, private val settingsDao: SettingsDao) {

    fun getSetting(key: String): String {
        return settings[key]?.value ?: ""
    }

    fun setSettingInStorage(setting: Setting) {
        settingsDao.setSettingInStorage(setting)
    }

    val settings by lazy {
        settingsDao.getSettingsFromStorage() ?: settingsInitializer.initializeSettings()
    }
}