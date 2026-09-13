package dev.laraib.khidki.data.repository

import dev.laraib.khidki.data.db.dao.LockoutDao
import dev.laraib.khidki.data.db.entities.LockoutEntity
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.data.ports.PersistenceLockoutStore

class RoomLockoutStore(
    private val lockoutDao: LockoutDao,
    private val maxFailures: Int = MAX_FAILURES,
    private val lockoutDurationMs: Long = LOCKOUT_DURATION_MS,
    private val failureWindowMs: Long = FAILURE_WINDOW_MS,
) : PersistenceLockoutStore {
    override suspend fun isLocked(requester: CanonicalPhone, nowMillis: Long): Boolean {
        val lockout = lockoutDao.getByRequester(requester.e164) ?: return false
        if (lockout.lockedUntilMillis > nowMillis) {
            return true
        }
        if (nowMillis - lockout.windowStartMillis > failureWindowMs) {
            lockoutDao.deleteByRequester(requester.e164)
        }
        return false
    }

    override suspend fun recordFailure(requester: CanonicalPhone, nowMillis: Long): Boolean {
        val existing = lockoutDao.getByRequester(requester.e164)
        val withinWindow = existing != null && nowMillis - existing.windowStartMillis <= failureWindowMs
        val nextCount = if (withinWindow) existing.failureCount + 1 else 1
        val windowStart = if (withinWindow) existing.windowStartMillis else nowMillis
        val lockedUntil = if (nextCount >= maxFailures) {
            nowMillis + lockoutDurationMs
        } else {
            0L
        }
        lockoutDao.upsert(
            LockoutEntity(
                requesterE164 = requester.e164,
                failureCount = nextCount,
                windowStartMillis = windowStart,
                lockedUntilMillis = lockedUntil,
            ),
        )
        return lockedUntil > nowMillis
    }

    override suspend fun clearFailures(requester: CanonicalPhone) {
        lockoutDao.deleteByRequester(requester.e164)
    }

    private companion object {
        const val MAX_FAILURES = 5
        const val LOCKOUT_DURATION_MS = 15L * 60L * 1000L
        const val FAILURE_WINDOW_MS = 15L * 60L * 1000L
    }
}
