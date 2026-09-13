package dev.laraib.khidki.platform.notification

import androidx.test.core.app.ApplicationProvider
import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuditEventType
import dev.laraib.khidki.domain.model.CanonicalPhone
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class StatusNotifierTest {
    @Test
    fun onAuditEvent_doesNotCrashWithoutNotificationPermission() {
        val notifier = StatusNotifier(ApplicationProvider.getApplicationContext())
        notifier.onAuditEvent(
            AuditEvent(
                type = AuditEventType.SESSION_ARMED,
                atMillis = 1_000L,
                requester = CanonicalPhone("+919876543210"),
            ),
        )
        notifier.onAuditEvent(
            AuditEvent(
                type = AuditEventType.CANDIDATE_FORWARDED,
                atMillis = 2_000L,
                requester = CanonicalPhone("+919876543210"),
            ),
        )
    }
}
