package com.aware.plugin.sms

import com.aware.utils.sentiment.SentimentAnalysis
import edu.emory.mathcs.nlp.component.tokenizer.token.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class Sentiment @Inject constructor(
    private val sentimentAnalysis: SentimentAnalysis
) {

    private var retrievalTimestamp: String = ""
    private var messageTimestamp: String = ""
    private var totalWords: Int = 0
    private var address: String = ""
    private var type: String = ""

    suspend fun getList(
        messages: ArrayList<Message>
    ): ArrayList<SentimentData> {
        return withContext(Dispatchers.IO) { //TODO inject dispatcher
            val sentimentList: ArrayList<SentimentData> = ArrayList()
            for (message in messages) {
                retrievalTimestamp = System.currentTimeMillis().toString()
                messageTimestamp = message.messageDate.toString()
                address = message.address.toString()
                type = message.type.toString()
                val text: String = message.msg.toString()
                val tokens: List<Token> = SentimentAnalysis.tokenizer(text)
                totalWords = tokens.size
                val scores = sentimentAnalysis.getScores(tokens)
                scores.map { (category, pair) ->
                    if (pair.first != 0.0) {
                        sentimentList.add(
                            SentimentData(
                                retrievalTimestamp,
                                messageTimestamp,
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
            sentimentList
        }
    }
}

data class SentimentData(
    val retrievalTimestamp: String,
    val messageTimestamp: String,
    val category: String,
    val totalWords: Int,
    val wordCount: Int,
    val score: Double,
    val address: String,
    val type: String
)