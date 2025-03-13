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
    private var messageCount: Int
    private var wordCount: Int

    init {
        isSmsPluginEnabled = false
        messageCount = Values.SMS_MESSAGE_COUNT_DEFAULT
        wordCount = Values.SMS_WORD_COUNT_DEFAULT
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

    interface EligibilityCheckCallback {
        fun onEligibilityChecked(isEligible: Boolean)
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
                        "plugin_sms_study_eligibility_message_count" -> messageCount = 270
                        "plugin_sms_study_eligibility_word_count" -> wordCount = 560
                    }
                }
            }
        }
    }

    fun isSmsPluginEnabled() = isSmsPluginEnabled

    fun getWordCount() = wordCount

    fun getMessageCount() = messageCount

    fun showSMSPermissionDialog(permissionsHandler: PermissionsHandler, permissionCallback:PermissionsHandler.PermissionCallback) {
        AlertDialog.Builder(activity).apply {
            setTitle("TTRU-AWARE: Study Eligibility Check")
            setMessage("To join study, TTRU-AWARE must perform an eligibility check on your device. \n" +
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

            val isEligible = if (totalMessageCount >= messageCount) {
                val smsWordCount = wordsFromSMS(smsCursor)
                val mmsWordCount = wordsFromMMS(mmsCursor, (wordCount - smsWordCount))
                (smsWordCount + mmsWordCount) >= wordCount
            } else {
                false
            }


            smsCursor?.close()
            mmsCursor?.close()

            delay(2000)

            withContext(Dispatchers.Main) {
                markEligibilityAsChecked()
                callback.onEligibilityChecked(isEligible)
                progressDialog.dismiss()
            }
        }
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