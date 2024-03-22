package com.aware.utils.sentiment

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentimentDictionary @Inject constructor(@ApplicationContext private val context: Context) {

    private val regularWordsMap = hashMapOf<String, HashMap<String, Double>>()
    private val wildcardWordsMap = hashMapOf<Int, HashMap<String, HashMap<String, Double>>>()

    init {
        buildDictionaryMaps()
    }

    private fun buildDictionaryMaps() {
        val dictionaryJson = JSONObject(loadDictionary())
        val words: JSONObject = dictionaryJson.getJSONObject("words")
        val wildcardWords: JSONObject = dictionaryJson.optJSONObject("wildcards") ?: JSONObject()

        processWordEntries(words, regularWordsMap)
        processWildcardEntries(wildcardWords, wildcardWordsMap)
    }

    private fun processWildcardEntries(wildcardWords: JSONObject, targetMap: HashMap<Int, HashMap<String, HashMap<String, Double>>>) {
        for(key in wildcardWords.keys()) {
            val length = key.length - 1
            val categoriesScores = wildcardWords.getJSONObject(key)
            val categoriesScoresMap = hashMapOf<String, Double>()
            for(category in categoriesScores.keys()) {
                categoriesScoresMap[category] = categoriesScores.getDouble(category)
            }
            targetMap.getOrPut(length) { hashMapOf() }[key] = categoriesScoresMap
        }
    }

    private fun processWordEntries(words: JSONObject, targetMap: HashMap<String, HashMap<String, Double>>) {
        val wordList: Iterator<String> = words.keys()
        while (wordList.hasNext()) {
            val word = wordList.next()
            val categoryAndScore = HashMap<String, Double>()
            val categories = words.getJSONObject(word)
            val categoriesList: Iterator<String> = categories.keys()
            while (categoriesList.hasNext()) {
                val category = categoriesList.next()
                val score = categories.getDouble(category)
                categoryAndScore[category] = score
            }
            targetMap[word] = categoryAndScore
        }
    }

    private fun loadDictionary(): String {
        val json: String
        try {
            val inputStream: InputStream
            val identifier =
                context.resources.getIdentifier("newdictionary", "raw", context.packageName)
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

    fun getRegularWordsMap(): HashMap<String, HashMap<String, Double>> {
        return regularWordsMap
    }

    fun getWildcardWordsMap(): HashMap<Int, HashMap<String, HashMap<String, Double>>> {
        return wildcardWordsMap
    }
}