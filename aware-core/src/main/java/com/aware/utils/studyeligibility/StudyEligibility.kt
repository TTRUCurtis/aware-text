package com.aware.utils.studyeligibility

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.telephony.TelephonyManager
import com.aware.R
import com.aware.ui.PermissionsHandler
import com.aware.utils.sentiment.SentimentAnalysis
import com.aware.utils.serverping.AwareServerPing
import com.google.android.gms.location.*
import android.location.Location
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class StudyEligibility(private val activity: Activity,
                       private val studyConfig: JSONArray?,
                       private val permissionsHandler: PermissionsHandler,
                       private val permissionCallback: PermissionsHandler.PermissionCallback,
                       private val studyRegistrationPermissions: ArrayList<String>) {

    private var smsPluginObject: JSONObject? = null
    private var isSmsPluginEnabled = false
    private var isBluetoothSensorEnabled = false
    private var checkWordsMessagesCount = false
    private var actualMessageCount = 0
    private var actualWordCount = 0
    private var requiredMessageCount = Values.SMS_MESSAGE_COUNT_DEFAULT
    private var requiredWordCount = Values.SMS_WORD_COUNT_DEFAULT
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
    private val studyEligibilityPermissions = arrayListOf(Manifest.permission.ACCESS_FINE_LOCATION)

    init {
        parseStudyConfig()
    }

    object Values {
        const val SMS_MESSAGE_COUNT_DEFAULT = 500
        const val SMS_WORD_COUNT_DEFAULT = 500
        const val PREFS_NAME = "StudyEligibilityPrefs"
        const val PREF_ELIGIBILITY_CHECKED_KEY = "EligibilityChecked"
    }

    private val sharedPreferences by lazy {
        activity.getSharedPreferences(Values.PREFS_NAME, Activity.MODE_PRIVATE)
    }

    fun hasEligibilityBeenChecked(): Boolean {
        return sharedPreferences.getBoolean(Values.PREF_ELIGIBILITY_CHECKED_KEY, false)
    }

    private fun markEligibilityAsChecked() {
        sharedPreferences.edit().putBoolean(Values.PREF_ELIGIBILITY_CHECKED_KEY, true).apply()
    }

    fun markEligibilityAsUnchecked() {
        sharedPreferences.edit().putBoolean(Values.PREF_ELIGIBILITY_CHECKED_KEY, false).apply()
    }

    private fun parseStudyConfig() {

        studyConfig?.let {
            for(i in 0 until studyConfig.length()) {
                val studyConfigObject = studyConfig.getJSONObject(i)
                if (studyConfigObject.has("plugins"))
                    studyConfigObject.getJSONArray("plugins").run {
                        (0 until length()).forEach { index ->
                            getJSONObject(index).let { pluginConfig ->
                                if (pluginConfig.getString("plugin") == "com.aware.plugin.sms") {
                                    smsPluginObject = pluginConfig
                                    isSmsPluginEnabled = true
                                    studyEligibilityPermissions.add(Manifest.permission.READ_SMS)
                                }

                            }
                        }
                    }
                if(studyConfigObject.has("sensors"))
                    studyConfigObject.getJSONArray("sensors")?.run {
                        (0 until length()).forEach { index ->
                            getJSONObject(index).let { sensorConfig ->
                                if(sensorConfig.getString("setting") == "status_bluetooth") {
                                    isBluetoothSensorEnabled = true
                                }
                            }
                        }
                    }
            }
        }

        smsPluginObject?.getJSONArray("settings")?.let { settings ->
            (0 until settings.length()).forEach { index ->
                settings.getJSONObject(index)?.let { setting ->
                    when (setting.getString("setting")) {
                        "plugin_sms_study_eligibility_message_count" -> requiredMessageCount = setting.getInt("value")
                        "plugin_sms_study_eligibility_word_count" -> requiredWordCount = setting.getInt("value")
                    }
                }
            }
        }
        if(requiredMessageCount > 0 || requiredWordCount >0) checkWordsMessagesCount = true
    }

    fun shouldPerformStudyEligibility() = (isSmsPluginEnabled && checkWordsMessagesCount) || isBluetoothSensorEnabled

    fun getWordCount() = requiredWordCount

    fun getMessageCount() = requiredMessageCount

    fun showStudyEligibilityDialog() {

        val message = if(isBluetoothSensorEnabled) {
            ", and keep your bluetooth on for the duration of the study."
        } else {
            "."
        }
        AlertDialog.Builder(activity).apply {
            setTitle("TTRU-AWARE: Study Eligibility Check")
            setMessage(activity.getString(R.string.study_eligibility_rationale) + message)
            setPositiveButton("OK") {_, _ ->
                permissionsHandler.requestPermissions(studyEligibilityPermissions, permissionCallback)
            }
            show()
        }
    }

    fun performStudyEligibilityCheck() {
        val progressDialog = ProgressDialog(activity).apply {
            setCancelable(false)
            setMessage("Performing study eligibility check, please wait.")
            setInverseBackgroundForced(false)
            show()
        }

        CoroutineScope(Dispatchers.IO).launch {
            var isEligible = false
            var bluetoothCheck = false

            if(isSmsPluginEnabled && checkWordsMessagesCount) {
                val smsUri = Uri.parse("content://sms/")
                val mmsUri = Uri.parse("content://mms/")

                val smsCursor = activity.applicationContext.contentResolver.query(
                    smsUri,
                    arrayOf("body"),
                    "type = ?",
                    arrayOf("2"),
                    null
                )

                val smsCount = smsCursor?.count ?: 0


                val mmsCursor = activity.applicationContext.contentResolver.query(
                    mmsUri,
                    null,
                    "msg_box = ?",
                    arrayOf("2"),
                    null
                )

                val mmsCount = mmsCursor?.count ?: 0

                 actualMessageCount = smsCount + mmsCount

                val smsCheck = if (actualMessageCount >= requiredMessageCount) {
                    val smsWordCount = wordsFromSMS(smsCursor)
                    val mmsWordCount = wordsFromMMS(mmsCursor, (requiredWordCount - smsWordCount))
                    actualWordCount = smsWordCount + mmsWordCount
                    (smsWordCount + mmsWordCount) >= requiredWordCount
                } else {
                    false
                }

                if(isBluetoothSensorEnabled) {
                    bluetoothCheck = BluetoothAdapter.getDefaultAdapter().isEnabled
                    isEligible = smsCheck && bluetoothCheck
                } else {
                    isEligible = smsCheck
                }

                smsCursor?.close()
                mmsCursor?.close()
            } else if(isBluetoothSensorEnabled) {
                bluetoothCheck = BluetoothAdapter.getDefaultAdapter().isEnabled
                isEligible = bluetoothCheck
            }
            delay(2000)

            val deviceLocation = (activity.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager).let { it?.networkCountryIso }


            withContext(Dispatchers.Main) {
                markEligibilityAsChecked()
                handleStudyEligibilityResult(isEligible, bluetoothCheck, deviceLocation)
                progressDialog.dismiss()
            }
        }
    }


    private fun handleStudyEligibilityResult(isEligible: Boolean, bluetoothCheck: Boolean, deviceLocation: String?) {

        val resultDialog = AlertDialog.Builder(activity)
        AwareServerPing.setStudyEligibilityInfo(isEligible, requiredWordCount, actualWordCount, requiredMessageCount, actualMessageCount, deviceLocation)
        if(isEligible) {
            resultDialog.setTitle("TTRU-AWARE: Study Eligibility Passed")
            resultDialog.setMessage(R.string.study_eligibility_success)
            resultDialog.setPositiveButton("continue") { _, _ ->

                permissionsHandler.requestPermissions(studyRegistrationPermissions, permissionCallback)
            }
        } else {
            AwareServerPing.sendEligibilityFailPing(activity)
            resultDialog.setTitle("TTRU-AWARE: Study Eligibility Failed")
            if(isBluetoothSensorEnabled && !bluetoothCheck) {
                resultDialog.setMessage(R.string.study_eligibility_fail_bluetooth)
            } else {
                resultDialog.setMessage(R.string.study_eligibility_fail)
            }
            resultDialog.setPositiveButton("continue") {_, _ ->


                val intent = Intent(activity, activity::class.java)
                activity.finish()
                activity.startActivity(intent)
            }

            markEligibilityAsUnchecked()

        }

        resultDialog.show()
    }


    private fun wordsFromSMS(cursor: Cursor?): Int {

        var words = 0
        cursor?.let { c ->
            if(c.moveToFirst()) {
                do {
                    val message = c.getString(c.getColumnIndexOrThrow("body"))
                    val tokens = SentimentAnalysis.tokenizer(message)
                    words += tokens.size
                } while(c.moveToNext() && words <= requiredWordCount)
            }
        }
        return words
    }

    private fun wordsFromMMS(cursor: Cursor?, requiredWordCount: Int): Int {
        var words = 0

        if(requiredWordCount <= 0) return 0

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val mmsId = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                val selectionPart = "mid = ? AND ct = 'text/plain'"
                val partCursor = activity.applicationContext.contentResolver.query(
                    Uri.parse("content://mms/part/"),
                    arrayOf("_id", "text"),
                    selectionPart,
                    arrayOf(mmsId),
                    null
                )

                if (partCursor != null && partCursor.moveToFirst()) {
                    do {
                        val text = partCursor.getString(partCursor.getColumnIndexOrThrow("text"))
                        val tokens = SentimentAnalysis.tokenizer(text)
                        words += tokens.size
                    } while (partCursor.moveToNext() && words <= requiredWordCount)
                    partCursor.close()
                }
            } while (cursor.moveToNext() && words <= requiredWordCount)
        }
        return words
    }
}