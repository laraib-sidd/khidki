package dev.laraib.khidki.data.adapter

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

internal object Blocking {
    fun <T> io(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }
}
