package com.aware.data.settings

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.util.Log
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.providers.Aware_Provider
import com.aware.providers.Aware_Provider.Aware_Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/*
* Data Access Object for settings database operations
*/
class SettingsDao @Inject constructor(@ApplicationContext private val appContext: Context) {

    fun insert(key: String, value: String) {
        CoroutineScope(Dispatchers.IO).launch {
            setSettingInStorage(key, value)
        }
    }

    fun insertAll(settings: MutableMap<String, String>) {
        for ((key, value) in settings) {
            setSettingInStorage(key, value)
        }
    }

    private fun setSettingInStorage(
        key: String,
        value: Any
    ) {
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
                if (qry.getString(qry.getColumnIndexOrThrow(Aware_Provider.Aware_Settings.SETTING_VALUE)) != value.toString()) {
                    appContext.contentResolver.update(
                        Aware_Provider.Aware_Settings.CONTENT_URI,
                        setting,
                        Aware_Provider.Aware_Settings.SETTING_ID + "=" + qry.getInt(
                            qry.getColumnIndexOrThrow(Aware_Provider.Aware_Settings.SETTING_ID)
                        ),
                        null
                    )
                    if (Aware.DEBUG) Log.d(
                        Aware.TAG,
                        "Updated: $key=$value in ${appContext.packageName}"
                    )
                }
            } catch (e: SQLiteException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            } catch (e: SQLException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            }
            //insert
        } else {
            try {
                appContext.contentResolver.insert(
                    Aware_Provider.Aware_Settings.CONTENT_URI,
                    setting
                )
                if (Aware.DEBUG) Log.d(Aware.TAG, "Added: $key=$value in ${appContext.packageName}")
            } catch (e: SQLiteException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            } catch (e: SQLException) {
                if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
            }
        }
        if (qry != null && !qry.isClosed) qry.close()
    }

    internal fun getSettingsFromStorage(): MutableMap<String, String>? {
        val settingsCursor: Cursor? = appContext.contentResolver.query(
            Aware_Provider.Aware_Settings.CONTENT_URI, null, null,
            null, null
        )

        val settings: MutableMap<String, String>?
        if (settingsCursor != null && settingsCursor.moveToFirst()) {
            val settingsCount = settingsCursor.count
            settings = HashMap(settingsCount)
            val keyColumnIndex =
                settingsCursor.getColumnIndexOrThrow(Aware_Provider.Aware_Settings.SETTING_KEY)
            val valueColumnIndex =
                settingsCursor.getColumnIndexOrThrow(Aware_Provider.Aware_Settings.SETTING_VALUE)
            for (i in 0 until settingsCount) {
                settingsCursor.moveToPosition(i)
                val key = settingsCursor.getString(keyColumnIndex)
                val value = settingsCursor.getString(valueColumnIndex)
                settings[key] = value
            }
            settingsCursor.close()
        } else settings = null
        return settings
    }

    fun clear() {
        appContext.contentResolver.delete(Aware_Settings.CONTENT_URI, null, null)
    }
}