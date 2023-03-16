package com.aware.utils.sentiment


import edu.emory.mathcs.nlp.component.tokenizer.token.Token
import org.junit.Assert.*

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.internal.createInstance
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SentimentAnalysisTest {

    @Mock
    private lateinit var mockDictionary: SentimentDictionary

    @InjectMocks
    private lateinit var classUnderTest: SentimentAnalysis

    private val wordOne = hashMapOf(
        "adoration" to 8.56,
        "amusement" to -4.598,
        "anger" to 2.986,
        "awe" to -7.596,
        "confusion" to 3.197,
        "contempt" to -4.681,
        "desire" to 9.169,
        "disappointment" to -3.589,
        "distress" to 8.234,
        "fear" to -6.167,
        "interest" to 3.437,
        "sadness" to -7.294
    )
    private val wordTwo = hashMapOf(
        "adoration" to -3.675,
        "amusement" to 9.234,
        "anger" to -3.643,
        "awe" to 7.264,
        "confusion" to -2.436,
        "contempt" to 6.746,
        "desire" to -7.163,
        "disappointment" to 5.397,
        "distress" to -2.873,
        "fear" to 5.747,
        "interest" to -4.916,
        "sadness" to 6.365
    )
    private val wordThree = hashMapOf(
        "adoration" to 8.369,
        "amusement" to -4.159,
        "anger" to 7.643,
        "awe" to -3.263,
        "confusion" to 4.632,
        "contempt" to -9.316,
        "desire" to 5.991,
        "disappointment" to -2.492,
        "distress" to 1.554,
        "fear" to -8.364,
        "interest" to 7.236,
        "sadness" to -6.255
    )
    private val wordFour = hashMapOf(
        "adoration" to -4.268,
        "amusement" to -3.687,
        "anger" to -7.236,
        "awe" to -2.369,
        "confusion" to -3.176,
        "contempt" to -7.462,
        "desire" to -6.315,
        "disappointment" to -4.361,
        "distress" to -3.487,
        "fear" to -8.169,
        "interest" to -1.364,
        "sadness" to -5.649
    )
    private val wordFive = hashMapOf(
        "adoration" to 7.365,
        "amusement" to 5.632,
        "anger" to 1.239,
        "awe" to 2.946,
        "confusion" to 8.649,
        "contempt" to 6.312,
        "desire" to 1.487,
        "disappointment" to 3.345,
        "distress" to 7.256,
        "fear" to 8.653,
        "interest" to 6.315,
        "sadness" to 5.362
    )
    private val mockDictionaryMap = hashMapOf(
        "hate" to wordOne,
        "lol" to wordTwo,
        "flower*" to wordThree,
        ":(" to wordFour,
        "blerg" to wordFive
    )

    @Test
    fun whenNoCategoriesForToken_getScores_returnsEmptyHashMap() {
        //whenever mock dictionary get categories is called for token, then return null
        whenever(mockDictionary.getDictionary()).thenReturn(mockDictionaryMap)
        //call get scores with input
        val text = "Hello Kitty"
        val tokens = classUnderTest.tokenizer(text)
        val map = classUnderTest.getScores(tokens)
        //assert empty hashmap
        assertEquals(hashMapOf<String, HashMap<String, Pair<Double, Int>>>(), map)
    }

    @Test
    fun whenTokenMatchesWordInDictionary() {

        whenever(mockDictionary.getDictionary()).thenReturn(mockDictionaryMap)
        val text = "flower"
        val tokens = classUnderTest.tokenizer(text)
        val map = classUnderTest.getScores(tokens)
        val fakeMap = hashMapOf(
            "adoration" to Pair(8.369, 1),
            "amusement" to Pair(-4.159, 1),
            "anger" to Pair(7.643, 1),
            "awe" to Pair(-3.263, 1),
            "confusion" to Pair(4.632, 1),
            "contempt" to Pair(-9.316, 1),
            "desire" to Pair(5.991, 1),
            "disappointment" to Pair(-2.492, 1),
            "distress" to Pair(1.554, 1),
            "fear" to Pair(-8.364, 1),
            "interest" to Pair(7.236, 1),
            "sadness" to Pair(-6.255, 1)
        )
        assertEquals(fakeMap, map)
    }


    @Test
    fun whenMultipleCategoriesReturnedForTokensList_getScores_returnsValidHashmap() {
        //whenever mock dictionary get categories is called for token1, then return categories1
        //whenever mock dictionary get categories is called for token2, then return categories2
        //whenever mock dictionary get categories is called for token3, then return categories1

        //call get scores with input

        //assert categories and word count are correct
    }


}