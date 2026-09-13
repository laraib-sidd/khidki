package dev.laraib.khidki.domain.auth

import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.ports.Clock

class AuthLockoutTracker(
    private val clock: Clock,
    private val maxFailures: Int = DEFAULT_MAX_FAILURES,
    private val lockoutDurationMillis: Long = DEFAULT_LOCKOUT_MILLIS,
) : LockoutTracker {
    private val failureTimestamps = mutableMapOf<CanonicalPhone, MutableList<Long>>()
    private val lockedUntil = mutableMapOf<CanonicalPhone, Long>()

    override fun isLocked(requester: CanonicalPhone): Boolean {
        val until = lockedUntil[requester] ?: return false
        val now = clock.nowMillis()
        if (now >= until) {
            lockedUntil.remove(requester)
            failureTimestamps.remove(requester)
            return false
        }
        return true
    }

    override fun recordFailure(requester: CanonicalPhone) {
        if (isLocked(requester)) {
            return
        }

        val now = clock.nowMillis()
        val windowStart = now - lockoutDurationMillis
        val timestamps = failureTimestamps.getOrPut(requester) { mutableListOf() }
        timestamps.removeAll { it < windowStart }
        timestamps.add(now)

        if (timestamps.size >= maxFailures) {
            lockedUntil[requester] = now + lockoutDurationMillis
        }
    }

    override fun recordSuccess(requester: CanonicalPhone) {
        failureTimestamps.remove(requester)
        lockedUntil.remove(requester)
    }

    fun failureCount(requester: CanonicalPhone): Int {
        val now = clock.nowMillis()
        val windowStart = now - lockoutDurationMillis
        return failureTimestamps[requester]?.count { it >= windowStart } ?: 0
    }

    fun clear() {
        failureTimestamps.clear()
        lockedUntil.clear()
    }

    companion object {
        const val DEFAULT_MAX_FAILURES: Int = 5
        const val DEFAULT_LOCKOUT_MILLIS: Long = 15L * 60L * 1000L
    }
}
