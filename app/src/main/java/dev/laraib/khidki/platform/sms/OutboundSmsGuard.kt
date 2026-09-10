package dev.laraib.khidki.platform.sms

import java.util.concurrent.ConcurrentHashMap

object OutboundSmsGuard {
    private const val MAX_ENTRIES = 100
    private val recentBodies = ConcurrentHashMap.newKeySet<String>()

    fun remember(body: String) {
        if (recentBodies.size >= MAX_ENTRIES) {
            recentBodies.clear()
        }
        recentBodies.add(body.trim())
    }

    fun wasRecentlySent(body: String): Boolean = recentBodies.contains(body.trim())
}
