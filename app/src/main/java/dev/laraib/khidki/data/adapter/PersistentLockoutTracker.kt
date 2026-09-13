package dev.laraib.khidki.data.adapter

import dev.laraib.khidki.data.ports.PersistenceLockoutStore
import dev.laraib.khidki.domain.auth.LockoutTracker
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.ports.Clock

class PersistentLockoutTracker(
    private val delegate: PersistenceLockoutStore,
    private val clock: Clock,
) : LockoutTracker {
    override fun isLocked(requester: CanonicalPhone): Boolean =
        Blocking.io { delegate.isLocked(requester, clock.nowMillis()) }

    override fun recordFailure(requester: CanonicalPhone) {
        Blocking.io { delegate.recordFailure(requester, clock.nowMillis()) }
    }

    override fun recordSuccess(requester: CanonicalPhone) {
        Blocking.io { delegate.clearFailures(requester) }
    }
}
