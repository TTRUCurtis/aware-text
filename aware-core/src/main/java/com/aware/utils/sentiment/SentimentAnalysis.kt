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

class SentimentAnalysis @Inject constructor(private val sentimentDictionary: SentimentDictionary) {

    fun getScores(tokens: List<Token>): HashMap<String, Pair<Double, Int>> {
        val sentimentDataMap = HashMap<String, Pair<Double, Int>>()

        for (token in tokens) {
            sentimentDictionary.getRegularWordsMap()[token.toString()]?.let { map ->
                updateSentimentDataMap(sentimentDataMap, map)
            }
        }

        for (token in tokens) {
            for(length in 1..token.toString().length){
                sentimentDictionary.getWildcardWordsMap()[length]?.let { entries ->
                    for((wildcard, categoriesScores) in entries){
                        if(token.toString().startsWith(wildcard.dropLast(1), ignoreCase = true)) {
                            updateSentimentDataMap(sentimentDataMap, categoriesScores)
                        }
                    }

                }
            }
        }
        return sentimentDataMap
    }

    private fun updateSentimentDataMap(sentimentDataMap: HashMap<String, Pair<Double, Int>>, scoreMap: HashMap<String, Double>) {
        scoreMap.forEach { (category, score) ->
            val (currentScore, count) = sentimentDataMap[category] ?: Pair(0.0, 0)
            sentimentDataMap[category] = Pair(currentScore + score, count + 1)
        }
    }

    fun getInstance(): SentimentAnalysis {
        return this
    }

    companion object {
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
    }
}

