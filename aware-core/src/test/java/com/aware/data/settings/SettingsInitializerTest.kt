package com.aware.data.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SettingsInitializerTest {

    @Mock
    private lateinit var mockAppContext: Context

    @Mock
    private lateinit var mockSharedPrefs: SharedPreferences

    @Mock
    private lateinit var mockPackageManager: PackageManager

    @Mock
    private lateinit var mockPackageInfo: PackageInfo

    @InjectMocks
    private lateinit var classUnderTest: SettingsInitializer

    @Before
    fun setup() {
        whenever(mockAppContext.packageName).thenReturn("com.aware.phone")
        whenever(
            mockAppContext.getSharedPreferences(
                mockAppContext.packageName,
                Application.MODE_PRIVATE
            )
        ).thenReturn(mockSharedPrefs)
        whenever(mockAppContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockSharedPrefs.all).thenReturn(fakeSettings)
        whenever(mockPackageManager.getPackageInfo(mockAppContext.packageName, PackageManager.GET_ACTIVITIES)).thenReturn(mockPackageInfo)
        mockPackageInfo.versionName = "4.0.817.bundle"
    }

    @Test
    fun initializeSettings() {
        val actualSettings = classUnderTest.initializeSettings()

        assertEquals("false", actualSettings["webservice_silent"])
        assertEquals("true", actualSettings["status_bluetooth"])
        assertEquals("4.0.817.bundle", actualSettings["aware_version"])
        assert(actualSettings["device_id"]!!.isNotBlank())
    }
}

private val fakeSettings = hashMapOf(
    "webservice_silent" to "false",
    "status_bluetooth" to "true"
)