package com.aware.plugin.sms

import android.util.Log
import java.lang.Exception
import java.text.SimpleDateFormat
import javax.inject.Inject

class Utils @Inject constructor() {

    private val TAG = Repository::class.java.simpleName

    fun getParsedTime(timeString: String) : Long {
        var retVal : Long = 0
        val timePatterns = mapOf("yyyy-MM-dd'T'HH:mm:ss.SSS" to 23,
            "yyyy-MM-dd'T'HH:mm:ss" to 19,
            "yyyy-MM-dd" to 10)
        for (pattern in timePatterns) {
            if (pattern.value == timeString.length) {
                val timeParser = SimpleDateFormat(pattern.key)
                try {
                    retVal = timeParser.parse(timeString)!!.getTime()
                    return retVal
                } catch(e: Exception) {
                    // Faield to Parse, drop to Log and return 0L
                }
            }
        }
        Log.e(TAG, "Failure to parse time string:" + timeString);
        return retVal
    }
}