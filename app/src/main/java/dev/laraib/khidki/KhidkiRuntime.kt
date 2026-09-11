package dev.laraib.khidki

import android.content.Context
import dev.laraib.khidki.data.KhidkiContainer
import dev.laraib.khidki.data.adapter.DiagnosticAuditStore
import dev.laraib.khidki.data.adapter.BlockingAuditStore
import dev.laraib.khidki.data.adapter.BlockingConfigurationRepository
import dev.laraib.khidki.data.adapter.BlockingSessionRepository
import dev.laraib.khidki.data.adapter.RoomDomainCredentialVerifier
import dev.laraib.khidki.domain.auth.AuthLockoutTracker
import dev.laraib.khidki.domain.auth.DefaultRequestAuthenticator
import dev.laraib.khidki.domain.budget.SmsBudgetLedger
import dev.laraib.khidki.domain.filter.Re2RuleMatcher
import dev.laraib.khidki.domain.model.AppState
import dev.laraib.khidki.domain.session.ForwardingEngine
import dev.laraib.khidki.platform.clock.AndroidSystemClock
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
            val configurationRepository = BlockingConfigurationRepository(container.configurationRepository)
            val sessionRepository = BlockingSessionRepository(
                delegate = container.sessionRepository,
                database = container.database,
                nowMillis = { clock.nowMillis() },
            )
            val auditStore = DiagnosticAuditStore(BlockingAuditStore(container.auditStore))
            val credentialVerifier = RoomDomainCredentialVerifier(
                database = container.database,
                keystore = container.credentialVerifier,
                nowMillis = { clock.nowMillis() },
            )
            val lockoutTracker = AuthLockoutTracker(clock)
            val requestAuthenticator = DefaultRequestAuthenticator(
                configurationRepository = configurationRepository,
                credentialVerifier = credentialVerifier,
                lockoutTracker = lockoutTracker,
            )
            val budgetLedger = SmsBudgetLedger(clock)
            val smsTransport = AndroidSmsTransport(context)
            val engine = ForwardingEngine(
                clock = clock,
                sessionRepository = sessionRepository,
                configurationRepository = configurationRepository,
                requestAuthenticator = requestAuthenticator,
                ruleMatcher = Re2RuleMatcher(),
                budgetLedger = budgetLedger,
                smsTransport = smsTransport,
                auditStore = auditStore,
            )
            val runtime = KhidkiRuntime(
                container = container,
                clock = clock,
                engine = engine,
                smsProcessor = SmsInboundProcessor(engine),
            )
            return runtime
        }
    }
}
