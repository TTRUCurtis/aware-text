package com.aware.plugin.sms

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.EditTextPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.util.Log
import com.aware.Aware
import com.aware.ui.AppCompatPreferenceActivity
import org.tukaani.xz.check.Check

class Settings : AppCompatPreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        val PLUGIN_SMS_CONTINUE_WITH_SMS_ONLY = "plugin_sms_contiue_with_sms_only"
        val PLUGIN_SMS_CONTINUE_WITH_MMS_ONLY = "plugin_sms_contiue_with_mms_only"
        //val STATUS_PLUGIN_SMS = "status_plugin_sms"
        val PLUGIN_SMS_SEND_FULL_DATA = "plugin_sms_send_full_data"
        val PLUGIN_SMS_STARTDATE = "plugin_sms_start_date"
        val PLUGIN_SMS_ENDDATE = "plugin_sms_end_date"
        val PLUGIN_SMS_SYNC_DATE = "plugin_sms_last_sync_date"
        val PLUGIN_SMS_SYNC_FREQUENCY = "plugin_sms_sync_frequency"
        val PLUGIN_SMS_MESSAGE_BATCH_LIMIT = "plugin_sms_message_batch_limit"
        val PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT = "plugin_sms_message_single_upload_limit"
        val PLUGIN_SMS_CURRENT_OFFSET_MMS = "plugin_sms_current_offset_mms"
        val PLUGIN_SMS_CURRENT_OFFSET_SMS = "plugin_sms_current_offset_sms"
        val PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP = "plugin_sms_last_server_sync_timestamp"
        val PLUGIN_SMS_SYNC_MIN_SELECT_DATE = "2000-01-01T00:00:01"
        //val PLUGIN_SMS_SEND_RECEIVED_DATA = "plugin_sms_send_received_data"
        val PLUGIN_SMS_SERVER_SYNC_FREQUENCY = "plugin_sms_server_sync_frequency"
        val PLUGIN_SMS_SEND_FULL_DATA_DEFAULT = false
        val PLUGIN_SMS_SEND_RECEIVED_DATA_DEFAULT = false
        val PLUGIN_SMS_SYNC_FREQUENCY_DEFAULT = 1L
        val PLUGIN_SMS_MESSAGE_BATCH_LIMIT_DEFAULT = 0
        val PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT_DEFAULT = 200L
        val PLUGIN_SMS_CURRENT_OFFSET_DEFAULT = 0L
        val PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP_DEFAULT = 0L
        val PLUGIN_SMS_SERVER_SYNC_FREQUENCY_DEFAULT = 6L
        val STATUS_PLUGIN_SMS_SENT = "status_retrieve_sent_messages"
        val STATUS_PLUGIN_SMS_RECEIVED = "status_retrieve_received_messages"
        val STATUS_SENTIMENT_ANALYSIS_RECEIVED = "status_sentiment_analysis_received_messages"
        val STATUS_SENTIMENT_ANALYSIS_SENT = "status_sentiment_analysis_sent_messages"

    }

    object Limit {
        val NO_LIMIT_INDICATOR = 0
    }

    private val LOCAL_TAG  = "AWARE::sms::Settings"
    //lateinit var status : CheckBoxPreference
    //lateinit var send_received_data : CheckBoxPreference
    lateinit var send_full_data: CheckBoxPreference
    lateinit var start_date : EditTextPreference
    lateinit var end_date : EditTextPreference
    lateinit var sync_date : EditTextPreference
    lateinit var sync_frequency : EditTextPreference
    lateinit var message_batch_limit : EditTextPreference
    lateinit var message_upload_limit : EditTextPreference
    lateinit var latest_server_sync : EditTextPreference
    lateinit var server_sync_frequency: EditTextPreference
    lateinit var sent_messages: CheckBoxPreference
    lateinit var received_messages: CheckBoxPreference
    lateinit var sent_sentiment: CheckBoxPreference
    lateinit var received_sentiment: CheckBoxPreference

    @Deprecated("Deprecated in Java")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences_sms)
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
//        status = findPreference(STATUS_PLUGIN_SMS) as CheckBoxPreference
//        if (Aware.getSetting(this, STATUS_PLUGIN_SMS).isEmpty())
//            Aware.setSetting(this, STATUS_PLUGIN_SMS, true)
//        status.isChecked = Aware.getSetting(this, STATUS_PLUGIN_SMS) == "true"
//
//        send_received_data = findPreference(PLUGIN_SMS_SEND_RECEIVED_DATA) as CheckBoxPreference
//        if (Aware.getSetting(this, PLUGIN_SMS_SEND_RECEIVED_DATA).isEmpty())
//            Aware.setSetting(this, PLUGIN_SMS_SEND_RECEIVED_DATA, PLUGIN_SMS_SEND_RECEIVED_DATA_DEFAULT)
//        send_received_data.isChecked = Aware.getSetting(this, PLUGIN_SMS_SEND_RECEIVED_DATA) == "true"

        sent_messages = findPreference(STATUS_PLUGIN_SMS_SENT) as CheckBoxPreference
        if(Aware.getSetting(this, STATUS_PLUGIN_SMS_SENT).isEmpty())
            Aware.setSetting(this, STATUS_PLUGIN_SMS_SENT, true)
        sent_messages.isChecked = Aware.getSetting(this, STATUS_PLUGIN_SMS_SENT) == "true"

        received_messages = findPreference(STATUS_PLUGIN_SMS_RECEIVED) as CheckBoxPreference
        if(Aware.getSetting(this, STATUS_PLUGIN_SMS_RECEIVED).isEmpty())
            Aware.setSetting(this, STATUS_PLUGIN_SMS_RECEIVED, false)
        received_messages.isChecked = Aware.getSetting(this, STATUS_PLUGIN_SMS_RECEIVED) == "true"

        sent_sentiment = findPreference(STATUS_SENTIMENT_ANALYSIS_SENT) as CheckBoxPreference
        if(Aware.getSetting(this, STATUS_SENTIMENT_ANALYSIS_SENT).isEmpty())
            Aware.setSetting(this, STATUS_SENTIMENT_ANALYSIS_SENT, false)
        sent_sentiment.isChecked = Aware.getSetting(this, STATUS_SENTIMENT_ANALYSIS_SENT) == "true"

        received_sentiment = findPreference(STATUS_SENTIMENT_ANALYSIS_RECEIVED) as CheckBoxPreference
        if(Aware.getSetting(this, STATUS_SENTIMENT_ANALYSIS_RECEIVED).isEmpty())
            Aware.setSetting(this, STATUS_SENTIMENT_ANALYSIS_RECEIVED, false)
        received_sentiment.isChecked = Aware.getSetting(this, STATUS_SENTIMENT_ANALYSIS_RECEIVED) == "true"

        send_full_data = findPreference(PLUGIN_SMS_SEND_FULL_DATA) as CheckBoxPreference
        if (Aware.getSetting(this, PLUGIN_SMS_SEND_FULL_DATA).isEmpty())
            Aware.setSetting(this, PLUGIN_SMS_SEND_FULL_DATA, PLUGIN_SMS_SEND_FULL_DATA_DEFAULT)
        send_full_data.isChecked = Aware.getSetting(this, PLUGIN_SMS_SEND_FULL_DATA) == "true"

        start_date = findPreference(PLUGIN_SMS_STARTDATE) as EditTextPreference
        if (Aware.getSetting(this, PLUGIN_SMS_STARTDATE).isEmpty())
            Aware.setSetting(this, PLUGIN_SMS_STARTDATE, "")
        start_date.text = Aware.getSetting(this, PLUGIN_SMS_STARTDATE)
        start_date.summary = Aware.getSetting(this, PLUGIN_SMS_STARTDATE)

        end_date = findPreference(PLUGIN_SMS_ENDDATE) as EditTextPreference
        if (Aware.getSetting(this, PLUGIN_SMS_ENDDATE).isEmpty())
            Aware.setSetting(this, PLUGIN_SMS_ENDDATE, "")
        end_date.text = Aware.getSetting(this, PLUGIN_SMS_ENDDATE)
        end_date.summary = Aware.getSetting(this, PLUGIN_SMS_ENDDATE)

        sync_date = findPreference(PLUGIN_SMS_SYNC_DATE) as EditTextPreference
        if (Aware.getSetting(this, PLUGIN_SMS_SYNC_DATE).isEmpty())
            Aware.setSetting(this, PLUGIN_SMS_SYNC_DATE, "")
        sync_date.text = Aware.getSetting(this, PLUGIN_SMS_SYNC_DATE)
        sync_date.summary = Aware.getSetting(this, PLUGIN_SMS_SYNC_DATE)

        sync_frequency = findPreference(PLUGIN_SMS_SYNC_FREQUENCY) as EditTextPreference
        if ((Aware.getSetting(this, PLUGIN_SMS_SYNC_FREQUENCY).isEmpty()) ||
            (Aware.getSetting(this, PLUGIN_SMS_SYNC_FREQUENCY) == "")) {
            Aware.setSetting(this, PLUGIN_SMS_SYNC_FREQUENCY, PLUGIN_SMS_SYNC_FREQUENCY_DEFAULT.toString())
        }
        sync_frequency.text = Aware.getSetting(this, PLUGIN_SMS_SYNC_FREQUENCY)
        sync_frequency.summary = "Every " + Aware.getSetting(this, PLUGIN_SMS_SYNC_FREQUENCY) + " minute(s)."

        message_batch_limit = findPreference(PLUGIN_SMS_MESSAGE_BATCH_LIMIT) as EditTextPreference
        if ((Aware.getSetting(this, PLUGIN_SMS_MESSAGE_BATCH_LIMIT).isEmpty()) ||
            (Aware.getSetting(this, PLUGIN_SMS_MESSAGE_BATCH_LIMIT) == "")) {
            Aware.setSetting(this, PLUGIN_SMS_MESSAGE_BATCH_LIMIT, PLUGIN_SMS_MESSAGE_BATCH_LIMIT_DEFAULT.toString())
        }
        message_batch_limit.text = Aware.getSetting(this, PLUGIN_SMS_MESSAGE_BATCH_LIMIT)
        message_batch_limit.summary = "Sync " + Aware.getSetting(this, PLUGIN_SMS_MESSAGE_BATCH_LIMIT) + " messages to db at a time. (To not timeout phone)"

        message_upload_limit = findPreference(PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT) as EditTextPreference
        if ((Aware.getSetting(this, PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT).isEmpty()) ||
            (Aware.getSetting(this, PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT) == "")) {
            Aware.setSetting(this, PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT, PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT_DEFAULT.toString())
        }
        message_upload_limit.text = Aware.getSetting(this, PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT)
        message_upload_limit.summary = "Upload " + Aware.getSetting(this, PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT) + " messages in one POST. (To not overrun server buffer)"

        latest_server_sync = findPreference(PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP) as EditTextPreference
        if ((Aware.getSetting(this, PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP).isEmpty()) ||
            (Aware.getSetting(this, PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP) == "")) {
            Aware.setSetting(this, PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP, PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP_DEFAULT.toString())
        }
        latest_server_sync.text = Aware.getSetting(this, PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP)
        latest_server_sync.summary = "Last Server Sync at:" + Aware.getSetting(this, PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP)

        server_sync_frequency = findPreference(PLUGIN_SMS_SERVER_SYNC_FREQUENCY) as EditTextPreference
        if ((Aware.getSetting(this, PLUGIN_SMS_SERVER_SYNC_FREQUENCY).isEmpty()) ||
            (Aware.getSetting(this, PLUGIN_SMS_SERVER_SYNC_FREQUENCY) == "")) {
            Aware.setSetting(this, PLUGIN_SMS_SERVER_SYNC_FREQUENCY, PLUGIN_SMS_SERVER_SYNC_FREQUENCY_DEFAULT.toString())
        }
        server_sync_frequency.text = Aware.getSetting(this, PLUGIN_SMS_SERVER_SYNC_FREQUENCY)
        server_sync_frequency.summary = "Sync to server every " + Aware.getSetting(this, PLUGIN_SMS_SERVER_SYNC_FREQUENCY) + " minutes. (+/- 5 min)"
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        val pref = findPreference(key)
        try {
            when (pref.key) {
                STATUS_PLUGIN_SMS_SENT -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getBoolean(key, false))
                    sent_messages.isChecked = sharedPreferences.getBoolean(key, false)
                }
                STATUS_PLUGIN_SMS_RECEIVED -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getBoolean(key, false))
                    received_messages.isChecked = sharedPreferences.getBoolean(key, false)
                }
                STATUS_SENTIMENT_ANALYSIS_SENT -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getBoolean(key, false))
                    sent_sentiment.isChecked = sharedPreferences.getBoolean(key, false)
                }
                STATUS_SENTIMENT_ANALYSIS_RECEIVED -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getBoolean(key, false))
                    received_sentiment.isChecked = sharedPreferences.getBoolean(key, false)
                }
                PLUGIN_SMS_SEND_FULL_DATA -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getBoolean(key, false))
                    send_full_data.isChecked = sharedPreferences.getBoolean(key, false)
                }
                PLUGIN_SMS_STARTDATE -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getString(key, ""))
                    start_date.text = Aware.getSetting(this, key)
                    start_date.summary = Aware.getSetting(this, key)
                    pref.summary = start_date.text
                }
                PLUGIN_SMS_ENDDATE -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getString(key, ""))
                    end_date.text = Aware.getSetting(this, key)
                    end_date.summary = Aware.getSetting(this, key)
                    pref.summary = end_date.text
                }
                PLUGIN_SMS_SYNC_DATE -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getString(key, ""))
                    sync_date.text = Aware.getSetting(this, key)
                    sync_date.summary = Aware.getSetting(this, key)
                    pref.summary = sync_date.text
                }
                PLUGIN_SMS_SYNC_FREQUENCY -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getString(key, "1"))
                    sync_frequency.text = Aware.getSetting(this, key)
                    sync_frequency.summary = "Every " + Aware.getSetting(this, key) + " minute(s)."
                    pref.summary = sync_frequency.text
                }
                PLUGIN_SMS_MESSAGE_BATCH_LIMIT -> {
                    Aware.setSetting(
                        this,
                        key,
                        sharedPreferences!!.getString(
                            key,
                            PLUGIN_SMS_MESSAGE_BATCH_LIMIT_DEFAULT.toString()
                        )
                    )
                    message_batch_limit.text = Aware.getSetting(this, key)
                    message_batch_limit.summary =
                        "Sync " + Aware.getSetting(this, key) + " messages to db at a time. (To not timeout phone)"
                    pref.summary = message_batch_limit.text
                }
                PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT -> {
                    Aware.setSetting(
                        this,
                        key,
                        sharedPreferences!!.getString(
                            key,
                            PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT_DEFAULT.toString()
                        )
                    )
                    message_upload_limit.text = Aware.getSetting(this, key)
                    message_upload_limit.summary =
                        "Upload " + Aware.getSetting(this, key) + " messages in one POST. (To not overrun server buffer)"
                    pref.summary = message_upload_limit.text
                }
                PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP -> {
                    Aware.setSetting(
                        this,
                        key,
                        sharedPreferences!!.getString(
                            key,
                            PLUGIN_SMS_LAST_SERVER_SYNC_TIMESTAMP_DEFAULT.toString()
                        )
                    )
                    latest_server_sync.text = Aware.getSetting(this, key)
                    latest_server_sync.summary =
                        "Last Server Sync at:" + Aware.getSetting(this, key)
                    pref.summary = latest_server_sync.text
                }
                PLUGIN_SMS_SERVER_SYNC_FREQUENCY -> {
                    Aware.setSetting(this, key, sharedPreferences!!.getString(key, PLUGIN_SMS_SERVER_SYNC_FREQUENCY_DEFAULT.toString()))
                    server_sync_frequency.text = Aware.getSetting(this, key)
                    server_sync_frequency.summary = "Sync to Server every " + Aware.getSetting(this, key) + " minutes. (Min 5, +/- 5 min)"
                    pref.summary = server_sync_frequency.text
                }
            }
        } catch (e:IllegalStateException) {
            Log.e(
                LOCAL_TAG,
                "Null preference received:" + key
                )
        }
    }
}