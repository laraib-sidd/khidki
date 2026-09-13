package dev.laraib.khidki.domain.budget

import dev.laraib.khidki.domain.ports.BudgetLedger
import dev.laraib.khidki.domain.ports.Clock

class SmsBudgetLedger(
    private val clock: Clock,
    private val maxPartsPerMessage: Int = MAX_PARTS_PER_MESSAGE,
    private val maxPartsPerDay: Int = MAX_PARTS_PER_DAY,
    private val rollingWindowMillis: Long = ROLLING_WINDOW_MILLIS,
) : BudgetLedger {
    private data class Reservation(val parts: Int, val atMillis: Long)

    private val reservations = mutableListOf<Reservation>()

    override fun estimateParts(body: String): Int {
        if (body.isEmpty()) {
            return 1
        }
        val singlePartLimit = 160
        val multipartLimit = 153
        return when {
            body.length <= singlePartLimit -> 1
            else -> {
                val remaining = body.length - singlePartLimit
                1 + ((remaining + multipartLimit - 1) / multipartLimit)
            }
        }
    }

    override fun canReserve(parts: Int): Boolean {
        if (parts <= 0) {
            return false
        }
        if (parts > maxPartsPerMessage) {
            return false
        }
        pruneExpired()
        return partsUsedInWindow() + parts <= maxPartsPerDay
    }

    override fun reserve(parts: Int): Boolean {
        if (!canReserve(parts)) {
            return false
        }
        reservations.add(Reservation(parts = parts, atMillis = clock.nowMillis()))
        return true
    }

    override fun partsUsedInWindow(): Int {
        pruneExpired()
        return reservations.sumOf { it.parts }
    }

    fun exceedsMessageLimit(body: String): Boolean = estimateParts(body) > maxPartsPerMessage

    private fun pruneExpired() {
        val cutoff = clock.nowMillis() - rollingWindowMillis
        reservations.removeAll { it.atMillis < cutoff }
    }

    companion object {
        const val MAX_PARTS_PER_MESSAGE: Int = 3
        const val MAX_PARTS_PER_DAY: Int = 20
        const val ROLLING_WINDOW_MILLIS: Long = 24L * 60L * 60L * 1000L
    }
}
