package com.aware.utils.serverping

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
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

    private var serverUrl: String? = null
    private var quitUrl: String? = null

    private var deviceInfo : JSONObject? = null
    private var permissionsStatus: JSONObject? = null
    private var studyEligibilityInfo: JSONObject? = null

    fun setServerUrl(url: String?) {
        if(serverUrl == null && url != null) {
            serverUrl = url
        }
    }

    fun setQuitUrl(url: String?) {
        if(quitUrl == null && url != null) {
            quitUrl = url
        }
    }

    fun setDeviceInfo(context: Context) {
        if (deviceInfo != null) return

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val identifier = "device_identifier"

        deviceInfo = kotlin.runCatching {
            JSONObject().apply {
                context.contentResolver.query(Aware_Device.CONTENT_URI, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        put(Aware_Device.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID))
                        put(Aware_Device.DEVICE, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.DEVICE)))
                        put(Aware_Device.BRAND, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.BRAND)))
                        put(Aware_Device.MANUFACTURER, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.MANUFACTURER)))
                        put(Aware_Device.MODEL, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.MODEL)))
                        put(Aware_Device.PRODUCT, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.PRODUCT)))
                        put(Aware_Device.RELEASE, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.RELEASE)))
                        put(Aware_Device.SDK, cursor.getString(cursor.getColumnIndexOrThrow(Aware_Device.SDK)))
                        put(identifier, androidId)
                    }
                }
            }
        }.getOrNull()
    }

    fun setPermissionsStatus(context: Context, permissions: List<String>) {
        permissionsStatus = JSONObject().apply {
            permissions.forEach { permission ->
                val granted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                put(permission, granted)
            }

            if(permissions.any {it.contains("BLUETOOTH", ignoreCase = true) }) {
                put("BLUETOOTH_ENABLED", BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false)
            }
        }
    }

    fun setStudyEligibilityInfo(
        result: Boolean,
        requiredWordCount: Int,
        actualWordCount: Int,
        requiredMessageCount: Int,
        actualMessageCount: Int,
        deviceLocation: String?,
        inUSA: Boolean
    ) {
        studyEligibilityInfo = JSONObject().apply {
            put("result", result)
            put("req_word_count", requiredWordCount)
            put("actual_word_count", actualWordCount)
            put("req_message_count", requiredMessageCount)
            put("actual_message_count", actualMessageCount)
            put("device_location", deviceLocation)
            put("in_usa", inUSA)
            put("bluetooth_enabled", BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false)
        }
    }

    fun sendStudyStatusPing(context: Context) {
        val url = serverUrl?.let { "$it/update" } ?: return
        val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)
        val permissions = permissionsStatus ?: return

        val validPermissionList = permissions.keys()
            .asSequence()
            .filter { it.startsWith("android.permission.") }
            .toList()

        setPermissionsStatus(context, validPermissionList)

        val json = buildJson {
            put(PID, pid)
            put("permission_status", permissions)
        } ?: return

        postToServer(url, json)
    }

    fun sendStudyRegistrationPing(context: Context) {
        val url = serverUrl ?: return
        val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)

        val json = buildJson {
            put(PID, pid)
            put("device_info", deviceInfo)
            put("permission_status", permissionsStatus)
            put("study_eligibility_info", studyEligibilityInfo)
        } ?: return


        postToServer(url, json)
    }

    fun sendQuitStudyPing(context: Context) {
        val url = quitUrl ?: return
        val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)

        val json = buildJson {
            put(PID, pid)
        } ?: return

        postToServer(url, json)
    }

    fun sendEligibilityFailPing(context: Context) {
        val url = serverUrl ?: return
        val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)
        val info = studyEligibilityInfo ?: return
        val deviceInfo = deviceInfo ?: return

        val json = buildJson {
            put(PID, pid)
            put("device_info", deviceInfo)
            put("study_eligibility_info", info)
        } ?: return

        postToServer(url, json)
    }

    private fun postToServer(url: String, json: JSONObject) {
        CoroutineScope(Dispatchers.IO).launch {
            Https().dataPOSTJson(url, json, true)
        }
    }

    private fun buildJson(builderAction: JSONObject.() -> Unit): JSONObject? {
        return runCatching {
            JSONObject().apply(builderAction)
        }.getOrNull()
    }

}