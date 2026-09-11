package dev.laraib.khidki.domain.auth

import dev.laraib.khidki.domain.model.CanonicalPhone

interface LockoutTracker {
    fun isLocked(requester: CanonicalPhone): Boolean

    fun recordFailure(requester: CanonicalPhone)

    fun recordSuccess(requester: CanonicalPhone)
}
