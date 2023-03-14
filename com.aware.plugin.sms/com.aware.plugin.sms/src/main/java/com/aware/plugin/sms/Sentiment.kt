package com.aware.plugin.sms

import android.content.Context
import com.aware.utils.sentiment.SentimentAnalysis
import com.aware.utils.sentiment.SentimentDictionary
import edu.emory.mathcs.nlp.component.tokenizer.token.Token


class Sentiment(
    context: Context,
    messages: ArrayList<Message>
) {
    private var sentimentList: ArrayList<SentimentData>
    private var sentimentAnalysis: SentimentAnalysis
    private var context: Context
    private var timestamp: String = ""
    private var totalWords: Int = 0
    private var address: String = ""
    private var type: String = ""

    fun getInstance(): Sentiment{
        return this
    }

    fun getList(): ArrayList<SentimentData>{
        return sentimentList
    }

    init{
        this.context = context
        sentimentList = ArrayList()
        sentimentAnalysis = SentimentAnalysis(SentimentDictionary( context))
        startSentimentAnalysis(messages)
    }

    private fun startSentimentAnalysis(messages: ArrayList<Message>) {
        for(message in messages){
            timestamp = message.messageDate.toString()
            address = message.address.toString()
            type = message.type.toString()
            val text: String = message.msg.toString()
            val tokens: List<Token> = sentimentAnalysis.tokenizer(text)
            totalWords = tokens.size
            val scores = sentimentAnalysis.getScores(tokens)
            scores.map { (category, pair) ->
                if(pair.first != 0.0){
                    sentimentList.add(
                        SentimentData(
                            timestamp,
                            category,
                            totalWords,
                            pair.second,
                            pair.first,
                            address,
                            type
                        )
                    )
                }
            }
        }
    }
}

data class SentimentData(
    val timestamp: String,
    val category: String,
    val totalWords: Int,
    val wordCount: Int,
    val score: Double,
    val address: String,
    val type: String
)