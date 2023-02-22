package com.aware.plugin.sms

import android.app.Service
import android.content.Intent
import android.util.Log
import com.aware.plugin.sms.Settings.Limit.NO_LIMIT_INDICATOR
import com.aware.utils.Aware_Plugin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
open class Plugin : Aware_Plugin() {

    object Action {
        const val ACTION_REFRESH_SMS = "ACTION_REFRESH_SMS"
    }

    object Logging {
        const val LOCAL_TAG = "Replacement Plugin"
    }

    @Inject
    lateinit var syncSettings: SyncSettings

    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var serverSync: ServerSync

    @Inject
    lateinit var dispatcher: CoroutineDispatcher

    private val job = SupervisorJob()

    override fun onCreate() {
        super.onCreate()

        Log.i(Logging.LOCAL_TAG, "This is echoed on create")
        AUTHORITY = Provider.getAuthority(this)
    }

    //This function gets called by AWARE to make sure this plugin is still running.
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (PERMISSIONS_OK) {
            val scope = CoroutineScope(dispatcher + job)

            scope.launch {

                //Check if we need this.. should not be starting plugin at all if disabled
                if (syncSettings.isPluginEnabled()) {
                    if (intent != null && intent.action != null && intent.action.equals(
                            Action.ACTION_REFRESH_SMS,
                            ignoreCase = true
                        )
                    ) {
                        AUTHORITY = Provider.getAuthority(applicationContext)

                        //Check where we can move this
                        if (syncSettings.isServerSyncTimestampSet()) syncSettings.setDefaultServerSyncTimestamp()

                        // Get the latest sync timestamp for the SMS table from the aware_log messages
                        // Compare to the currently stored timestamp.  If they're the same, we don't want to
                        // dump any more messages. (Wait for the latest sync). This means it won't pull anymore sms into the aware db until it has synced with server
                        val latestSyncTime: Double = serverSync.getLatestSMSServerSync()
                        val latestDataInsertTime: Double = repository.getLatestSMSDateStamp()
                        if (latestSyncTime >= latestDataInsertTime) {
                            val beginSelectTime: Long
                            val endSelectTime: Long
                            if (syncSettings.sendFullHistory()) {
                                beginSelectTime = syncSettings.getStartDateTime()
                                endSelectTime = syncSettings.getEndDateTime()
                            } else {
                                if (repository.hasLastSyncDateTime()) {
                                    beginSelectTime = repository.getLastSmsSyncTime()
                                    endSelectTime = System.currentTimeMillis()
                                } else {
                                    // This should only happen the first time through
                                    // when we're not pulling any previous data.
                                    beginSelectTime = System.currentTimeMillis()
                                    endSelectTime = System.currentTimeMillis()

                                    //Note see if this is necessary
                                    syncSettings.setSmsSyncDate(System.currentTimeMillis())
                                }
                            }
                            // If we're doing batches, we need to make sure we're setting the endSelectTime
                            // to the time we've saved off to start re-pulling from, otherwise we're going
                            // to get double inserts for any messages that come in during the batch process
                            // (Check if Settings.PLUGIN_SMS_CURRENT_OFFSET > 0, and if so, make sure
                            // end time is not greater than  Settings.PLUGIN_SMS_SYNC_DATE
                            var filterReceivedMessages = ""
                            var filterReceivedMmsMessages = ""
                            if (syncSettings.retrieveOnlySentMessages()) {
                                filterReceivedMessages = " AND type <> 1"
                                filterReceivedMmsMessages = " AND m_type <> 132"
                            }

                            val limit = syncSettings.getLimit()
                            val mmsLimit = limit / 2 //Will still be 0 if no limit indicator
                            val smsLimit = limit - mmsLimit

                            val smsOffset = syncSettings.getSmsOffset()
                            val mmsOffset = syncSettings.getMmsOffset()

                            val mmsList = if (!repository.pullSmsOnly()) repository.getMms(
                                beginSelectTime,
                                endSelectTime,
                                filterReceivedMmsMessages,
                                mmsLimit,
                                mmsOffset
                            ) ?: ArrayList() else ArrayList()

                            val smsList = if (!repository.pullMmsOnly()) repository.getSms(
                                beginSelectTime,
                                endSelectTime,
                                filterReceivedMessages,
                                smsLimit,
                                smsOffset
                            ) ?: ArrayList() else ArrayList()

                            val joinedList = ArrayList<Message>()
                            joinedList.addAll(smsList)
                            joinedList.addAll(mmsList)
                            serverSync.syncMessages(joinedList)

                            // If setting sentiment over sms is turned on we do the following

                            val sentimentAnalysis = Sentiment(applicationContext, joinedList).getInstance()
                            val sentimentDataList: ArrayList<SentimentData> = sentimentAnalysis.getList()
                            serverSync.syncSentiment(sentimentDataList)
//                            for(sentiment in sentimentDataList){
//                                Log.i("SENTIMENTDATA::", "timestamp ${sentiment.timestamp}")
//                                Log.i("SENTIMENTDATA::", "category: ${sentiment.category}")
//                                Log.i("SENTIMENTDATA::", "totalWords: ${sentiment.totalWords.toString()}")
//                                Log.i("SENTIMENTDATA::", "wordCount: ${sentiment.wordCount.toString()}")
//                                Log.i("SENTIMENTDATA::", "score: ${sentiment.score.toString()}")
//                                Log.i("SENTIMENTDATA::", "address: ${sentiment.address}")
//                                Log.i("SENTIMENTDATA::", "type: ${sentiment.type}")
//                                Log.i("SENTIMENTDATA::", " ")
//                                Log.i("SENTIMENTDATA::", " ")
//                            }
                            // If this is the first run, save off the current time in case we need to be in
                            // batch mode
                            // If not pulling Full history:
                            //     (1) If there was no limit, or we got less than the limit of messages pulled
                            //         set time to pull messages from to now, set offset to 0.
                            //     (2) If there was a limit and we hit it, keep the current time to pull messages
                            //         from, and set the offset to the current offset + the limit.
                            // If Pulling Full History:
                            //     (2)  If there was no limit to the amount of messages pulled (we got all of them)
                            //          or we got less than a full batch we're done pulling this history.  Set
                            //          the pulling full history to false, and set the offset to 0
                            //     (3)  If there was a limit and we hit it, set the new offset to the
                            //          current offset + limit.

                            //Check if this is necessary
                            if (!syncSettings.isSmsSyncDateSet()) {
                                syncSettings.setSmsSyncDate(System.currentTimeMillis())
                            }

                            if (limit == NO_LIMIT_INDICATOR || (mmsList.size < mmsLimit && smsList.size < smsLimit)) {
                                repository.lastPulledDateTime(System.currentTimeMillis())
                                repository.donePullingHistory()
                                repository.continueWithSmsOnly(false)
                                repository.continueWithMmsOnly(false)
                                repository.updateSmsOffset(0)
                                repository.updateMmsOffset(0)
                            } else {
                                if (smsList.size < smsLimit) {
                                    repository.updateSmsOffset(0)
                                    repository.continueWithMmsOnly(true)
                                } else {
                                    val newSmsOffset = smsOffset + smsLimit
                                    repository.updateSmsOffset(newSmsOffset)
                                }

                                if (mmsList.size < mmsLimit) {
                                    repository.updateMmsOffset(0)
                                    repository.continueWithSmsOnly(true)
                                } else {
                                    val newMmsOffset = mmsOffset + mmsLimit
                                    repository.updateMmsOffset(newMmsOffset)
                                }
                            }
                        }
                    }

                    //TODO don't think we need this except for maybe the first time?
                    syncSettings.updateSchedule()

                    //TODO don't think we need this except for maybe the first time?
                    serverSync.updateServerSyncSettings()
                }
            }
        } else {
            throw IllegalStateException(TAG + "SMS Plugin does not have the required permissions")
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        syncSettings.disablePlugin()
    }
}
