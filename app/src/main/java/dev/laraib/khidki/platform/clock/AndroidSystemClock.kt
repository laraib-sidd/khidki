package dev.laraib.khidki.platform.clock

import android.os.SystemClock
import dev.laraib.khidki.data.prefs.AppPreferences
import dev.laraib.khidki.domain.ports.Clock

class AndroidSystemClock(
    private val appPreferences: AppPreferences,
) : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun bootId(): String = appPreferences.bootId

    override fun monotonicNanos(): Long = SystemClock.elapsedRealtimeNanos()
}
