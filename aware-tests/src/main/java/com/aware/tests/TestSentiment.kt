package com.aware.tests

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aware.Aware
import com.aware.plugin.sentiment.Plugin
import com.aware.plugin.sentiment.Settings

class TestSentiment : AwareTest {
    override fun test(context: Context?) {
        Aware.setSetting(context, Settings.PLUGIN_SENTIMENT_PACKAGES, "com.whatsapp", "com.aware.plugin.sentiment")
        Aware.startPlugin(context, "com.aware.plugin.sentiment")
        Plugin.setSensorObserver(object : Plugin.Companion.AWARESensorObserver {
            override fun onTextContextChanged(data: ContentValues) {
                Log.d("TestSentiment", data.toString())
            }
        })
    }
}
