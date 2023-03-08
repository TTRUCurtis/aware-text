package com.aware.data.settings

import org.junit.Assert.*

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@RunWith(MockitoJUnitRunner::class)
class SettingsRepositoryTest {

    @Mock
    private lateinit var mockSettingsInitializer: SettingsInitializer

    @Mock
    private lateinit var mockSettingsDao: SettingsDao

    @InjectMocks
    private lateinit var classUnderTest: SettingsRepository

    private val fakeSettings = hashMapOf(
        "aware_version" to Setting("aware_version", "4.0.817.bundle"),
        "webservice_silent" to Setting("webservice_silent", "false"),
        "status_bluetooth" to Setting("status_bluetooth", "false")
    )

    @Test
    fun whenSettingsInitialized_getSetting_returnsSettingFromStorage() {
        whenever(mockSettingsDao.getSettingsFromStorage()).thenReturn(fakeSettings)

        val actual = classUnderTest.getSetting("webservice_silent")

        assertEquals("false", actual)
    }

    @Test
    fun whenSettingsNotInitialized_getSettings_returnsSettingFromInitializer() {
        whenever(mockSettingsDao.getSettingsFromStorage()).thenReturn(null)
        whenever(mockSettingsInitializer.initializeSettings()).thenReturn(fakeSettings)

        val actual = classUnderTest.settings

        assertEquals(fakeSettings, actual)
    }

    @Test
    fun setSettingsInStorage() {
        val fakeSetting = Setting("key", "value")

        classUnderTest.setSettingInStorage(fakeSetting)

        verify(mockSettingsDao).setSettingInStorage(fakeSetting)
    }
}