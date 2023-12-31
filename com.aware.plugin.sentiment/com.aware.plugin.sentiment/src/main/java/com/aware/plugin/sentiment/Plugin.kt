package com.aware.plugin.sentiment

import android.app.Service
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import com.aware.Applications
import com.aware.Aware
import com.aware.Aware_Preferences
import com.aware.providers.Applications_Provider
import com.aware.providers.Keyboard_Provider
import com.aware.utils.Aware_Plugin
import com.aware.utils.sentiment.SentimentAnalysis
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class Plugin : Aware_Plugin() {

    companion object {
        const val PACKAGE_NAME = "packageName"
        const val TYPED_TEXT = "typedText"

        interface AWARESensorObserver {
            fun onTextContextChanged(data: ContentValues)
        }

        var awareSensor: AWARESensorObserver? = null

        fun setSensorObserver(observer: AWARESensorObserver) {
            awareSensor = observer
        }

        fun getSensorObserver(): AWARESensorObserver {
            return awareSensor!!
        }
    }

    @Inject
    lateinit var sentimentAnalysis: SentimentAnalysis

    /**
     * The package name of where the keyboard was interesting to track
     */
    var keyboardInApp = ""

    /**
     * List that contains the device's installed keyboard methods
     */
    var installedKeyboards = ""


    /**
     * Where we keep the buffer of written text
     */
    var textBuffer = ""
    //adding variable that is overwritten to store final text value without edits
    var textBufferNew = ""
    //adding variable for previous text
    var prevTextBuffer = ""

    override fun onCreate() {
        Log.i("ABTest", "This is echoed on create");
        super.onCreate()
        AUTHORITY = Provider.getAuthority(this)
        TAG = "AWARE: Sentiment"

        contextBroadcaster.setProvider(AUTHORITY)
        contextBroadcaster.setTag(TAG)

        val usingInput = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        installedKeyboards = usingInput.enabledInputMethodList.toString()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i("ABTest", "This is echoed on start");
        if (PERMISSIONS_OK) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true")

            if (Aware.getSetting(applicationContext, Settings.STATUS_PLUGIN_SENTIMENT).isEmpty()) {
                Aware.setSetting(applicationContext, Settings.STATUS_PLUGIN_SENTIMENT, true)
            } else {
                if (Aware.getSetting(applicationContext, Settings.STATUS_PLUGIN_SENTIMENT).equals("false", ignoreCase = true)) {
                    Aware.stopPlugin(applicationContext, packageName)
                    return Service.START_STICKY
                }
            }
            Log.i("ABTest", "this is echoed if permissions are correct");
            if (Applications.isAccessibilityServiceActive(this)) {
                Log.i("ABTest", "This is echoed before set sensor observer");
                Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true)
                Aware.startKeyboard(this);


                Log.i("ABTest", "Sentiment object is ")

                Applications.setSensorObserver(object : Applications.AWARESensorObserver {
                    override fun onCrash(data: ContentValues?) {}
                    override fun onNotification(data: ContentValues?) {}
                    override fun onBackground(data: ContentValues?) {}
                    override fun onKeyboard(data: ContentValues?) {

                        var packagesOfInterest: List<String> = listOf()
                        var flag = 0;
                        if (Aware.getSetting(applicationContext, Settings.PLUGIN_SENTIMENT_PACKAGES).isNotBlank()) {
                            Log.i("ABTest", "Plugin packages is not blank ");
                            packagesOfInterest = Aware.getSetting(applicationContext, Settings.PLUGIN_SENTIMENT_PACKAGES).split(",")
                            if (packagesOfInterest.contains(data!!.getAsString(Keyboard_Provider.Keyboard_Data.PACKAGE_NAME))) {
                                flag = 1;
                            }
                        } else {
                            Log.i("ABTest", "Plugin packages is blank ");
                            packagesOfInterest.plus(data!!.getAsString(Keyboard_Provider.Keyboard_Data.PACKAGE_NAME));
                            flag = 1;
                        }

                        Log.i("ABTest", "package data is " + data!!.getAsString(Keyboard_Provider.Keyboard_Data.PACKAGE_NAME));
                        //if (packagesOfInterest.contains(data!!.getAsString(Keyboard_Provider.Keyboard_Data.PACKAGE_NAME))) {
                        if (flag == 1) {

                            keyboardInApp = data!!.getAsString(Keyboard_Provider.Keyboard_Data.PACKAGE_NAME)
                            textBuffer = textBuffer.plus(". ").plus(data.getAsString(Keyboard_Provider.Keyboard_Data.CURRENT_TEXT))

                            val tempCurrTextBuffer = data.getAsString(Keyboard_Provider.Keyboard_Data.CURRENT_TEXT);
                            val tempTextBuffer = data.getAsString(Keyboard_Provider.Keyboard_Data.BEFORE_TEXT);

                            Log.i("ABTest", "Current Text is ");
                            Log.i("ABTest", tempCurrTextBuffer);
                            Log.i("ABTest", "Before text is");
                            Log.i("ABTest", tempTextBuffer);

                            //log only when input ends for corrected text values only
                            // this happens when prevTextBuffer has 0 length and textBufferNew has length>0
                            if (tempCurrTextBuffer.length > 0 && tempTextBuffer.length == 0) {

                                //replace the [ and ] with blanks
                                var interstring1 = textBufferNew.replace("[", "");
                                var interstring2 = interstring1.replace("]", "");
                                Log.i("ABTest", "After corrections prev text is");
                                Log.i("ABTest", interstring2);
                                val tokens = sentimentAnalysis.tokenizer(interstring1)
                                val testHash = sentimentAnalysis.getScores(tokens)
                            }
                            textBufferNew = tempCurrTextBuffer;
                            prevTextBuffer = tempTextBuffer;
                        }
                    }

                    override fun onTouch(data: ContentValues?) {}
                    override fun onForeground(data: ContentValues?) {
                        Log.i("ABTest", "echoed on foreground");
                        val currentApp = data!!.getAsString(Applications_Provider.Applications_Foreground.PACKAGE_NAME)
                        if (installedKeyboards.contains(currentApp)) return //we ignore foreground package of keyboard input

                        if (!textBuffer.isEmpty() && currentApp != keyboardInApp) { //we were using an app of interest and changed app

                            //replace the [ and ] with blanks
                            val interstring1 = textBufferNew.replace("[", "");
                            val interstring2 = interstring1.replace("]", "");

                            Log.i("ABTest", "Echoed before reset $interstring2");
                            val tokens = sentimentAnalysis.tokenizer(interstring2)
                            //val testHash = Sentiment.getScoreFromInput(interstring2);
                            val testHash = sentimentAnalysis.getScores(tokens)
                            val contentValues = ContentValues()
                            contentValues.put(Provider.Sentiment_Data.DEVICE_ID, Aware.getSetting(applicationContext, Aware_Preferences.DEVICE_ID))
                            contentValues.put(Provider.Sentiment_Data.TIMESTAMP, System.currentTimeMillis())
                            contentValues.put(Provider.Sentiment_Data.TYPE, keyboardInApp)
                            contentValues.put(Provider.Sentiment_Data.TOTAL_WORDS, tokens.size)


                            for ((category, pair) in testHash) {
                                if(pair.second > 0){
                                    contentValues.put(Provider.Sentiment_Data.CATEGORY, category)
                                    contentValues.put(Provider.Sentiment_Data.SCORE, pair.first)
                                    contentValues.put(Provider.Sentiment_Data.DICTIONARY_WORDS, pair.second)
                                    contentResolver.insert(Provider.Sentiment_Data.CONTENT_URI, contentValues) //does the actual data insert
                                    Log.i("ABTest", "Inserted into database: $contentValues")
                                }
                                awareSensor?.onTextContextChanged(contentValues)
                            }

                            textBuffer = ""
                            textBufferNew = ""
                            prevTextBuffer = "";
                            keyboardInApp = ""
                        }
                    }
                })
            }

            if (Aware.isStudy(this)) {
                val aware_account = Aware.getAWAREAccount(applicationContext)
                val authority = Provider.getAuthority(applicationContext)
                val frequency = java.lang.Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60

                ContentResolver.setIsSyncable(aware_account, authority, 1)
                ContentResolver.setSyncAutomatically(aware_account, authority, true)
                val request = SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(aware_account, authority)
                        .setExtras(Bundle()).build()
                ContentResolver.requestSync(request)
            }
        }

        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, false)
        Aware.setSetting(this, Settings.STATUS_PLUGIN_SENTIMENT, false)
        Aware.stopKeyboard(this)

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Provider.getAuthority(this),
                Bundle.EMPTY
        );
    }
}
