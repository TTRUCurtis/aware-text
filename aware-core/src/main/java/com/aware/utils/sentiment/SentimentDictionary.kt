package com.aware.utils.sentiment

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import javax.inject.Inject

class SentimentDictionary @Inject constructor(@ApplicationContext private val context: Context) {

    private lateinit var dictionaryMap: HashMap<String, HashMap<String, Double>>

    init {
        try {
            buildDictionaryMap()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun getCategories(token: String): HashMap<String, Double>? {
        return dictionaryMap[token]
    }

    private fun buildDictionaryMap() {
        dictionaryMap = HashMap()
        val dictionaryJson = JSONObject(loadDictionary())
        val words: JSONObject = dictionaryJson.getJSONObject("words")
        val wordList: Iterator<String> = words.keys()
        while (wordList.hasNext()) {
            val word: String = wordList.next()
            val categoryAndScore: HashMap<String, Double> = HashMap()
            val categories: JSONObject = words.getJSONObject(word)
            val categoriesList: Iterator<String> = categories.keys()
            while (categoriesList.hasNext()) {
                val category: String = categoriesList.next()
                val score: Double = categories.getDouble(category)
                categoryAndScore[category] = score
            }
            dictionaryMap[word] = categoryAndScore
        }
    }


    private fun loadDictionary(): String {
        val json: String
        try {
            val inputStream: InputStream
            val identifier =
                context.resources.getIdentifier("sentiment_dictionary", "raw", context.packageName)
            inputStream = context.resources.openRawResource(identifier)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val charset: Charset = Charsets.UTF_8
            json = String(buffer, charset)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return "" //or null (change return type to String?)
        }
        return json
    }
}