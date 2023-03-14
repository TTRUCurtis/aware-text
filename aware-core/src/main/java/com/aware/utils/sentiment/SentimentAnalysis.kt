package com.aware.utils.sentiment

import edu.emory.mathcs.nlp.common.util.IOUtils
import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer
import edu.emory.mathcs.nlp.component.tokenizer.token.Token
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class SentimentAnalysis @Inject constructor(private val dictionary: SentimentDictionary) {

    fun getScores(tokens: List<Token>): HashMap<String, Pair<Double, Int>> {
        val sentimentDataMap = HashMap<String, Pair<Double, Int>>()
        for (token in tokens) {
            dictionary.getCategories(token.toString())?.map { (category, score) ->
                val currentScore: Double = sentimentDataMap[category]!!.first
                val newScore = currentScore.plus(score)
                val newCount = sentimentDataMap[category]!!.second.plus(1)
                sentimentDataMap.replace(category, Pair(newScore, newCount))
            }
        }
        return sentimentDataMap
    }

    fun tokenizer(text: String): MutableList<Token> {
        val tokenizer: Tokenizer = EnglishTokenizer()
        val stream: InputStream = ByteArrayInputStream(
            text.lowercase().toByteArray(StandardCharsets.UTF_8)
        )
        val input: BufferedReader = IOUtils.createBufferedReader(stream)

        var tokens: MutableList<Token> = mutableListOf()
        var line: String?

        while (input.readLine().also { line = it } != null) {
            tokens = tokenizer.tokenize(line)
        }

        input.close()
        return tokens
    }

    fun getInstance(): SentimentAnalysis {
        return this
    }
}

