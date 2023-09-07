package com.aware.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONException
import java.util.ArrayList

class PermissionsHandler(private val activity: Activity) {

    private var permissionCallback: PermissionCallback? = null

    fun requestPermissions(permissions: List<String>, callback: PermissionCallback?) {
        permissionCallback = callback
        val permissionsToRequest: MutableList<String> = ArrayList()
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isEmpty()) {
            permissionCallback!!.onPermissionGranted()
        } else {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest.toTypedArray(),
                RC_PERMISSIONS
            )
        }
    }

    fun handlePermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == RC_PERMISSIONS) {
            val deniedPermissions: MutableList<String> = ArrayList()
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i])
                }
            }
            if (deniedPermissions.isEmpty()) {
                permissionCallback!!.onPermissionGranted()
            } else {
                val showRationale = shouldShowRationale(deniedPermissions)
                if (showRationale) {
                    permissionCallback!!.onPermissionDeniedWithRationale(deniedPermissions)
                } else {
                    permissionCallback!!.onPermissionDenied(deniedPermissions)
                }
            }
        }
    }

    private fun shouldShowRationale(permissions: List<String>): Boolean {
        for (permission in permissions) {
            if (activity.shouldShowRequestPermissionRationale(permission)) {
                return true
            }
        }
        return false
    }

    interface PermissionCallback {
        fun onPermissionGranted()
        fun onPermissionDenied(deniedPermissions: List<String>?)
        fun onPermissionDeniedWithRationale(deniedPermissions: List<String>?)
    }

    companion object {

        const val RC_PERMISSIONS = 112
        const val EXTRA_REQUIRED_PERMISSIONS = "required_permissions"
        const val EXTRA_REDIRECT_ACTIVITY = "redirect_activity"
        const val EXTRA_REDIRECT_SERVICE = "redirect_service"

        object Plugins {
            const val AMBIENT_NOISE = "com.aware.plugin.ambient_noise"
            const val CONTACTS_LIST = "com.aware.plugin.contacts_list"
            const val ESM_JSON = "com.aware.plugin.esm.json"
            const val ESM_SCHEDULER = "com.aware.plugin.esm.scheduler"
            const val ACTIVITY_REC = "com.aware.plugin.google.activity_recognition"
            const val AUTH = "com.aware.plugin.google.auth"
            const val FUSED_LOCATION = "com.aware.plugin.google.fused_location"
            const val OPEN_WEATHER = "com.aware.plugin.openweather"
            const val SENSOR_TAG = "com.aware.plugin.sensortag"
            const val SENTIMENT = "com.aware.plugin.sentiment"
            const val SMS = "com.aware.plugin.sms"
            const val STUDENT_LIFE_AUDIO = "com.aware.plugin.studentlife.audio"
            const val SENSOR_BLUETOOTH = "status_bluetooth"
            const val SENSOR_COMMUNICATION = "status_communication_events"
            const val SENSOR_LOCATION = "status_location_gps"
            const val SENSOR_TELEPHONY = "status_telephony"
            const val SENSOR_WIFI = "status_wifi"
        }

        fun getPermissions(pluginOrSensor: String): List<String> {
            when (pluginOrSensor) {
                Plugins.AMBIENT_NOISE -> return listOf(
                    Manifest.permission.RECORD_AUDIO
                )
                Plugins.CONTACTS_LIST -> return listOf(
                    Manifest.permission.READ_CONTACTS
                )
                Plugins.ESM_JSON -> return listOf(
                    Manifest.permission.INTERNET
                )
                Plugins.ESM_SCHEDULER -> return listOf(
                    Manifest.permission.READ_CALENDAR
                )
                Plugins.AUTH -> return listOf(
                    Manifest.permission.READ_PHONE_STATE
                )
                Plugins.FUSED_LOCATION -> return listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                Plugins.OPEN_WEATHER -> return listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                Plugins.SENSOR_TAG -> return listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
                Plugins.SMS -> return listOf(
                    Manifest.permission.READ_SMS
                )
                Plugins.STUDENT_LIFE_AUDIO -> return listOf(
                    Manifest.permission.RECORD_AUDIO
                )
                Plugins.SENSOR_BLUETOOTH -> return listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
                Plugins.SENSOR_COMMUNICATION -> return listOf(
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG
                )
                Plugins.SENSOR_LOCATION -> return listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                Plugins.SENSOR_TELEPHONY -> return listOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                Plugins.SENSOR_WIFI -> return listOf(
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE
                )
                Plugins.ACTIVITY_REC -> return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    listOf(Manifest.permission.ACTIVITY_RECOGNITION) else emptyList()
                else -> return emptyList()
            }
        }

        fun getRequiredPermissions(): ArrayList<String> {
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
    }



}