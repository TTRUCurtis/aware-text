package com.aware.utils.sentiment

import org.junit.Assert.*

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SentimentAnalysisTest {

    @Mock
    private lateinit var mockDictionary: SentimentDictionary

    @InjectMocks
    private lateinit var classUnderTest: SentimentAnalysis

    @Test
    fun whenNoCategoriesForToken_getScores_returnsEmptyHashMap() {
        //whenever mock dictionary get categories is called for token, then return null

        //call get scores with input

        //assert empty hashmap
    }

    @Test
    fun whenMultipleCategoriesReturnedForTokensList_getScores_returnsValidHashmap() {
        //whenever mock dictionary get categories is called for token1, then return categories1
        //whenever mock dictionary get categories is called for token2, then return categories2
        //whenever mock dictionary get categories is called for token3, then return categories1

        //call get scores with input

        //assert categories and word count are correct
    }

    @Test
    fun getScores() {
    }

    @Test
    fun tokenizer() {
    }
}