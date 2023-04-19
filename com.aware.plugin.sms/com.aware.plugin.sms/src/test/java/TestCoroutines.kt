package com.aware.plugin.sms

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.test.runTest
import org.junit.Test

/*
* A class for playing around with Kotlin coroutines
*/
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutines {

    private val eventsChannel = Channel<Int>()

    @Test
    fun main() = runTest {
        launch {
            eventsChannel.consumeEach { processEvent(it) }
        }
        for (i in 1..10) {
            println("Times method called: $i")
            launch {
                eventsChannel.send(i)
            }
            println("I don't wanna wait: $i")
        }
    }

    private suspend fun processEvent(i: Int) {
        val data = fetchData(i)
        println(data)
        val moreData = fetchMoreData(i)
        println("$data + $moreData")
    }

    private suspend fun fetchData(i: Int): String {
        return withContext(Dispatchers.IO) {
            "Data $i"
        }
    }

    private suspend fun fetchMoreData(i: Int): String {
        return withContext(Dispatchers.IO) {
            "More data $i"
        }
    }
}