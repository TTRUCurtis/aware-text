package com.aware.data.settings

import org.junit.Assert.*

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock


@RunWith(MockitoJUnitRunner::class)
class SettingsRepositoryTest {

    @Mock
    private lateinit var mockSettingsInitializer: SettingsInitializer

    @Mock
    private lateinit var mockSettingsDao: SettingsDao

    private val FAKE_SETTINGS = hashMapOf(
        "aware_version" to Setting("aware_version", "4.0.817.bundle"),
        "webservice_silent" to Setting("webservice_silent", "false"),
        "status_bluetooth" to Setting("status_bluetooth", "false")
    )

    @Test
    fun getSetting() {
        val mockSettingsDao = mock<SettingsDao> {
            on { getSettingsFromStorage() } doReturn FAKE_SETTINGS
        }
    }

    /*
     * Here we need to make sure of the following:
     * if (settings not initialized)
     *  initialize settings
     * else
     *  return settings from storage
     *
     */
    @Test
    fun getSettings() {
    }

    /*
    * Here we need to make sure of the following
    *   - if we already have a device ID, throw an error
    *   - device label is done differently
    *   - otherwise, update if existing
    *   - insert if not existing
    */
    @Test
    fun setSettingInStorage() {
    }
}