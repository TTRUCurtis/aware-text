package com.aware.utils.studyeligibility

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import com.aware.R
import com.aware.ui.PermissionsHandler
import com.aware.utils.sentiment.SentimentAnalysis
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class StudyEligibility(private val activity: Activity,
                       private val studyConfig: JSONArray?,
                       private val permissionsHandler: PermissionsHandler,
                       private val permissionCallback: PermissionsHandler.PermissionCallback,
                       private val permissions: ArrayList<String>) {

    private var smsPluginObject: JSONObject? = null
    private var isSmsPluginEnabled = false
    private var isBluetoothSensorEnabled = false
    private var checkWordsMessagesCount = false
    private var messageCount = Values.SMS_MESSAGE_COUNT_DEFAULT
    private var wordCount = Values.SMS_WORD_COUNT_DEFAULT

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
                        (0 until length()).mapNotNull { index ->
                            getJSONObject(index).let { pluginConfig ->
                                if (pluginConfig.getString("plugin") == "com.aware.plugin.sms") {
                                    smsPluginObject = pluginConfig
                                    isSmsPluginEnabled = true
                                }

                            }
                        }
                    }
                if(studyConfigObject.has("sensors"))
                    studyConfigObject.getJSONArray("sensors")?.run {
                        (0 until length()).mapNotNull { index ->
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
            (0 until settings.length()).mapNotNull { index ->
                settings.getJSONObject(index)?.let { setting ->
                    when (setting.getString("setting")) {
                        "plugin_sms_study_eligibility_message_count" -> messageCount = setting.getInt("value")
                        "plugin_sms_study_eligibility_word_count" -> wordCount = setting.getInt("value")
                    }
                }
            }
        }
    }

    fun shouldPerformStudyEligibility() = (isSmsPluginEnabled && checkWordsMessagesCount) || isBluetoothSensorEnabled

    fun getWordCount() = wordCount

    fun getMessageCount() = messageCount

    fun showStudyEligibilityDialog() {

        val message = if((isSmsPluginEnabled && checkWordsMessagesCount) && isBluetoothSensorEnabled) {
            "Please grant the SMS permission and enable bluetooth and keep it on for the duration of the study."
        } else if(isSmsPluginEnabled && checkWordsMessagesCount) {
            "Please grant the SMS permission."
        } else if(isBluetoothSensorEnabled) {
            "Please enable bluetooth and keep it on for the duration of the study."
        } else {
            ""
        }
        AlertDialog.Builder(activity).apply {
            setTitle("TTRU-AWARE: Study Eligibility Check")
            setMessage("To join the study, TTRU-AWARE must perform an eligibility check on your device. $message")
            setPositiveButton("OK") {_, _ ->
                if(isSmsPluginEnabled && checkWordsMessagesCount) {
                    permissionsHandler.requestPermissions(
                        listOf(Manifest.permission.READ_SMS),
                        permissionCallback
                    )
                } else {
                    performStudyEligibilityCheck()
                }
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
            var smsCheck = false
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

                val totalMessageCount = smsCount + mmsCount

                smsCheck = if (totalMessageCount >= messageCount) {
                    val smsWordCount = wordsFromSMS(smsCursor)
                    val mmsWordCount = wordsFromMMS(mmsCursor, (wordCount - smsWordCount))
                    (smsWordCount + mmsWordCount) >= wordCount
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
            withContext(Dispatchers.Main) {
                markEligibilityAsChecked()
                handleStudyEligibilityResult(isEligible, smsCheck, bluetoothCheck)
                progressDialog.dismiss()
            }
        }
    }

    private fun handleStudyEligibilityResult(isEligible: Boolean, smsCheck: Boolean, bluetoothCheck: Boolean) {

        val resultDialog = AlertDialog.Builder(activity)
        if(isEligible) {
            resultDialog.setTitle("TTRU-AWARE: Study Eligibility Passed")
            resultDialog.setMessage(R.string.study_eligibility_success)
            resultDialog.setPositiveButton("continue") { _, _ ->
                permissionsHandler.requestPermissions(permissions, permissionCallback)
            }
        } else {
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
                } while(c.moveToNext() && words <= wordCount)
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