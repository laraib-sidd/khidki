package dev.laraib.khidki.data.adapter

import dev.laraib.khidki.data.ports.PersistenceBudgetLedger
import dev.laraib.khidki.domain.budget.SmsBudgetLedger
import dev.laraib.khidki.domain.ports.BudgetLedger
import dev.laraib.khidki.domain.ports.Clock

class BlockingDomainBudgetLedger(
    private val delegate: PersistenceBudgetLedger,
    private val clock: Clock,
    private val maxPartsPerMessage: Int = SmsBudgetLedger.MAX_PARTS_PER_MESSAGE,
    private val maxPartsPerDay: Int = SmsBudgetLedger.MAX_PARTS_PER_DAY,
) : BudgetLedger {
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
        if (parts <= 0 || parts > maxPartsPerMessage) {
            return false
        }
        val used = Blocking.io { delegate.countReservedPartsInWindow(clock.nowMillis()) }
        return used + parts <= maxPartsPerDay
    }

    override fun reserve(parts: Int): Boolean {
        if (!canReserve(parts)) {
            return false
        }
        return Blocking.io {
            delegate.reserveParts(parts, sessionId = null, nowMillis = clock.nowMillis()) != null
        }
    }

    override fun partsUsedInWindow(): Int =
        Blocking.io { delegate.countReservedPartsInWindow(clock.nowMillis()) }
}
