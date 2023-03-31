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

    object SyncKeys{
        const val SCHEDULER_PLUGIN_SMS = "SCHEDULER_PLUGIN_SMS"
        const val ACTION_REFRESH_SMS = "ACTION_REFRESH_SMS"
        val TAG: String = SyncSettings::class.java.simpleName
    }



    fun sendFullHistory() =
        Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SEND_FULL_DATA).isNotEmpty() &&
                Aware.getSetting(
                    applicationContext,
                    Settings.PLUGIN_SMS_SEND_FULL_DATA
                ) == "true"

    fun getStartDateTime(): Long {
        return if (isStartDateSetByStudy()) {
            getStartDateFromSettings()
        } else {
            getParsedTime(Settings.PLUGIN_SMS_SYNC_MIN_SELECT_DATE)
        }
    }

    fun isSettingChecked(setting: String): Boolean{
        return Aware.getSetting(applicationContext, setting).isNotEmpty()
    }

    private fun getStartDateFromSettings() = getParsedTime(
        Aware.getSetting(
            applicationContext,
            Settings.PLUGIN_SMS_STARTDATE
        )
    )

    private fun isStartDateSetByStudy() =
        Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_STARTDATE).isNotEmpty() &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_STARTDATE) != "")


    private fun getParsedTime(timeString: String): Long {
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
                    retVal = timeParser.parse(timeString)!!.time
                    return retVal
                } catch (e: Exception) {
                    // Failed to Parse, drop to Log and return 0L
                }
            }
        }
        Log.e(SyncKeys.TAG, "Failure to parse time string:$timeString")
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

    fun getEndDateTime(): Long {
        return if (isEndDateSet()) {
            getEndDateFromSettings()
        } else if (hasCurrentMmsOffset() || hasCurrentSmsOffset()) {
            getSmsSyncDateFromSettings()
        } else {
            System.currentTimeMillis()
        }
    }

    private fun hasCurrentSmsOffset(): Boolean { //TODO where is this used and how to update?
        return (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS).isNotEmpty() &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS)
                    .toLong() != Settings.PLUGIN_SMS_CURRENT_OFFSET_DEFAULT)) &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE).isNotEmpty())
    }

    private fun hasCurrentMmsOffset(): Boolean { //TODO where is this used and how to update?
        return (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS).isNotEmpty() &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS)
                    .toLong() != Settings.PLUGIN_SMS_CURRENT_OFFSET_DEFAULT)) &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE).isNotEmpty())
    }

    private fun isEndDateSet() =
        Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_ENDDATE).isNotEmpty() &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_ENDDATE) != "")

    private fun setParsedTime(inTime: Long): String {
        val timeParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        return timeParser.format(inTime)
    }


    fun updateSchedule() {
        try {
            var smsSync = Scheduler.getSchedule(applicationContext, SyncKeys.SCHEDULER_PLUGIN_SMS)
            var checkInterval = Settings.PLUGIN_SMS_SYNC_FREQUENCY_DEFAULT
            try {
                checkInterval =
                    Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_FREQUENCY)
                        .toLong()
            } catch (ex: NumberFormatException) {

            }
            if (smsSync == null || smsSync.interval != checkInterval) {
                smsSync = Scheduler.Schedule(SyncKeys.SCHEDULER_PLUGIN_SMS)
                smsSync.interval = checkInterval
                smsSync.actionType = Scheduler.ACTION_TYPE_SERVICE
                smsSync.actionIntentAction = SyncKeys.ACTION_REFRESH_SMS
                smsSync.actionClass =
                    applicationContext.packageName + "/" + Plugin::class.java.name
                Scheduler.saveSchedule(applicationContext, smsSync)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun isSmsSyncDateSet() =
        Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE).isNotEmpty() &&
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
                    SyncKeys.TAG,
                    "Failure to parse Message Batch Limit" + Aware.getSetting(
                        applicationContext,
                        Settings.PLUGIN_SMS_MESSAGE_BATCH_LIMIT
                    )
                )
                limit = 0
            }
        }
        return limit
    }

    fun disablePlugin() {
        //TODO check if we need to set other setting statuses to false as well
        Aware.setSetting(applicationContext, Settings.STATUS_PLUGIN_SMS_SENT, false)
        Scheduler.removeSchedule(applicationContext, SyncKeys.SCHEDULER_PLUGIN_SMS)

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
                    SyncKeys.TAG,
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
                    SyncKeys.TAG,
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