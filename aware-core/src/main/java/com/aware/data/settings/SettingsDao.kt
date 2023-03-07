package com.aware.data.settings

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.util.Log
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.providers.Aware_Provider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.HashMap
import javax.inject.Inject

class SettingsDao @Inject constructor(@ApplicationContext private val appContext: Context) {

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
            } catch (e: IllegalStateException) {
                Log.i(Aware.TAG, "This will be thrown the first time through since settings are not yet loaded in memory", e)
            }
        }
        if (key == Aware_Preferences.DEVICE_LABEL && (value as String).isNotEmpty()) {
            val newLabel = ContentValues()
            newLabel.put(Aware_Provider.Aware_Device.LABEL, value)
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
        setting.put(Aware_Provider.Aware_Settings.SETTING_KEY, key)
        setting.put(Aware_Provider.Aware_Settings.SETTING_VALUE, value.toString())

        val qry = appContext.contentResolver.query(
            Aware_Provider.Aware_Settings.CONTENT_URI,
            null,
            Aware_Provider.Aware_Settings.SETTING_KEY + " LIKE '" + key + "'",
            null,
            null
        )
        //update
        if (qry != null && qry.moveToFirst()) {
            try {
                if (qry.getString(qry.getColumnIndex(Aware_Provider.Aware_Settings.SETTING_VALUE)) != value.toString()) {
                    appContext.contentResolver.update(
                        Aware_Provider.Aware_Settings.CONTENT_URI,
                        setting,
                        Aware_Provider.Aware_Settings.SETTING_ID + "=" + qry.getInt(
                            qry.getColumnIndex(Aware_Provider.Aware_Settings.SETTING_ID)
                        ),
                        null
                    )
                    if (Aware.DEBUG) Log.d(Aware.TAG, "Updated: $key=$value in ${appContext.packageName}")
                }
            } catch (e: SQLiteException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            } catch (e: SQLException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            }
            //insert
        } else {
            try {
                appContext.contentResolver.insert(Aware_Provider.Aware_Settings.CONTENT_URI, setting)
                if (Aware.DEBUG) Log.d(Aware.TAG, "Added: $key=$value in ${appContext.packageName}")
            } catch (e: SQLiteException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            } catch (e: SQLException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            }
        }
        if (qry != null && !qry.isClosed) qry.close()
    }

    internal fun getSettingsFromStorage(): MutableMap<String, Setting>? {
        val settingsCursor: Cursor? = appContext.contentResolver.query(
            Aware_Provider.Aware_Settings.CONTENT_URI, null, null,
            null, null
        )

        val settings: MutableMap<String, Setting>?
        if (settingsCursor != null && settingsCursor.moveToFirst()) {
            val settingsCount = settingsCursor.count
            settings = HashMap(settingsCount)
            val keyColumnIndex = settingsCursor.getColumnIndex(Aware_Provider.Aware_Settings.SETTING_KEY)
            val valueColumnIndex = settingsCursor.getColumnIndex(Aware_Provider.Aware_Settings.SETTING_VALUE)
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