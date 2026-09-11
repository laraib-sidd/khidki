package dev.laraib.khidki.platform.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object DiagnosticEventBus {
    private const val MAX_EVENTS = 50
    private val lock = ReentrantLock()
    private val buffer = ArrayDeque<String>(MAX_EVENTS)
    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events: StateFlow<List<String>> = _events.asStateFlow()

    fun record(message: String) {
        val entry = "[${timestamp()}] $message"
        lock.withLock {
            if (buffer.size >= MAX_EVENTS) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
            _events.value = buffer.toList()
        }
    }

    fun clear() {
        lock.withLock {
            buffer.clear()
            _events.value = emptyList()
        }
    }

    private fun timestamp(): String {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)
        return formatter.format(Date())
    }
}
