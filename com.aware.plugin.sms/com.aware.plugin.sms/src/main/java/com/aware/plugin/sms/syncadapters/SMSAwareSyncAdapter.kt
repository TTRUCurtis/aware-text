package com.aware.plugin.sms.syncadapters

import com.aware.plugin.sms.Settings

import android.content.Context
import com.aware.Aware
import com.aware.syncadapters.AwareSyncAdapter

class SMSAwareSyncAdapter(context: Context?, autoInitialize: Boolean, allowParallelSyncs: Boolean) : AwareSyncAdapter(context,
    autoInitialize, allowParallelSyncs
){
    // NOTE: IF YOU ARE USING AN OLDER VERSION OF AwareSyncAdapter, you need to change it's setting
    // from private to protected
    override fun getBatchSize(): Int {

        if ((Aware.getSetting(context, Settings.PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT).isEmpty()) ||
            (Aware.getSetting(context, Settings.PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT) == "")) {
            return Settings.PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT_DEFAULT.toInt()
        }
        return Aware.getSetting(context, Settings.PLUGIN_SMS_MESSAGE_SINGLE_UPLOAD_LIMIT).toInt()

    }
}