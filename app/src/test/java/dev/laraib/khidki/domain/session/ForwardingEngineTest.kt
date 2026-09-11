package dev.laraib.khidki.domain.session

import dev.laraib.khidki.domain.auth.AuthLockoutTracker
import dev.laraib.khidki.domain.auth.DefaultRequestAuthenticator
import dev.laraib.khidki.domain.budget.SmsBudgetLedger
import dev.laraib.khidki.domain.clock.FakeClock
import dev.laraib.khidki.domain.filter.Re2RuleMatcher
import dev.laraib.khidki.domain.model.AppState
import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.CandidateHandleResult
import dev.laraib.khidki.domain.model.CommandHandleResult
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.domain.model.ConfigurationVersion
import dev.laraib.khidki.domain.model.CredentialPolicy
import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.SendOutcome
import dev.laraib.khidki.domain.model.SendResult
import dev.laraib.khidki.domain.model.SessionOrigin
import dev.laraib.khidki.domain.model.SessionState
import dev.laraib.khidki.domain.model.TerminalOutcome
import dev.laraib.khidki.domain.model.TimedArmRejectReason
import dev.laraib.khidki.domain.model.TimedArmResult
import dev.laraib.khidki.domain.session.ForwardingEngine.Companion.MAX_TIMED_DURATION_SECONDS
import dev.laraib.khidki.domain.ports.AuditStore
import dev.laraib.khidki.domain.ports.ConfigurationRepository
import dev.laraib.khidki.domain.ports.CredentialVerifier
import dev.laraib.khidki.domain.ports.SessionRepository
import dev.laraib.khidki.domain.ports.SmsTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ForwardingEngineTest {
    private val requester = CanonicalPhone("+919876543210")
    private val password = "12345678"
    private val configId = ConfigurationId(UUID.randomUUID())

    private lateinit var clock: FakeClock
    private lateinit var sessionRepository: InMemorySessionRepository
    private lateinit var configurationRepository: InMemoryConfigurationRepository
    private lateinit var credentialVerifier: TestCredentialVerifier
    private lateinit var lockoutTracker: AuthLockoutTracker
    private lateinit var budgetLedger: SmsBudgetLedger
    private lateinit var smsTransport: RecordingSmsTransport
    private lateinit var auditStore: RecordingAuditStore
    private lateinit var engine: ForwardingEngine

    @Before
    fun setUp() {
        clock = FakeClock(currentMillis = 1_000L, currentBootId = "boot-1")
        sessionRepository = InMemorySessionRepository()
        configurationRepository = InMemoryConfigurationRepository(defaultConfiguration())
        credentialVerifier = TestCredentialVerifier(requester, password, clock)
        lockoutTracker = AuthLockoutTracker(clock)
        budgetLedger = SmsBudgetLedger(clock)
        smsTransport = RecordingSmsTransport()
        auditStore = RecordingAuditStore()

        engine = ForwardingEngine(
            clock = clock,
            sessionRepository = sessionRepository,
            configurationRepository = configurationRepository,
            requestAuthenticator = DefaultRequestAuthenticator(
                configurationRepository = configurationRepository,
                credentialVerifier = credentialVerifier,
                lockoutTracker = lockoutTracker,
            ),
            ruleMatcher = Re2RuleMatcher(),
            budgetLedger = budgetLedger,
            smsTransport = smsTransport,
            auditStore = auditStore,
        )
    }

    @Test
    fun handleCommand_successArmsSessionAndSendsAckWithoutPassword() {
        val result = engine.handleCommand(requester, password)

        assertTrue(result is CommandHandleResult.SessionCreated)
        val session = (result as CommandHandleResult.SessionCreated).session
        assertEquals(SessionState.ARMED, session.state)
        assertEquals("boot-1", session.bootId)
        assertEquals(1_000L + 120_000L, session.expiresAtMillis)

        assertEquals(1, smsTransport.sentMessages.size)
        val ack = smsTransport.sentMessages.first()
        assertEquals(requester, ack.to)
        assertEquals("TestLabel 120s 1", ack.body)
        assertFalse(ack.body.contains(password))
    }

    @Test
    fun handleCommand_unknownRequesterRejectedBeforePasswordCheck() {
        val unknown = CanonicalPhone("+919111111111")

        val result = engine.handleCommand(unknown, password)

        assertEquals(CommandHandleResult.UnknownRequester, result)
        assertTrue(smsTransport.sentMessages.isEmpty())
        assertEquals(0, lockoutTracker.failureCount(requester))
    }

    @Test
    fun handleCommand_authFailureLocksOutAfterFiveFailures() {
        repeat(4) {
            val result = engine.handleCommand(requester, "00000000")
            assertEquals(CommandHandleResult.AuthFailed, result)
        }
        assertFalse(lockoutTracker.isLocked(requester))

        val fifth = engine.handleCommand(requester, "00000000")
        assertEquals(CommandHandleResult.AuthFailed, fifth)
        assertTrue(lockoutTracker.isLocked(requester))

        val blocked = engine.handleCommand(requester, password)
        assertEquals(CommandHandleResult.LockedOut, blocked)
    }

    @Test
    fun handleCommand_ignoresSecondCommandWhileSessionActive() {
        val first = engine.handleCommand(requester, password)
        assertTrue(first is CommandHandleResult.SessionCreated)

        val second = engine.handleCommand(requester, password)
        assertEquals(CommandHandleResult.IgnoredActiveSession, second)
        assertEquals(1, smsTransport.sentMessages.size)
    }

    @Test
    fun handleCommand_reusableCredentialAfterSessionCompletes() {
        val first = engine.handleCommand(requester, password)
        assertTrue(first is CommandHandleResult.SessionCreated)

        val forward = engine.handleCandidate("VM-HDFCBK", "Your OTP is 654321")
        assertTrue(forward is CandidateHandleResult.Forwarded)

        val second = engine.handleCommand(requester, password)
        assertTrue(second is CommandHandleResult.SessionCreated)
        assertEquals(3, smsTransport.sentMessages.size)
    }

    @Test
    fun handleCommand_expiredCredentialRejected() {
        credentialVerifier.setExpiry(clock.nowMillis() + 1_000L)
        clock.advanceMillis(2_000L)

        val result = engine.handleCommand(requester, password)
        assertEquals(CommandHandleResult.AuthFailed, result)
    }

    @Test
    fun refreshSessions_expiresArmedSessionAfterWindow() {
        engine.handleCommand(requester, password)
        clock.advanceMillis(121_000L)

        engine.refreshSessions()

        val active = sessionRepository.getActiveSession()
        assertNull(active)
    }

    @Test
    fun refreshSessions_bootIdChangeExpiresArmedSession() {
        engine.handleCommand(requester, password)
        clock.setBootId("boot-2")

        engine.refreshSessions()

        assertNull(sessionRepository.getActiveSession())
    }

    @Test
    fun setAppState_pauseCancelsActiveSession() {
        engine.handleCommand(requester, password)
        engine.setAppState(AppState.PAUSED)

        assertNull(sessionRepository.getActiveSession())

        val command = engine.handleCommand(requester, password)
        assertEquals(CommandHandleResult.AppPaused, command)

        val candidate = engine.handleCandidate("VM-HDFCBK", "654321")
        assertEquals(CandidateHandleResult.AppPaused, candidate)
    }

    @Test
    fun handleCandidate_forwardsMatchingOtpOnce() {
        engine.handleCommand(requester, password)

        val first = engine.handleCandidate("VM-HDFCBK", "Your OTP is 654321")
        assertTrue(first is CandidateHandleResult.Forwarded)
        assertEquals(SessionState.SUBMITTED, (first as CandidateHandleResult.Forwarded).session.state)

        val second = engine.handleCandidate("VM-HDFCBK", "654321 again")
        assertEquals(CandidateHandleResult.AlreadyForwarded, second)
        assertEquals(2, smsTransport.sentMessages.size)
    }

    @Test
    fun handleCandidate_rejectsNonMatchingSender() {
        engine.handleCommand(requester, password)

        val result = engine.handleCandidate("OTHER", "654321")
        assertEquals(CandidateHandleResult.NoMatch, result)
        assertEquals(1, smsTransport.sentMessages.size)
    }

    @Test
    fun handleCandidate_rejectsWhenSessionExpired() {
        engine.handleCommand(requester, password)
        clock.advanceMillis(121_000L)

        val result = engine.handleCandidate("VM-HDFCBK", "654321")
        assertEquals(CandidateHandleResult.SessionExpired, result)
    }

    @Test
    fun armTimedWindow_successArmsSessionWithoutAckSms() {
        val result = engine.armTimedWindow(configId, 900)

        assertTrue(result is TimedArmResult.Success)
        val session = (result as TimedArmResult.Success).session
        assertEquals(SessionOrigin.TIMED, session.origin)
        assertEquals(SessionState.ARMED, session.state)
        assertEquals(requester, session.requester)
        assertEquals(0, session.forwardCount)
        assertEquals(1_000L + 900_000L, session.expiresAtMillis)
        assertTrue(smsTransport.sentMessages.isEmpty())
    }

    @Test
    fun armTimedWindow_rejectsDurationAboveMax() {
        val result = engine.armTimedWindow(configId, MAX_TIMED_DURATION_SECONDS + 1)
        assertTrue(result is TimedArmResult.Rejected)
        assertEquals(
            TimedArmRejectReason.DURATION_OUT_OF_RANGE,
            (result as TimedArmResult.Rejected).reason,
        )
    }

    @Test
    fun armTimedWindow_multiForwardStaysArmedAndIncrementsCount() {
        engine.armTimedWindow(configId, 900)

        val first = engine.handleCandidate("VM-HDFCBK", "Your OTP is 654321")
        assertTrue(first is CandidateHandleResult.Forwarded)
        val afterFirst = (first as CandidateHandleResult.Forwarded).session
        assertEquals(SessionState.ARMED, afterFirst.state)
        assertEquals(SessionOrigin.TIMED, afterFirst.origin)
        assertEquals(1, afterFirst.forwardCount)

        val second = engine.handleCandidate("VM-HDFCBK", "654321 again")
        assertTrue(second is CandidateHandleResult.Forwarded)
        assertEquals(2, (second as CandidateHandleResult.Forwarded).session.forwardCount)
        assertEquals(2, smsTransport.sentMessages.size)
        assertEquals(requester, smsTransport.sentMessages[0].to)
        assertEquals(requester, smsTransport.sentMessages[1].to)
    }

    @Test
    fun armTimedWindow_reqDuringTimedWindowReturnsIgnoredActiveSession() {
        engine.armTimedWindow(configId, 900)

        val reqDuringTimed = engine.handleCommand(requester, password)
        assertEquals(CommandHandleResult.IgnoredActiveSession, reqDuringTimed)
        assertTrue(smsTransport.sentMessages.isEmpty())
    }

    @Test
    fun armTimedWindow_cancelRecordsTimedCancellation() {
        engine.armTimedWindow(configId, 900)
        assertTrue(engine.cancelTimedWindow())

        assertNull(sessionRepository.getActiveSession())
    }

    @Test
    fun armTimedWindow_expiryTerminatesTimedSession() {
        engine.armTimedWindow(configId, 60)
        clock.advanceMillis(61_000L)

        engine.refreshSessions()

        assertNull(sessionRepository.getActiveSession())
    }

    @Test
    fun handleCommand_ackFailureDoesNotCreateSession() {
        smsTransport.failNextSend = true

        val result = engine.handleCommand(requester, password)

        assertEquals(CommandHandleResult.AckFailed, result)
        assertNull(sessionRepository.getActiveSession())
    }

    private fun defaultConfiguration(): Configuration =
        Configuration(
            id = configId,
            version = ConfigurationVersion(1),
            label = "TestLabel",
            requester = requester,
            filterRules = FilterRules(
                senderPatterns = listOf("HDFCBK"),
                contentPatterns = listOf("\\d{6}"),
            ),
            windowSeconds = 120,
            credentialPolicy = CredentialPolicy(windowSeconds = 120),
        )

    private class InMemorySessionRepository : SessionRepository {
        private var activeSession: AuthorizationSession? = null

        override fun getActiveSession(): AuthorizationSession? =
            activeSession?.takeUnless {
                it.terminalOutcome == TerminalOutcome.EXPIRED ||
                    it.terminalOutcome == TerminalOutcome.CANCELLED
            }

        override fun saveSession(session: AuthorizationSession?) {
            activeSession = session
        }

        override fun terminateSession(sessionId: UUID, outcome: TerminalOutcome): AuthorizationSession? {
            val current = activeSession ?: return null
            if (current.id != sessionId) {
                return null
            }
            val terminated = current.copy(
                state = SessionState.SUBMITTED,
                terminalOutcome = outcome,
            )
            activeSession = terminated
            return terminated
        }
    }

    private class InMemoryConfigurationRepository(
        private val configuration: Configuration,
    ) : ConfigurationRepository {
        override fun findByRequester(requester: CanonicalPhone): Configuration? =
            if (requester == configuration.requester) configuration else null

        override fun findById(id: ConfigurationId): Configuration? =
            if (id == configuration.id) configuration else null

        override fun findAll(): List<Configuration> = listOf(configuration)
    }

    private class TestCredentialVerifier(
        private val requester: CanonicalPhone,
        private val password: String,
        private val clock: FakeClock,
    ) : CredentialVerifier {
        private var expiryMillis: Long = clock.nowMillis() + CredentialPolicy.DEFAULT_LIFETIME_MILLIS

        fun setExpiry(expiryMillis: Long) {
            this.expiryMillis = expiryMillis
        }

        override fun verify(requester: CanonicalPhone, password: String): Boolean {
            if (clock.nowMillis() >= expiryMillis) {
                return false
            }
            return requester == this.requester && constantTimeEquals(password, this.password)
        }

        override fun isKnownRequester(requester: CanonicalPhone): Boolean = requester == this.requester

        private fun constantTimeEquals(left: String, right: String): Boolean {
            if (left.length != right.length) {
                return false
            }
            var diff = 0
            for (index in left.indices) {
                diff = diff or (left[index].code xor right[index].code)
            }
            return diff == 0
        }
    }

    private class RecordingSmsTransport : SmsTransport {
        data class SentMessage(val to: CanonicalPhone, val body: String)

        val sentMessages = mutableListOf<SentMessage>()
        var failNextSend: Boolean = false

        override fun send(to: CanonicalPhone, body: String): SendResult {
            if (failNextSend) {
                failNextSend = false
                return SendResult(outcome = SendOutcome.FAILED, parts = 1)
            }
            sentMessages.add(SentMessage(to, body))
            return SendResult(outcome = SendOutcome.SENT, parts = 1)
        }
    }

    private class RecordingAuditStore : AuditStore {
        val events = mutableListOf<AuditEvent>()

        override fun record(event: AuditEvent) {
            events.add(event)
        }
    }
}
