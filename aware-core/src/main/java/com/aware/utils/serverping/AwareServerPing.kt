package com.aware.utils.serverping

import android.content.Context
import android.content.pm.PackageManager
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.providers.Aware_Provider.Aware_Device
import com.aware.utils.Https
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

object AwareServerPing {

    private const val PID = "pid"
    private var SERVER_URL: String? = null
    private var DEVICE_INFO: JSONObject? = null
    private var PERMISSIONS_STATUS: JSONObject? = null
    private var STUDY_INFO: JSONObject? = null

    fun sendStudyStatusPing(context: Context) {

        SERVER_URL?.let {
            val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)
            CoroutineScope(Dispatchers.IO).launch {
                Https().dataPOSTJson(SERVER_URL, JSONObject().put(PID, pid), true)
            }
        }
    }

    fun setServerURL(url: String?) {
        url?.let {
            SERVER_URL = it
        }
    }

    fun setDeviceInfo(context: Context) {

         DEVICE_INFO = JSONObject().apply {
             context.contentResolver.query(Aware_Device.CONTENT_URI, null, null, null, null, null).use { cursor ->
                 if(cursor != null && cursor.moveToFirst()) {
                     put(Aware_Device.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID))
                     put(Aware_Device.DEVICE, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.DEVICE)))
                     put(Aware_Device.BRAND, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.BRAND)))
                     put(Aware_Device.MANUFACTURER, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.MANUFACTURER)))
                     put(Aware_Device.MODEL, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.MODEL)))
                     put(Aware_Device.PRODUCT, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.PRODUCT)))
                     put(Aware_Device.RELEASE, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.RELEASE)))
                     put(Aware_Device.SDK, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.SDK)))
                 }
             }
         }
    }

    fun setPermissionsStatus(context: Context, permissions: List<String>) {

        PERMISSIONS_STATUS = JSONObject().apply {
            permissions.forEach { permission ->
                put(permission, context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    fun setStudyInfo(enrolled: Boolean, result: Boolean) {

        STUDY_INFO = JSONObject().apply {
            put("enrolled", enrolled)
            put("result", result)
        }
    }

    fun getRegistrationData(): JSONObject {

         return JSONObject().apply {
             put("Device Info", DEVICE_INFO)
             put("Permissions Status", PERMISSIONS_STATUS)
             put("Study Info", STUDY_INFO)
         }
    }
}