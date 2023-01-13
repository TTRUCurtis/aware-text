package com.aware.plugin.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.aware.Aware
import com.aware.utils.Encrypter
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import javax.inject.Inject


class Repository @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val utils: Utils
) {

    object MsgConstants {
        const val KEY_THREAD_ID = "thread_id"
        const val KEY_ADDRESS = "address"
        const val KEY_DATE = "date"
    }

    object SmsConstants {
        const val KEY_TYPE = "type"
        const val KEY_MSG_BODY = "body"
    }

    object MmsConstants {
        const val KEY_M_TYPE = "m_type"
        const val KEY_TEXT = "text"
        const val KEY_CT = "ct"
        const val KEY_ID = "_id"
        const val KEY_DATA = "_data"
    }


    fun getSms(
        beginSelectTime: Long,
        endSelectTime: Long,
        filterReceivedMessages: String,
        limit: Int,
        offset: Int
    ): MutableList<Message>? {
        val uriInbox = Uri.parse("content://sms/")

        val queryString =
            "date > $beginSelectTime AND date <= $endSelectTime$filterReceivedMessages"

        val sortString: String = if (limit != 0) {
            "${android.provider.Telephony.TextBasedSmsColumns.DATE} ASC " +
                    "LIMIT " + limit + " OFFSET " + offset
        } else {
            "${android.provider.Telephony.TextBasedSmsColumns.DATE} ASC "
        }

        val c: Cursor? = applicationContext.contentResolver.query(
            uriInbox, null, queryString, null, sortString
        )

        val smsList: MutableList<Message>?

        if (c != null && c.moveToFirst()) {

            smsList = ArrayList()


            for (i in 0 until c.count) {

                val type =
                    c.getString(c.getColumnIndexOrThrow(SmsConstants.KEY_TYPE))
                val threadId =
                    c.getString(c.getColumnIndexOrThrow(MsgConstants.KEY_THREAD_ID))

                val phoneNumber = c.getString(c.getColumnIndexOrThrow(MsgConstants.KEY_ADDRESS))

                val user = Encrypter.formatAndHashAddress(
                    applicationContext, phoneNumber )

                val date =
                    c.getString(c.getColumnIndexOrThrow(MsgConstants.KEY_DATE))
                val msg =
                    c.getString(c.getColumnIndexOrThrow(SmsConstants.KEY_MSG_BODY))
                val currentTime = System.currentTimeMillis()
                val isMms = false
                smsList.add(
                    Message(
                        threadId,
                        user,
                        type,
                        date,
                        currentTime.toString(),
                        msg,
                        isMms
                    )
                )
                c.moveToNext()
            }
        } else {
            smsList = null
        }
        c?.close()
        smsList?.sortBy { it.retrievalDate } //can this be done by the db query itself?
        return smsList
    }

    fun getMms(
        beginSelectTime: Long,
        endSelectTime: Long,
        filterReceivedMessages: String,
        limit: Int,
        offset: Int
    ): MutableList<Message>? {
        //The following are needed to query the MMS table, timestamps are off by /1000 in MMS table
        val adjustedBeginSelectTime = beginSelectTime / 1000
        val adjustedEndSelectTime = endSelectTime / 1000

        val queryString =
            "date > $adjustedBeginSelectTime AND date <= $adjustedEndSelectTime$filterReceivedMessages"

        val sortString: String = if (limit != 0) {
            "${android.provider.Telephony.BaseMmsColumns.DATE} ASC " +
                    "LIMIT " + limit + " OFFSET " + offset
        } else {
            "${android.provider.Telephony.BaseMmsColumns.DATE} ASC "
        }
        //Variables declared outside of cursor loop for scope
        var addresses = ""
        var body = ""
        var date: Long
        var mid : String
        var threadId: String
        val timeStamp = System.currentTimeMillis()
        var type: String

        val mmsUri = Uri.parse("content://mms")
        val mmsProjection = arrayOf("_id", "thread_id", "msg_box", "ct_t", "date", "m_type")
        val mmsSelection = "(msg_box=1 or msg_box=2) AND $queryString"
        val mmsCursor = applicationContext.contentResolver.query(
            mmsUri,
            mmsProjection,
            mmsSelection,
            null,
            sortString
        )

        val mmsList: MutableList<Message>?

        if (mmsCursor != null) {
            mmsList = ArrayList()
            try {
                if (mmsCursor.moveToFirst()) {

                    do {
                        mid = mmsCursor.getString(0)
                        threadId = mmsCursor.getString(mmsCursor.getColumnIndexOrThrow(MsgConstants.KEY_THREAD_ID))
                        val mmsDate = mmsCursor.getString(mmsCursor.getColumnIndexOrThrow(MsgConstants.KEY_DATE)) //date is given by a fraction of 1000, hence following line
                        date = mmsDate.toLong() * 1000
                        type = mmsCursor.getString(mmsCursor.getColumnIndexOrThrow(MmsConstants.KEY_M_TYPE))

                        val mmsPartSelection = "mid=$mid"
                        val mmsPartUri = Uri.parse("content://mms/part")
                        val mmsPartProjection = arrayOf("*")
                        val mmsPartCursor = applicationContext.contentResolver.query(
                            mmsPartUri,
                            mmsPartProjection,
                            mmsPartSelection,
                            null,
                            null
                        )


                        if (mmsPartCursor != null && mmsPartCursor.moveToFirst()) {

                            do {
                                val mmsPartType =
                                    mmsPartCursor.getString(mmsPartCursor.getColumnIndexOrThrow(MmsConstants.KEY_CT))
                                val mmsPartId =
                                    mmsPartCursor.getString(mmsPartCursor.getColumnIndexOrThrow(MmsConstants.KEY_ID))
                                if ("text/plain" == mmsPartType) {
                                    val data =
                                        mmsPartCursor.getString(mmsPartCursor.getColumnIndex(MmsConstants.KEY_DATA))
                                    body = if (data != null) {
                                        getMmsText(mmsPartId)
                                    } else {
                                        mmsPartCursor.getString(mmsPartCursor.getColumnIndexOrThrow(MmsConstants.KEY_TEXT))
                                    }
                                }

                            } while (mmsPartCursor.moveToNext())
                            mmsPartCursor.close()
                        }

                        val mmsAddrUri = Uri.parse("content://mms/$mid/addr")
                        val mmsAddrProjection = arrayOf("address")
                        val mmsAddrCursor: Cursor? = applicationContext.contentResolver.query(
                            mmsAddrUri,
                            mmsAddrProjection,
                            mid,
                            null,
                            null
                        )

                        if (mmsAddrCursor != null && mmsAddrCursor.moveToFirst()) {
                            val addressesBuilder = StringBuilder()
                            do {
                                val rawAddress = mmsAddrCursor.getString(
                                    mmsAddrCursor.getColumnIndexOrThrow(
                                        MsgConstants.KEY_ADDRESS
                                    )
                                )

                                val address = if (rawAddress != "insert-address-token") rawAddress else ""

                                val encryptedAddress =
                                    Encrypter.formatAndHashAddress(applicationContext, address)

                                addressesBuilder.append("$encryptedAddress ")

                            } while (mmsAddrCursor.moveToNext())
                            mmsAddrCursor.close()
                            addresses = addressesBuilder.trim().toString()
                        }

                        val isMms = true
                        mmsList.add(
                            Message(
                                threadId,
                                addresses,
                                type,
                                date.toString(),
                                timeStamp.toString(),
                                body,
                                isMms
                            )
                        )
                    } while (mmsCursor.moveToNext())
                }
            } catch (e: java.lang.Exception) {
                Log.e("This is the error ", e.message.toString())
            }
        } else {
            mmsList = null
        }
        mmsCursor?.close()
        mmsList?.sortBy { it.retrievalDate }
        return mmsList
    }

    //What does this mean
    fun hasLastSyncDateTime(): Boolean {
        return Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE).isNotEmpty() &&
                (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SYNC_DATE) != "")
    }

    fun getLastSmsSyncTime(): Long {
        return utils.getParsedTime(
            Aware.getSetting(
                applicationContext,
                Settings.PLUGIN_SMS_SYNC_DATE
            )
        )
    }

    fun getLatestSMSDateStamp(): Double {
        var lastDataInsertTimestamp = 0.0
        val lastTimestamp: Cursor? = applicationContext.contentResolver.query(
            Provider.Sms_Data.CONTENT_URI,
            null,
            null,
            null,
            Provider.Sms_Data.RETRIEVAL_TIMESTAMP + " DESC LIMIT 1"
        )
        if (lastTimestamp != null && lastTimestamp.moveToFirst()) {
            try {
                lastDataInsertTimestamp =
                    lastTimestamp.getDouble(lastTimestamp.getColumnIndex(Provider.Sms_Data.RETRIEVAL_TIMESTAMP))
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            lastTimestamp.close()
        }
        return lastDataInsertTimestamp
    }

    fun lastPulledDateTime(dateTime: Long) {
        Aware.setSetting(
            applicationContext,
            Settings.PLUGIN_SMS_SYNC_DATE,
            setParsedTime(dateTime)
        )
    }


    private fun setParsedTime(inTime: Long): String {
        val timeParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        return timeParser.format(inTime)
    }

    private fun getMmsText(id: String): String {
        val partURI = Uri.parse("content://mms/part/$id")
        var inputS: InputStream? = null
        val sb = StringBuilder()
        try {
            inputS = applicationContext.contentResolver.openInputStream(partURI)
            if (inputS != null) {
                val inputSr = InputStreamReader(inputS, "UTF-8")
                val reader = BufferedReader(inputSr)
                var temp = reader.readLine()
                while (temp != null) {
                    sb.append(temp)
                    temp = reader.readLine()
                }
            }
        } catch (e: IOException) {
        } finally {
            if (inputS != null) {
                try {
                    inputS.close()
                } catch (e: IOException) {
                }
            }
        }
        return sb.toString()
    }

    fun donePullingHistory() {
        Aware.setSetting(applicationContext, Settings.PLUGIN_SMS_SEND_FULL_DATA, false)
    }

    fun continueWithSmsOnly(smsOnly: Boolean) {
        Aware.setSetting(applicationContext, Settings.PLUGIN_SMS_CONTINUE_WITH_SMS_ONLY, smsOnly)
    }

    fun pullSmsOnly() =
        Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CONTINUE_WITH_SMS_ONLY).isNotEmpty() &&
                Aware.getSetting(
                    applicationContext,
                    Settings.PLUGIN_SMS_CONTINUE_WITH_SMS_ONLY
                ) == "true"

    fun continueWithMmsOnly(mmsOnly: Boolean) {
        Aware.setSetting(applicationContext, Settings.PLUGIN_SMS_CONTINUE_WITH_MMS_ONLY, mmsOnly)
    }

    fun pullMmsOnly() =
        Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_CONTINUE_WITH_MMS_ONLY).isNotEmpty() &&
                Aware.getSetting(
                    applicationContext,
                    Settings.PLUGIN_SMS_CONTINUE_WITH_MMS_ONLY
                ) == "true"

    fun updateSmsOffset(i: Int) {
        Aware.setSetting(
            applicationContext,
            Settings.PLUGIN_SMS_CURRENT_OFFSET_SMS,
            i.toString()
        )
    }

    fun updateMmsOffset(i: Int) {
        Aware.setSetting(
            applicationContext,
            Settings.PLUGIN_SMS_CURRENT_OFFSET_MMS,
            i.toString()
        )
    }
}