package com.aware.phone.ui

import android.Manifest
import android.content.Context

import android.os.Build
import androidx.core.content.PermissionChecker
import com.aware.providers.Bluetooth_Provider
import java.util.ArrayList


object PermissionUtils {

    private const val AMBIENT_NOISE = "com.aware.plugin.ambient_noise"
    private const val CONTACTS_LIST = "com.aware.plugin.contacts_list"
    private const val DEVICE_USAGE = "com.aware.plugin.device_usage"
    private const val ESM_JSON = "com.aware.plugin.esm.json"
    private const val ESM_SCHEDULER = "com.aware.plugin.esm.scheduler"
    private const val FITBIT = "com.aware.plugin.fitbit"
    private const val ACTIVITY_REC = "com.aware.plugin.google.activity_recognition"
    private const val AUTH = "com.aware.plugin.google.auth"
    private const val FUSED_LOCATION = "com.aware.plugin.google.fused_location"
    private const val OPENWEATHER = "com.aware.plugin.openweather"
    private const val SENSORTAG = "com.aware.plugin.sensortag"
    private const val SENTIMENT = "com.aware.plugin.sentiment"
    private const val SMS = "com.aware.plugin.sms"
    private const val STUDENTLIFE_AUDIO = "com.aware.plugin.studentlife.audio"
    private const val SENSOR_BLUETOOTH = "status_bluetooth"
    private const val SENSOR_COMMUNICATION = "status_communications_events"
    private const val SENSOR_LOCATION = "status_location_gps"
    private const val SENSOR_TELEPHONY = "status_telephony"
    private const val SENSOR_WIFI = "status_wifi"

    @JvmStatic
    fun getRequiredPermissions(): ArrayList<String> {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            return arrayListOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.GET_ACCOUNTS,
                Manifest.permission.WRITE_SYNC_SETTINGS,
                Manifest.permission.READ_SYNC_SETTINGS,
                Manifest.permission.READ_SYNC_STATS,
                Manifest.permission.FOREGROUND_SERVICE
            )
        }
        return arrayListOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.WRITE_SYNC_SETTINGS,
            Manifest.permission.READ_SYNC_SETTINGS,
            Manifest.permission.READ_SYNC_STATS
        )
    }

    fun getPermissions(pluginOrSensor: String): List<String> {
        when (pluginOrSensor) {
            AMBIENT_NOISE -> return listOf(
                Manifest.permission.RECORD_AUDIO
            )
            CONTACTS_LIST -> return listOf(
                Manifest.permission.READ_CONTACTS
            )
            ESM_JSON -> return listOf(
                Manifest.permission.INTERNET
            )
            ESM_SCHEDULER -> return listOf(
                Manifest.permission.READ_CALENDAR
            )
            AUTH -> return listOf(
                Manifest.permission.READ_PHONE_STATE
            )
            FUSED_LOCATION -> return listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            OPENWEATHER -> return listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            SENSORTAG -> return listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            SMS -> return listOf(
                Manifest.permission.READ_SMS
            )
            STUDENTLIFE_AUDIO -> return listOf(
                Manifest.permission.RECORD_AUDIO
            )
            SENSOR_BLUETOOTH -> return listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            SENSOR_COMMUNICATION -> return listOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG
            )
            SENSOR_LOCATION -> return listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            SENSOR_TELEPHONY -> return listOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            SENSOR_WIFI -> return listOf(
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
            )
            else -> return emptyList()
        }

    }


//    @JvmStatic
//    val requiredPermissions: ArrayList<String>?
//        get() {
//            if (REQUIRED_PERMISSIONS == null) {
//                REQUIRED_PERMISSIONS = ArrayList()
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.ACCESS_WIFI_STATE)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.CAMERA)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.BLUETOOTH)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.BLUETOOTH_ADMIN)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.ACCESS_COARSE_LOCATION)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.ACCESS_FINE_LOCATION)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.READ_PHONE_STATE)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.GET_ACCOUNTS)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.WRITE_SYNC_SETTINGS)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.READ_SYNC_SETTINGS)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.READ_SYNC_STATS)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.READ_SMS)
//                REQUIRED_PERMISSIONS!!.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) REQUIRED_PERMISSIONS!!.add(
//                    Manifest.permission.FOREGROUND_SERVICE
//                )
//            }
//            return REQUIRED_PERMISSIONS
//        }

//    fun checkIfHasRequiredPermissions(context: Context?): Boolean {
//        for (p in requiredPermissions!!) {
//            if (PermissionChecker.checkSelfPermission(
//                    context!!,
//                    p
//                ) != PermissionChecker.PERMISSION_GRANTED
//            ) {
//                return false
//            }
//        }
//        return true
//    }
}