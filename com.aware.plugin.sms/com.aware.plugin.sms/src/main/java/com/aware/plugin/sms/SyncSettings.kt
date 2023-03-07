package com.aware.plugin.sms

import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.aware.Aware
import com.aware.utils.Scheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONException
import java.lang.Exception
import java.text.SimpleDateFormat
import javax.inject.Inject

class SyncSettings @Inject constructor(
    @ApplicationContext private val applicationContext: Context) {

    val SCHEDULER_PLUGIN_SMS = "SCHEDULER_PLUGIN_SMS"
    val ACTION_REFRESH_SMS = "ACTION_REFRESH_SMS"
    private val TAG = SyncSettings::class.java.simpleName

    fun sendFullHistory() =
        Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SEND_FULL_DATA).isNotEmpty() &&
                Aware.getSetting(
                    applicationContext,
                    Settings.PLUGIN_SMS_SEND_FULL_DATA
                ) == "true"

    fun getStartDateTime(): Long {
        if (isStartDateSetByStudy()) {
            return getStartDateFromSettings()
        } else {
            return getParsedTime(Settings.PLUGIN_SMS_SYNC_MIN_SELECT_DATE)
        }
    }

    fun isSettingChecked(setting: String): Boolean{
        return Aware.getSetting(applicationContext, setting).isNotEmpty()
    }

    fun isPluginEnabled(setting: String): Boolean {
        return if (Aware.getSetting(applicationContext, setting)
                .isEmpty()
        ) {
            Aware.setSetting(applicationContext, setting, true)
            true
        } else if (Aware.getSetting(applicationContext, setting)
                .equals("true", ignoreCase = true)
        ) {
            true
        } else {
            Aware.stopPlugin(
                applicationContext,
                applicationContext.packageName //TODO is this the same package as com.aware.plugin.sms?
            )
            false
        }
    }

    fun setLastSyncToDefault() {
        Aware.setSetting(
            applicationContext,
            Settings.PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP,
            Settings.PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP_DEFAULT.toString()
        )
    }

    private fun getStartDateFromSettings() = getParsedTime(
        Aware.getSetting(
            applicationContext,
            Settings.PLUGIN_SMS_STARTDATE
        )
    )

    private fun isStartDateSetByStudy() =
        !(Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_STARTDATE)
            .isEmpty()) &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_STARTDATE) != "")


    fun getParsedTime(timeString: String): Long {
        var retVal: Long = 0
        val timePatterns = mapOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS" to 23,
            "yyyy-MM-dd'T'HH:mm:ss" to 19,
            "yyyy-MM-dd" to 10
        )
        for (pattern in timePatterns) {
            if (pattern.value == timeString.length) {
                val timeParser = SimpleDateFormat(pattern.key)
                try {
                    retVal = timeParser.parse(timeString)!!.getTime()
                    return retVal
                } catch (e: Exception) {
                    // Faield to Parse, drop to Log and return 0L
                }
            }
        }
        Log.e(TAG, "Failure to parse time string:" + timeString);
        return retVal
    }

    private fun getEndDateFromSettings(): Long {
        return getParsedTime(
            Aware.getSetting(
                applicationContext,
                Settings.PLUGIN_SMS_ENDDATE
            )
        )
    }

    fun retrieveOnlySentMessages(): Boolean {
        return (Aware.getSetting(
            applicationContext,
            Settings.STATUS_PLUGIN_SMS_RECEIVED
        ).isEmpty()) ||
                Aware.getSetting(
                    applicationContext,
                    Settings.STATUS_PLUGIN_SMS_RECEIVED
                ) != "true"

    }

    fun getEndDateTime(): Long {
        return if (isEndDateSet()) {
            getEndDateFromSettings()
        } else if (hasCurrentMmsOffset() || hasCurrentSmsOffset()) {
            getSmsSyncDateFromSettings()
        } else {
            System.currentTimeMillis()
        }
    }

    fun hasCurrentSmsOffset(): Boolean { //TODO where is this used and how to update?
        return (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS).isNotEmpty() &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS)
                    .toLong() != Settings.PLUGIN_SMS_CURRENT_OFFSET_DEFAULT)) &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE).isNotEmpty())
    }

    fun hasCurrentMmsOffset(): Boolean { //TODO where is this used and how to update?
        return (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS).isNotEmpty() &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS)
                    .toLong() != Settings.PLUGIN_SMS_CURRENT_OFFSET_DEFAULT)) &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE).isNotEmpty())
    }

    private fun isEndDateSet() =
        !(Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_ENDDATE)
            .isEmpty()) &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_ENDDATE) != "")

    fun setParsedTime(inTime: Long): String {
        val timeParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        return timeParser.format(inTime)
    }


    fun updateSchedule() {
        try {
            var sms_sync = Scheduler.getSchedule(applicationContext, SCHEDULER_PLUGIN_SMS)
            var checkInterval = Settings.PLUGIN_SMS_SYNC_FREQUENCY_DEFAULT
            try {
                checkInterval =
                    Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_FREQUENCY)
                        .toLong()
            } catch (ex: NumberFormatException) {

            }
            if (sms_sync == null || sms_sync.interval != checkInterval) {
                sms_sync = Scheduler.Schedule(SCHEDULER_PLUGIN_SMS)
                sms_sync.interval = checkInterval
                sms_sync!!.actionType = Scheduler.ACTION_TYPE_SERVICE
                sms_sync!!.actionIntentAction = ACTION_REFRESH_SMS
                sms_sync!!.actionClass =
                    applicationContext.packageName + "/" + Plugin::class.java.getName()
                Scheduler.saveSchedule(applicationContext, sms_sync)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun isSmsSyncDateSet() =
        !(Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE).isEmpty()) &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE) != "")

    private fun getSmsSyncDateFromSettings(): Long {
        return getParsedTime(
            Aware.getSetting(
                applicationContext,
                Settings.PLUGIN_SMS_SYNC_DATE
            )
        )
    }

    fun setSmsSyncDate(time: Long) {
        Aware.setSetting(
            applicationContext,
            Settings.PLUGIN_SMS_SYNC_DATE,
            setParsedTime(time)
        )
    }

    fun getLimit(): Int {
        var limit = 0
        if (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_MESSAGE_BATCH_LIMIT)
                .isNotEmpty()
        ) {
            try {
                limit =
                    Aware.getSetting(
                        applicationContext,
                        Settings.PLUGIN_SMS_MESSAGE_BATCH_LIMIT
                    )
                        .toInt()
            } catch (e: IllegalArgumentException) {
                Log.e(
                    TAG,
                    "Failure to parse Message Batch Limit" + Aware.getSetting(
                        applicationContext,
                        Settings.PLUGIN_SMS_MESSAGE_BATCH_LIMIT
                    )
                );
                limit = 0
            }
        }
        return limit
    }

    fun disablePlugin() {
        Aware.setSetting(applicationContext, Settings.STATUS_PLUGIN_SMS_SENT, false)
        Scheduler.removeSchedule(applicationContext, SCHEDULER_PLUGIN_SMS)

        ContentResolver.setSyncAutomatically(
            Aware.getAWAREAccount(applicationContext),
            Provider.getAuthority(applicationContext),
            false
        )
        ContentResolver.removePeriodicSync(
            Aware.getAWAREAccount(applicationContext),
            Provider.getAuthority(applicationContext),
            Bundle.EMPTY
        )
    }

    fun isServerSyncTimestampSet(): Boolean {
        return Aware.getSetting(
            applicationContext,
            Settings.PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP
        ).isEmpty()
    }

    fun setDefaultServerSyncTimestamp() {
        Aware.setSetting(
            applicationContext,
            Settings.PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP,
            Settings.PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP_DEFAULT.toString())
    }

    fun getSmsOffset(): Int {
        var offset = 0
        if (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS).isNotEmpty()
        ) {
            offset = try {
                Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS)
                    .toInt()
            } catch (e: IllegalArgumentException) {
                Log.e(
                    TAG,
                    "Failure to parse SMS_CURRENT_OFFSET" + Aware.getSetting(
                        applicationContext,
                        Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS
                    )
                )
                0
            }
        }
        return offset
    }

    fun getMmsOffset(): Int {
        var offset = 0
        if (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_MMS).isNotEmpty()
        ) {
            offset = try {
                Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_MMS)
                    .toInt()
            } catch (e: IllegalArgumentException) {
                Log.e(
                    TAG,
                    "Failure to parse SMS_CURRENT_OFFSET" + Aware.getSetting(
                        applicationContext,
                        Settings.PLUGIN_SMS_CURRENT_OFFSET_MMS
                    )
                )
                0
            }
        }
        return offset
    }

    fun filterList(list: ArrayList<Message>, includeSentMessages: Boolean, includeReceivedMessages: Boolean): ArrayList<Message> {
        val tempList = ArrayList<Message>()
        for (l in list) {
            if (includeSentMessages && l.type == "sent message") {
                tempList.add(l)
            } else if (includeReceivedMessages && l.type == "received message") {
                tempList.add(l)
            }
        }
        return tempList
    }
}