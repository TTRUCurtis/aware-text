package com.aware

import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import edu.emory.mathcs.nlp.common.util.IOUtils
import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer
import edu.emory.mathcs.nlp.component.tokenizer.token.Token
import org.json.JSONArray
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class SentimentAnalysis(
    context: Context,
) {

    private var context: Context
    private lateinit var dictionary: JSONObject
    private lateinit var dictionaryMap: HashMap<String, HashMap<String, Double>>
    private lateinit var categoriesArray: JSONArray
    private lateinit var sentimentDataMap: HashMap<String, Pair<Double, Int>>


    init{
        this.context = context
        try{
            dictionary = JSONObject(loadDictionary(this.context))
        }catch(e: JSONException){
            e.printStackTrace()
        }
        buildMaps()
    }

    private fun buildMaps() {

        dictionaryMap = HashMap()
        sentimentDataMap = HashMap()
        val words: JSONObject = dictionary.getJSONObject("words")
        val wordList: Iterator<String> = words.keys()
        while(wordList.hasNext()){
            val word: String = wordList.next()
            val categoryAndScore: HashMap<String, Double> = HashMap()
            val categories: JSONObject = words.getJSONObject(word)
            val categoriesList: Iterator<String> = categories.keys()
            while(categoriesList.hasNext()){
                val category: String = categoriesList.next()
                val score: Double = categories.getDouble(category)
                categoryAndScore[category] = score
            }
            dictionaryMap[word] = categoryAndScore
        }

        categoriesArray = dictionary.getJSONArray("categories")
        resetScore()
    }

    fun getScores(tokens: List<Token>){
        for(token in tokens){
            if(dictionaryMap.containsKey(token.toString())){
                val tempMap = dictionaryMap[token.toString()]
                tempMap?.map{ (category, score) ->
                    val currentScore: Double = sentimentDataMap[category]!!.first
                    val newScore = currentScore.plus(score)
                    val newCount = sentimentDataMap[category]!!.second.plus(1)
                    sentimentDataMap.replace(category, Pair(newScore, newCount))
                }
            }
        }
    }

    fun resetScore() {
        val size = categoriesArray.length()
        val initialScore = 0.0
        val initialCount = 0
        for(i in 0 until size){
            val categoryItem: String = categoriesArray[i].toString()
            sentimentDataMap[categoryItem] = Pair(initialScore, initialCount)
        }
    }

    private fun loadDictionary(context: Context): String{
        val json: String
        try{
            val inputStream: InputStream
            val identifier = context.resources.getIdentifier("sentiment_dictionary", "raw", context.packageName)
            inputStream = context.resources.openRawResource(identifier)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val charset: Charset = Charsets.UTF_8
            json = String(buffer, charset)
        }catch (ex: IOException){
            ex.printStackTrace()
            return "" //or null (change return type to String?)
        }
        return json
    }

    fun tokenizer(text: String): MutableList<Token>{
        val tokenizer: Tokenizer = EnglishTokenizer()
        val stream: InputStream = ByteArrayInputStream(
            text.lowercase().toByteArray(StandardCharsets.UTF_8)
        )
        val input: BufferedReader = IOUtils.createBufferedReader(stream)

        var tokens: MutableList<Token> = mutableListOf()
        var line: String?

        while (input.readLine().also{ line = it } != null) {
            tokens = tokenizer.tokenize(line)
        }

        input.close()
        return tokens
    }

    fun getInstance(): SentimentAnalysis {
        return this
    }

    fun getSentimentMap(): HashMap<String, Pair<Double, Int>>{
        return sentimentDataMap
    }
    }

