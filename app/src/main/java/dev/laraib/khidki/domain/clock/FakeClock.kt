package dev.laraib.khidki.domain.clock

import dev.laraib.khidki.domain.ports.Clock

class FakeClock(
    private var currentMillis: Long = 0L,
    private var currentBootId: String = "boot-0",
    private var currentMonotonicNanos: Long = 0L,
) : Clock {
    override fun nowMillis(): Long = currentMillis

    override fun bootId(): String = currentBootId

    override fun monotonicNanos(): Long = currentMonotonicNanos

    fun setMillis(millis: Long) {
        currentMillis = millis
    }

    fun advanceMillis(deltaMillis: Long) {
        currentMillis += deltaMillis
    }

    fun setBootId(bootId: String) {
        currentBootId = bootId
    }

    fun setMonotonicNanos(nanos: Long) {
        currentMonotonicNanos = nanos
    }
}
