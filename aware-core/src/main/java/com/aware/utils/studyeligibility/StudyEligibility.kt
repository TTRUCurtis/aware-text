package com.aware.utils.studyeligibility

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.database.Cursor
import android.net.Uri
import com.aware.ui.PermissionsHandler
import com.aware.utils.sentiment.SentimentAnalysis
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class StudyEligibility(private val activity: Activity) {

    private var smsPluginObject: JSONObject? = null
    private var isSmsPluginEnabled: Boolean
    private var shouldPerformStudyEligibility: Boolean
    private var messageCount: Int
    private var wordCount: Int

    init {
        isSmsPluginEnabled = false
        shouldPerformStudyEligibility = false
        messageCount = Values.SMS_MESSAGE_COUNT_DEFAULT
        wordCount = Values.SMS_WORD_COUNT_DEFAULT
    }

    object Values {
        const val SMS_MESSAGE_COUNT_DEFAULT = 500
        const val SMS_WORD_COUNT_DEFAULT = 500
    }

    interface EligibilityCheckCallback {
        fun onEligibilityChecked(isEligible: Boolean)
    }

    fun checkForSmsPluginStatus(studyConfig: JSONArray?) {

        studyConfig?.let {
            for(i in 0 until studyConfig.length()) {
                val studyConfigObject = studyConfig.getJSONObject(i)
                if(studyConfigObject.has("plugins"))
                    studyConfigObject.getJSONArray("plugins").run {
                        (0 until length()).mapNotNull { index ->
                            getJSONObject(index).let { pluginConfig ->
                                if(pluginConfig.getString("plugin") == "com.aware.plugin.sms"){
                                    smsPluginObject = pluginConfig
                                    isSmsPluginEnabled = true
                                    shouldPerformStudyEligibility = true
                                }

                            }
                        }
                    }
            }
        }
    }

    fun isSmsPluginEnabled() = isSmsPluginEnabled

    fun shouldPerformStudyEligibility() = shouldPerformStudyEligibility

    fun getWordCount() = wordCount

    fun getMessageCount() = messageCount

    fun showSMSPermissionDialog(permissionsHandler: PermissionsHandler, permissionCallback:PermissionsHandler.PermissionCallback) {
        AlertDialog.Builder(activity).apply {
            setTitle("AWARE: Study Eligibility Check")
            setMessage("To join study, AWARE must perform an eligibility check on your device. \n" +
                    "Please grant the following SMS permission to run check")
            setPositiveButton("OK") { _, _ ->
                permissionsHandler.requestPermissions(listOf(Manifest.permission.READ_SMS), permissionCallback)
            }
            show()
        }
    }

    fun performStudyEligibilityCheck(callback: EligibilityCheckCallback) {
        val progressDialog = ProgressDialog(activity).apply {
            setCancelable(false)
            setMessage("Performing study eligibility check, please wait.")
            setInverseBackgroundForced(false)
            show()
        }
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
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
                val isEligible = activity.applicationContext.contentResolver.query(
                    Uri.parse("content://sms/"), null, null, null, null
                )?.use { cursor ->
                    cursor.count >= messageCount || hasEnoughWords(cursor, wordCount)
                } ?: false

                delay(2000)

                callback.onEligibilityChecked(isEligible)
                progressDialog.dismiss()
                CoroutineScope(Dispatchers.Main).cancel()
            }
        }
        shouldPerformStudyEligibility = false
    }

    private fun hasEnoughWords(cursor: Cursor, wordCount: Int): Boolean {

        var words = 0
        if(cursor.moveToFirst()) {
            do {
                val message = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                val tokens = SentimentAnalysis.tokenizer(message)
                words += tokens.size
            } while(cursor.moveToNext() && words <= wordCount)
        }
        return words >= wordCount
    }
}