package dev.laraib.khidki

import android.content.Context
import dev.laraib.khidki.data.KhidkiContainer
import dev.laraib.khidki.data.adapter.BlockingDomainBudgetLedger
import dev.laraib.khidki.data.adapter.DiagnosticAuditStore
import dev.laraib.khidki.data.adapter.BlockingAuditStore
import dev.laraib.khidki.data.adapter.BlockingSessionRepository
import dev.laraib.khidki.domain.model.AppState
import dev.laraib.khidki.domain.session.ForwardingEngine
import dev.laraib.khidki.platform.clock.AndroidSystemClock
import dev.laraib.khidki.platform.notification.ForwardingNotificationManager
import dev.laraib.khidki.platform.notification.StatusNotifier
import dev.laraib.khidki.platform.sms.AndroidSmsTransport
import dev.laraib.khidki.platform.sms.SmsInboundProcessor

class KhidkiRuntime private constructor(
    val container: KhidkiContainer,
    val clock: AndroidSystemClock,
    val engine: ForwardingEngine,
    val smsProcessor: SmsInboundProcessor,
) {
    val appPreferences = container.appPreferences

    fun onBootCompleted() {
        appPreferences.bootId = "boot-${clock.nowMillis()}"
        engine.refreshSessions()
    }

    fun refreshAppState(hasSmsPermission: Boolean) {
        when {
            !hasSmsPermission -> engine.setAppState(AppState.BLOCKED_PERMISSION)
            !appPreferences.isMasterEnabled -> engine.setAppState(AppState.PAUSED)
            else -> engine.setAppState(AppState.READY)
        }
    }

    companion object {
        @Volatile
        private var instance: KhidkiRuntime? = null

        fun get(context: Context): KhidkiRuntime =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): KhidkiRuntime {
            val container = KhidkiContainer.get(context)
            val clock = AndroidSystemClock(container.appPreferences)
            val sessionRepository = BlockingSessionRepository(
                delegate = container.sessionRepository,
                database = container.database,
                nowMillis = { clock.nowMillis() },
            )
            val budgetLedger = BlockingDomainBudgetLedger(container.budgetLedger, clock)
            val smsTransport = AndroidSmsTransport(context)
            val forwardingNotificationManager = ForwardingNotificationManager(
                context = context,
                sessionProvider = {
                    sessionRepository.getActiveSession()?.takeIf { it.isActive }
                },
            )
            val statusNotifier = StatusNotifier(context, forwardingNotificationManager)
            val auditStore = DiagnosticAuditStore(
                delegate = BlockingAuditStore(container.auditStore),
                statusNotifier = statusNotifier,
            )
            val engine = ForwardingEngine(
                clock = clock,
                sessionRepository = sessionRepository,
                budgetLedger = budgetLedger,
                smsTransport = smsTransport,
                auditStore = auditStore,
            )
            val runtime = KhidkiRuntime(
                container = container,
                clock = clock,
                engine = engine,
                smsProcessor = SmsInboundProcessor(
                    engine = engine,
                    duplicateFingerprintStore = container.duplicateFingerprintStore,
                    nowMillis = { clock.nowMillis() },
                ),
            )
            return runtime
        }
    }
}
