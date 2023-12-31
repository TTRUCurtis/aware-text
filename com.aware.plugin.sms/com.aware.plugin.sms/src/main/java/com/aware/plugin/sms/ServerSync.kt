package com.aware.plugin.sms

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.SyncRequest
import android.os.Bundle
import com.aware.Aware
import com.aware.Aware_Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

class ServerSync @Inject constructor(
    @ApplicationContext private val applicationContext: Context
){
    suspend fun syncMessages(messageList: List<Message>) {
        withContext(Dispatchers.IO) { //TODO inject dispatcher

            for (message in messageList) {
                val smsInfo = ContentValues()
                smsInfo.put(
                    Provider.Sms_Data.RETRIEVAL_TIMESTAMP,
                    message.retrievalDate
                )
                smsInfo.put(
                    Provider.Sms_Data.DEVICE_ID,
                    Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID)
                )
                smsInfo.put(Provider.Sms_Data.MESSAGE_TIMESTAMP, message.messageDate)
                smsInfo.put(Provider.Sms_Data.MSG_TYPE, message.type)
                smsInfo.put(Provider.Sms_Data.MSG_THREAD_ID, message.threadId)
                smsInfo.put(Provider.Sms_Data.MSG_ADDRESS, message.address)
                smsInfo.put(Provider.Sms_Data.MSG_BODY, message.msg)
                smsInfo.put(Provider.Sms_Data.MSG_MMS_PART_TYPE, message.mmsPartType)

                applicationContext.contentResolver.insert(
                    Provider.Sms_Data.CONTENT_URI,
                    smsInfo
                )
            }
        }
    }

    suspend fun syncSentiment(sentimentList: List<SentimentData>) {
        withContext(Dispatchers.IO) { //TODO inject dispatcher

            for (sentiment in sentimentList) {
                val sentimentInfo = ContentValues()
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.RETRIEVAL_TIMESTAMP, sentiment.retrievalTimestamp
                )
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.DEVICE_ID,
                    Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID)
                )
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.MESSAGE_TIMESTAMP, sentiment.messageTimestamp
                )
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.CATEGORY, sentiment.category
                )
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.TOTAL_WORDS, sentiment.totalWords
                )
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.DICTIONARY_WORDS, sentiment.wordCount
                )
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.SCORE, sentiment.score
                )
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.ADDRESS, sentiment.address
                )
                sentimentInfo.put(
                    Provider.Sentiment_Analysis.TYPE, sentiment.type
                )

                applicationContext.contentResolver.insert(
                    Provider.Sentiment_Analysis.CONTENT_URI,
                    sentimentInfo
                )
            }
        }
    }

    //STUDY-SYNC: plugin_sms
    fun setPeriodic() {
        if (Aware.isStudy(applicationContext)) {
            val aware_account = Aware.getAWAREAccount(applicationContext)
            val authority = Provider.getAuthority(applicationContext)
            val frequency: Long
            if (Aware.getSetting(applicationContext, Settings.PLUGIN_SMS_SERVER_SYNC_FREQUENCY)
                    .isEmpty()
            ) {
                frequency = Settings.PLUGIN_SMS_SERVER_SYNC_FREQUENCY_DEFAULT * 60L
            } else {
                frequency = java.lang.Long.parseLong(
                    Aware.getSetting(
                        applicationContext,
                        Settings.PLUGIN_SMS_SERVER_SYNC_FREQUENCY
                    )
                ) * 60L
            }
            //val frequency:Long = 6L*60L  //(minutes * 60 sec/min)
            //The flex in frequency of recurrent calls can't be smaller than 5 min or .05 * frequency (whichever is larger)
            val frequencyFlex: Long = max(
                (frequency + (20L - 1L)) / 20L,
                5L * 60L
            ) // Math trick Note (x + (y-1)) / y is short for Integer Division x / y Round up
            ContentResolver.setIsSyncable(aware_account, authority, 1)
            ContentResolver.setSyncAutomatically(aware_account, authority, true)
            val request = SyncRequest.Builder()
                // syncPeriodic(sync time in seconds, flex time (MAX(.05 * frequency, 5 min) in seconds )
                .syncPeriodic(frequency, frequencyFlex)
                .setSyncAdapter(aware_account, authority)
                .setExtras(Bundle()).build()
            ContentResolver.requestSync(request)
        }
    }
}
