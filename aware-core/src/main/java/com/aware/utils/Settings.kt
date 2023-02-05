package com.aware.utils

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.preference.PreferenceManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.R
import com.aware.providers.Aware_Provider.Aware_Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashMap

fun loadSettings(context: Context, prefs: SharedPreferences): LiveData<Map<String, String>?> = liveData(Dispatchers.IO) {

    val settings = getSettingsFromStorage(context) ?: HashMap()

    if (prefs.all.isEmpty() && settings[Aware_Preferences.DEVICE_ID].isNullOrEmpty()) {
        PreferenceManager.setDefaultValues(
            context,
            "com.aware.phone",
            Application.MODE_PRIVATE,
            com.aware.R.xml.aware_preferences,
            true
        )
        prefs.edit().apply()
    } else {
        PreferenceManager.setDefaultValues(
            context,
            "com.aware.phone",
            Application.MODE_PRIVATE,
            R.xml.aware_preferences,
            false
        )
    }
    val defaults = prefs.all
    for ((key, value) in defaults) {
        if (settings[key].isNullOrEmpty()) {
            settings[key] = value.toString()
            setSetting(context, key, value.toString(), "com.aware.phone") //default AWARE settings
        }
    }
    if (settings[Aware_Preferences.DEVICE_ID].isNullOrEmpty()) {
        val uuid = UUID.randomUUID()
        settings[Aware_Preferences.DEVICE_ID] = uuid.toString()

        setSetting(
            context,
            Aware_Preferences.DEVICE_ID,
            uuid.toString(),
            "com.aware.phone"
        )
    }
    if (settings[Aware_Preferences.WEBSERVICE_SERVER].isNullOrEmpty()) {
        val awareFrameworkApi = "https://api.awareframework.com/index.php"
        settings[Aware_Preferences.WEBSERVICE_SERVER] = awareFrameworkApi
        setSetting(
            context,
            Aware_Preferences.WEBSERVICE_SERVER,
            awareFrameworkApi, "com.aware.phone")
    }
    try {
        val awareInfo = context.packageManager.getPackageInfo(
            context.packageName, PackageManager.GET_ACTIVITIES
        )
        settings[Aware_Preferences.AWARE_VERSION] = awareInfo.versionName
        setSetting(context, Aware_Preferences.AWARE_VERSION, awareInfo.versionName, "com.aware.phone")
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
    Aware.setSettings(settings)
    emit(settings)
}

@SuppressLint("Range")
fun setSettingAsync(context: Context, key: String, value: Any, package_name: String) {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        setSetting(context, key, value, package_name)
    }
}

@SuppressLint("Range")
private fun setSetting(
    context: Context,
    key: String,
    value: Any,
    package_name: String
) {
    val setting = ContentValues()
    setting.put(Aware_Settings.SETTING_KEY, key)
    setting.put(Aware_Settings.SETTING_VALUE, value.toString())
    setting.put(Aware_Settings.SETTING_PACKAGE_NAME, package_name)

    val qry = context.contentResolver.query(
        Aware_Settings.CONTENT_URI,
        null,
        Aware_Settings.SETTING_KEY + " LIKE '" + key + "' AND " + Aware_Settings.SETTING_PACKAGE_NAME + " LIKE '" + package_name + "'",
        null,
        null
    )
    //update
    if (qry != null && qry.moveToFirst()) {
        try {
            if (qry.getString(qry.getColumnIndex(Aware_Settings.SETTING_VALUE)) != value.toString()) {
                context.contentResolver.update(
                    Aware_Settings.CONTENT_URI,
                    setting,
                    Aware_Settings.SETTING_ID + "=" + qry.getInt(
                        qry.getColumnIndex(Aware_Settings.SETTING_ID)
                    ),
                    null
                )
                if (Aware.DEBUG) Log.d(Aware.TAG, "Updated: $key=$value in $package_name")
            }
        } catch (e: SQLiteException) {
            if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
        } catch (e: SQLException) {
            if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
        }
        //insert
    } else {
        try {
            context.contentResolver.insert(Aware_Settings.CONTENT_URI, setting)
            if (Aware.DEBUG) Log.d(Aware.TAG, "Added: $key=$value in $package_name")
        } catch (e: SQLiteException) {
            if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
        } catch (e: SQLException) {
            if (Aware.DEBUG) Log.d(Aware.TAG, e.message!!)
        }
    }
    if (qry != null && !qry.isClosed) qry.close()
}

fun getSettingsFromStorage(context: Context): MutableMap<String, String>? {
    val settingsCursor: Cursor? = context.contentResolver.query(
        Aware_Settings.CONTENT_URI, null, null,
        null, null
    )

    val settings: MutableMap<String, String>?
    if (settingsCursor != null && settingsCursor.moveToFirst()) {
        val settingsCount = settingsCursor.count
        settings = HashMap(settingsCount)
        val keyColumnIndex = settingsCursor.getColumnIndex(Aware_Settings.SETTING_KEY)
        val valueColumnIndex = settingsCursor.getColumnIndex(Aware_Settings.SETTING_VALUE)
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