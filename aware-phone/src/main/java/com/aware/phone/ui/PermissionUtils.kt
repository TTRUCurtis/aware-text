package com.aware.phone.ui

import android.Manifest

import android.os.Build
import org.json.JSONArray
import org.json.JSONException
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
    private const val SENSOR_COMMUNICATION = "status_communication_events"
    private const val SENSOR_LOCATION = "status_location_gps"
    private const val SENSOR_TELEPHONY = "status_telephony"
    private const val SENSOR_WIFI = "status_wifi"

    @JvmStatic
    private fun getRequiredPermissions(): ArrayList<String> {
        val requiredPermissions = arrayListOf(
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.WRITE_SYNC_SETTINGS,
            Manifest.permission.READ_SYNC_SETTINGS,
            Manifest.permission.READ_SYNC_STATS
        )

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE)
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return requiredPermissions
    }

    @JvmStatic
    fun populatePermissionsList(studyConfig: JSONArray): ArrayList<String> {
        var plugins = JSONArray()
        var sensors = JSONArray()
        for (i in 0 until studyConfig.length()) {
            try {
                val element = studyConfig.getJSONObject(i)
                if (element.has("plugins")) {
                    plugins = element.getJSONArray("plugins")
                }
                if (element.has("sensors")) {
                    sensors = element.getJSONArray("sensors")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        val permissions: ArrayList<String> = ArrayList()
        for (i in 0 until plugins.length()) {
            try {
                val plugin = plugins.getJSONObject(i)
                permissions.addAll(
                    getPermissions(plugin.getString("plugin"))
                )  //sends in "com.aware.plugin...."
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        for (i in 0 until sensors.length()) {
            try {
                val sensor = sensors.getJSONObject(i)
                permissions.addAll(
                    getPermissions(sensor.getString("setting"))
                )
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        permissions.addAll(getRequiredPermissions())

        return ArrayList(permissions.distinct())
    }

    private fun getPermissions(pluginOrSensor: String): List<String> {
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
            ACTIVITY_REC -> return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                listOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
            else -> return emptyList()
        }
    }
}