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
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

object AwareServerPing {

    private const val PID = "pid"
    private const val PREF_NAME = "aware_server_prefs"

    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_QUIT_URL = "quit_url"
    private const val KEY_DEBUG_URL = "debug_url"
    private const val KEY_DEVICE_INFO = "device_info"
    private const val KEY_PERMISSION_STATUS = "permissions_status"
    private const val KEY_STUDY_ELIGIBILITY_INFO = "study_eligibility_info"

    private var serverUrl: String? = null
    private var quitUrl: String? = null
    private var debugUrl: String? = null
    private var deviceInfo : JSONObject? = null
    private var permissionsStatus: JSONObject? = null
    private var studyEligibilityInfo: JSONObject? = null

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        serverUrl = prefs.getString(KEY_SERVER_URL, null)
        quitUrl = prefs.getString(KEY_QUIT_URL, null)
        debugUrl = prefs.getString(KEY_DEBUG_URL, null)

        prefs.getString(KEY_DEVICE_INFO, null)?.let {
            deviceInfo = JSONObject(it)
        }
        prefs.getString(KEY_PERMISSION_STATUS, null)?.let {
            permissionsStatus = JSONObject(it)
        }
        prefs.getString(KEY_STUDY_ELIGIBILITY_INFO, null)?.let {
            studyEligibilityInfo = JSONObject(it)
        }
    }


    fun getExceptionStackTraceAsString(e: Exception): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        return sw.toString()
    }

    fun setServerUrl(context:Context, url: String?) {
        if(serverUrl == null && url != null) {
            serverUrl = url
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SERVER_URL, url).apply()
        }
    }

    private fun getServerUrl(context: Context): String? {
        if(serverUrl==null) {
            serverUrl = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SERVER_URL, null)
        } else {
            serverUrl
        }

        return serverUrl
    }

    fun setQuitUrl(context: Context, url: String?) {
        if(quitUrl == null && url != null) {
            quitUrl = url
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_QUIT_URL, url).apply()
        }
    }

    private fun getQuitUrl(context: Context): String? {
        if(quitUrl==null) {
            quitUrl = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(quitUrl, null)
        } else {
            quitUrl
        }

        return quitUrl
    }

    fun setDebugUrl(context: Context, url: String?) {
        if(debugUrl == null && url != null) {
            debugUrl = url
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_DEBUG_URL, url).apply()
        }
    }

    private fun getDebugUrl(context: Context): String? {
        if(debugUrl==null) {
            debugUrl = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEBUG_URL, null)
        } else {
            debugUrl
        }

        return debugUrl
    }

    fun setDeviceInfo(context: Context) {
        if (deviceInfo != null) return

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

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
                        put("device_identifier", androidId)
                    }
                }
            }
        }.getOrNull()

        deviceInfo?.let {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_DEVICE_INFO, it.toString()).apply()
        }
    }

    private fun getDeviceInfo(context: Context): JSONObject? {
        if (deviceInfo == null) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DEVICE_INFO, null)?.let {
                    deviceInfo = JSONObject(it)
                }
        }
        return deviceInfo
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

        permissionsStatus?.let {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_PERMISSION_STATUS, it.toString()).apply()
        }
    }

    private fun getPermissionsStatus(context: Context): JSONObject? {
        if (permissionsStatus == null) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PERMISSION_STATUS, null)?.let {
                    permissionsStatus = JSONObject(it)
                }
        }
        return permissionsStatus
    }

    fun setStudyEligibilityInfo(
        context: Context,
        result: Boolean,
        requiredWordCount: Int,
        actualWordCount: Int,
        requiredMessageCount: Int,
        actualMessageCount: Int,
        deviceLocation: String?
    ) {
        studyEligibilityInfo = JSONObject().apply {
            put("result", result)
            put("req_word_count", requiredWordCount)
            put("actual_word_count", actualWordCount)
            put("req_message_count", requiredMessageCount)
            put("actual_message_count", actualMessageCount)
            put("device_location", deviceLocation)
            put("bluetooth_enabled", BluetoothAdapter.getDefaultAdapter()?.isEnabled ?: false)
        }

        studyEligibilityInfo?.let {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_STUDY_ELIGIBILITY_INFO, it.toString()).apply()
        }
    }

    private fun getStudyEligibilityInfo(context: Context): JSONObject? {
        if (studyEligibilityInfo == null) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_STUDY_ELIGIBILITY_INFO, null)?.let {
                    studyEligibilityInfo = JSONObject(it)
                }
        }
        return studyEligibilityInfo
    }

    fun sendStudyStatusPing(context: Context) {
        val url = getServerUrl(context) ?: return
        val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)
        val permissions = getPermissionsStatus(context) ?: return
        val validPermissionList = permissions.keys()
            .asSequence()
            .filter { it.startsWith("android.permission.") }
            .toList()

        setPermissionsStatus(context, validPermissionList)

        val json = buildJson {
            put(PID, pid)
            put("permission_status", permissions)
        } ?: return

        postToServer(url.let { "$it/update" }, json)
    }

    fun sendStudyRegistrationPing(context: Context) {
        val url = getServerUrl(context) ?: return
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
        val url = getQuitUrl(context) ?: return
        val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)

        val json = buildJson {
            put(PID, pid)
        } ?: return

        postToServer(url, json)
    }

    fun sendEligibilityFailPing(context: Context) {
        val url = getServerUrl(context) ?: return
        val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)
        val info = getStudyEligibilityInfo(context) ?: return
        val deviceInfo = getDeviceInfo(context) ?: return

        val json = buildJson {
            put(PID, pid)
            put("device_info", deviceInfo)
            put("study_eligibility_info", info)
        } ?: return

        postToServer(url, json)
    }

    fun sendDebugPing(context: Context, source: String, message: String) {
        val url = getDebugUrl(context) ?: return
        val pid = Aware.getSetting(context, Aware_Preferences.DEVICE_ID)
        val currentMillis = System.currentTimeMillis()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val readableTime = formatter.format(Date(currentMillis))

        val json = buildJson {
            put(PID, pid)
            put("timestamp", readableTime)
            put("code_source", source)
            put("message", message)
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