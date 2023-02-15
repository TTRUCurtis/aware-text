package com.aware.data

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.R
import com.aware.providers.Aware_Provider
import com.aware.providers.Aware_Provider.Aware_Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val appContext: Context){

    private val packageName = appContext.packageName

    fun getSetting(key: String): LiveData<String> {
        return Transformations.map(settings) { it[key]?.value ?: "" }
    }

    val settings: LiveData<Map<String, Setting>> =
        liveData(Dispatchers.IO) {

            val settings = getSettingsFromStorage() ?: HashMap()

            val sharedPrefs = appContext.getSharedPreferences(packageName, Application.MODE_PRIVATE)
            if (sharedPrefs.all.isEmpty() && settings[Aware_Preferences.DEVICE_ID] == null) {
                PreferenceManager.setDefaultValues( //TODO remove this once we switch to https://developer.android.com/develop/ui/views/components/settings
                    appContext,
                    packageName,
                    Application.MODE_PRIVATE,
                    com.aware.R.xml.aware_preferences,
                    true
                )
                sharedPrefs.edit().apply()
            } else {
                PreferenceManager.setDefaultValues(
                    appContext,
                    packageName,
                    Application.MODE_PRIVATE,
                    R.xml.aware_preferences,
                    false
                )
            }
            val defaults = sharedPrefs.all
            for ((key, value) in defaults) {
                if (settings[key] == null) {
                    settings[key] = Setting(key, value.toString())
                    setSettingInStorage(
                        key,
                        value.toString()
                    ) //default AWARE settings
                }
            }
            if (settings[Aware_Preferences.DEVICE_ID] == null) {
                val uuid = UUID.randomUUID()
                settings[Aware_Preferences.DEVICE_ID] = Setting(Aware_Preferences.DEVICE_ID, uuid.toString())

                setSettingInStorage(
                    Aware_Preferences.DEVICE_ID,
                    uuid.toString())
            }
            if (settings[Aware_Preferences.WEBSERVICE_SERVER] == null) {
                val awareFrameworkApi = "https://api.awareframework.com/index.php"
                settings[Aware_Preferences.WEBSERVICE_SERVER] = Setting(Aware_Preferences.WEBSERVICE_SERVER, awareFrameworkApi)
                setSettingInStorage(
                    Aware_Preferences.WEBSERVICE_SERVER,
                    awareFrameworkApi
                )
            }
            try {
                val awareInfo = appContext.packageManager.getPackageInfo(
                    appContext.packageName, PackageManager.GET_ACTIVITIES
                )
                settings[Aware_Preferences.AWARE_VERSION] = Setting(Aware_Preferences.AWARE_VERSION, awareInfo.versionName)
                setSettingInStorage(
                    Aware_Preferences.AWARE_VERSION,
                    awareInfo.versionName
                )
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            emit(settings)
        }

    @SuppressLint("Range")
    fun setSettingInStorage(setting: Setting) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            setSettingInStorage(setting.key, setting.value)
        }
    }

    @SuppressLint("Range")
    private fun setSettingInStorage(
        key: String,
        value: Any
    ) {
        //Check if we already have a device ID
        if (key == Aware_Preferences.DEVICE_ID){
            try {
                val setting = Aware.getSetting(appContext, Aware_Preferences.DEVICE_ID)
                if (setting.isNotEmpty()) {
                    Log.d(Aware.TAG, "AWARE UUID: $setting")
                }
            } catch (e: NullPointerException) {
                Log.i(Aware.TAG, "This will be thrown the first time through since settings are not yet loaded in memory", e)
            }
        }
        if (key == Aware_Preferences.DEVICE_LABEL && (value as String).isNotEmpty()) {
            val newLabel = ContentValues()
            newLabel.put(Aware_Provider.Aware_Device.LABEL, value as String)
            appContext.applicationContext.contentResolver.update(
                Aware_Provider.Aware_Device.CONTENT_URI,
                newLabel,
                Aware_Provider.Aware_Device.DEVICE_ID + " LIKE '" + Aware.getSetting(
                    appContext,
                    Aware_Preferences.DEVICE_ID
                ) + "'",
                null
            )
        }
        val setting = ContentValues()
        setting.put(Aware_Settings.SETTING_KEY, key)
        setting.put(Aware_Settings.SETTING_VALUE, value.toString())
        setting.put(Aware_Settings.SETTING_PACKAGE_NAME, packageName) //TODO shouldn't need this anymore

        val qry = appContext.contentResolver.query(
            Aware_Settings.CONTENT_URI,
            null,
            Aware_Settings.SETTING_KEY + " LIKE '" + key + "' AND " + Aware_Settings.SETTING_PACKAGE_NAME + " LIKE '" + packageName + "'",
            null,
            null
        )
        //update
        if (qry != null && qry.moveToFirst()) {
            try {
                if (qry.getString(qry.getColumnIndex(Aware_Settings.SETTING_VALUE)) != value.toString()) {
                    appContext.contentResolver.update(
                        Aware_Settings.CONTENT_URI,
                        setting,
                        Aware_Settings.SETTING_ID + "=" + qry.getInt(
                            qry.getColumnIndex(Aware_Settings.SETTING_ID)
                        ),
                        null
                    )
                    if (Aware.DEBUG) Log.d(Aware.TAG, "Updated: $key=$value in $packageName")
                }
            } catch (e: SQLiteException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            } catch (e: SQLException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            }
            //insert
        } else {
            try {
                appContext.contentResolver.insert(Aware_Settings.CONTENT_URI, setting)
                if (Aware.DEBUG) Log.d(Aware.TAG, "Added: $key=$value in $packageName")
            } catch (e: SQLiteException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            } catch (e: SQLException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            }
        }
        if (qry != null && !qry.isClosed) qry.close()
    }

    private fun getSettingsFromStorage(): MutableMap<String, Setting>? {
        val settingsCursor: Cursor? = appContext.contentResolver.query(
            Aware_Settings.CONTENT_URI, null, null,
            null, null
        )

        val settings: MutableMap<String, Setting>?
        if (settingsCursor != null && settingsCursor.moveToFirst()) {
            val settingsCount = settingsCursor.count
            settings = HashMap(settingsCount)
            val keyColumnIndex = settingsCursor.getColumnIndex(Aware_Settings.SETTING_KEY)
            val valueColumnIndex = settingsCursor.getColumnIndex(Aware_Settings.SETTING_VALUE)
            for (i in 0 until settingsCount) {
                settingsCursor.moveToPosition(i)
                val key = settingsCursor.getString(keyColumnIndex)
                val value = settingsCursor.getString(valueColumnIndex)
                settings[key] = Setting(key, value)
            }
            settingsCursor.close()
        } else settings = null
        return settings
    }
}