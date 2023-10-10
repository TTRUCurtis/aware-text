package com.aware.plugin.sms

import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


//TODO change this to a data class or at least use Kotlin's primary constructor parameters
open class Message(threadId: String?, address: String, type: String, message_date: String?, retrieval_date: String?, msg: String?, mmsPartType: String?, isMms: Boolean) {

    var tag = "AWARE::sms"
    var threadId: String? = null
    var address:String? = null
        private set
    var type: String? = null
    var defaultType: String? = null
    var messageDate: String? = null
    var retrievalDate: String? = null
    var msg: String? = null
    var mmsPartType: String? = null

    private fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }


    private fun setAddress(address: String) {
        var modAddress = address
        try {
            modAddress = md5(modAddress)
        } catch (e: NullPointerException) {
            Log.i(this.tag, "SMS::setAddress: is null")        }
        this.address = address
    }

    private fun setType(type: String, isMms: Boolean) {
        defaultType = type
        if(isMms){
            this.type = if (type == "132") "received message" else "sent message"
        }else{
            this.type = if (type == "1") "received message" else "sent message"
        }
    }

    init {
       // this.id = id
        this.threadId = threadId
        setAddress(address)
        setType(type, isMms)
        this.messageDate = message_date
        this.retrievalDate = retrieval_date
        this.msg = msg
        this.mmsPartType = mmsPartType
    }
}
