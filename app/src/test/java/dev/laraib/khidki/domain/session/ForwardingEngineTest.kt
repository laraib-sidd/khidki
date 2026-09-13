package dev.laraib.khidki.domain.session

import dev.laraib.khidki.domain.budget.SmsBudgetLedger
import dev.laraib.khidki.domain.clock.FakeClock
import dev.laraib.khidki.domain.filter.ForwardingPolicy
import dev.laraib.khidki.domain.model.AppState
import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.DestinationSnapshot
import dev.laraib.khidki.domain.model.CandidateHandleResult
import dev.laraib.khidki.domain.model.SendOutcome
import dev.laraib.khidki.domain.model.SendResult
import dev.laraib.khidki.domain.model.SessionOrigin
import dev.laraib.khidki.domain.model.SessionState
import dev.laraib.khidki.domain.model.TerminalOutcome
import dev.laraib.khidki.domain.model.TimedArmRejectReason
import dev.laraib.khidki.domain.model.TimedArmResult
import dev.laraib.khidki.domain.session.ForwardingEngine.Companion.MAX_TIMED_DURATION_SECONDS
import dev.laraib.khidki.domain.ports.AuditStore
import dev.laraib.khidki.domain.ports.SessionRepository
import dev.laraib.khidki.domain.ports.SmsTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ForwardingEngineTest {
    private val requester = CanonicalPhone("+919876543210")
    private val banksPolicy = ForwardingPolicy(otpBanks = true, otpOther = true)

    private lateinit var clock: FakeClock
    private lateinit var sessionRepository: InMemorySessionRepository
    private lateinit var budgetLedger: SmsBudgetLedger
    private lateinit var smsTransport: RecordingSmsTransport
    private lateinit var auditStore: RecordingAuditStore
    private lateinit var engine: ForwardingEngine

    @Before
    fun setUp() {
        clock = FakeClock(currentMillis = 1_000L, currentBootId = "boot-1")
        sessionRepository = InMemorySessionRepository()
        budgetLedger = SmsBudgetLedger(clock)
        smsTransport = RecordingSmsTransport()
        auditStore = RecordingAuditStore()

        engine = ForwardingEngine(
            clock = clock,
            sessionRepository = sessionRepository,
            budgetLedger = budgetLedger,
            smsTransport = smsTransport,
            auditStore = auditStore,
        )
    }

    @Test
    fun refreshSessions_expiresArmedSessionAfterWindow() {
        engine.armTimedWindow(defaultDestinations(), 120)
        clock.advanceMillis(121_000L)

        engine.refreshSessions()

        assertNull(sessionRepository.getActiveSession())
    }

    @Test
    fun refreshSessions_bootIdChangeExpiresArmedSession() {
        engine.armTimedWindow(defaultDestinations(), 900)
        clock.setBootId("boot-2")

        engine.refreshSessions()

        assertNull(sessionRepository.getActiveSession())
    }

    @Test
    fun setAppState_pauseCancelsActiveSession() {
        engine.armTimedWindow(defaultDestinations(), 900)
        engine.setAppState(AppState.PAUSED)

        assertNull(sessionRepository.getActiveSession())

        val candidate = engine.handleCandidate("VM-HDFCBK", "654321")
        assertEquals(CandidateHandleResult.AppPaused, candidate)
    }

    @Test
    fun handleCandidate_rejectsNonMatchingSender() {
        engine.armTimedWindow(defaultDestinations(), 900)

        val result = engine.handleCandidate("OTHER", "654321")
        assertEquals(CandidateHandleResult.NoMatch, result)
        assertTrue(smsTransport.sentMessages.isEmpty())
    }

    @Test
    fun handleCandidate_rejectsWhenSessionExpired() {
        engine.armTimedWindow(defaultDestinations(), 120)
        clock.advanceMillis(121_000L)

        val result = engine.handleCandidate("VM-HDFCBK", "654321")
        assertEquals(CandidateHandleResult.SessionExpired, result)
    }

    @Test
    fun armTimedWindow_successArmsSessionWithoutAckSms() {
        val result = engine.armTimedWindow(defaultDestinations(), 900)

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
        val result = engine.armTimedWindow(defaultDestinations(), MAX_TIMED_DURATION_SECONDS + 1)
        assertTrue(result is TimedArmResult.Rejected)
        assertEquals(
            TimedArmRejectReason.DURATION_OUT_OF_RANGE,
            (result as TimedArmResult.Rejected).reason,
        )
    }

    @Test
    fun armTimedWindow_multiForwardStaysArmedAndIncrementsCount() {
        engine.armTimedWindow(defaultDestinations(), 900)

        val first = engine.handleCandidate("VM-HDFCBK", "Your OTP is 654321")
        assertTrue(first is CandidateHandleResult.Forwarded)
        val afterFirst = (first as CandidateHandleResult.Forwarded).session
        assertEquals(SessionState.ARMED, afterFirst.state)
        assertEquals(SessionOrigin.TIMED, afterFirst.origin)
        assertEquals(1, afterFirst.forwardCount)

        val second = engine.handleCandidate("VM-HDFCBK", "OTP 112233 for login")
        assertTrue(second is CandidateHandleResult.Forwarded)
        assertEquals(2, (second as CandidateHandleResult.Forwarded).session.forwardCount)
        assertEquals(2, smsTransport.sentMessages.size)
        assertEquals(requester, smsTransport.sentMessages[0].to)
        assertEquals(requester, smsTransport.sentMessages[1].to)
    }

    @Test
    fun armTimedWindow_cancelRecordsTimedCancellation() {
        engine.armTimedWindow(defaultDestinations(), 900)
        assertTrue(engine.cancelTimedWindow())

        assertNull(sessionRepository.getActiveSession())
    }

    @Test
    fun armTimedWindow_expiryTerminatesTimedSession() {
        engine.armTimedWindow(defaultDestinations(), 60)
        clock.advanceMillis(61_000L)

        engine.refreshSessions()

        assertNull(sessionRepository.getActiveSession())
    }

    @Test
    fun policyArm_epfDropsWhenOnlyBanksEnabled() {
        val banksOnly = ForwardingPolicy(otpBanks = true)
        engine.armTimedWindow(defaultDestinations(banksOnly), 900)

        val result = engine.handleCandidate("VM-EPFOHO", "Your EPF OTP is 123456")
        assertEquals(CandidateHandleResult.NoMatch, result)
        assertTrue(smsTransport.sentMessages.isEmpty())
    }

    @Test
    fun policyArm_epfForwardsWhenGovernmentEnabled() {
        val government = ForwardingPolicy(otpGovernment = true)
        engine.armTimedWindow(defaultDestinations(government), 900)

        val result = engine.handleCandidate("VM-EPFOHO", "Your EPF OTP is 123456")
        assertTrue(result is CandidateHandleResult.Forwarded)
    }

    @Test
    fun multiDestination_momGetsBanksBrotherGetsShopping() {
        val mom = CanonicalPhone("+919111111111")
        val brother = CanonicalPhone("+919222222222")
        val destinations =
            listOf(
                DestinationSnapshot(
                    "mom",
                    "Mom",
                    mom,
                    ForwardingPolicy(
                        otpBanks = true,
                        otpShopping = false,
                        otpUpi = false,
                        otpOther = false,
                    ),
                ),
                DestinationSnapshot(
                    "bro",
                    "Brother",
                    brother,
                    ForwardingPolicy(
                        otpShopping = true,
                        otpBanks = false,
                        otpUpi = false,
                        otpOther = false,
                    ),
                ),
            )
        engine.armTimedWindow(destinations, 900)

        val bankResult = engine.handleCandidate("VM-HDFCBK", "Your OTP is 654321")
        assertTrue(bankResult is CandidateHandleResult.Forwarded)
        assertEquals(1, (bankResult as CandidateHandleResult.Forwarded).session.forwardCount)
        assertEquals(1, smsTransport.sentMessages.size)
        assertEquals(mom, smsTransport.sentMessages[0].to)

        val shopResult = engine.handleCandidate("AM-AMAZON", "Your Amazon OTP is 112233")
        assertTrue(shopResult is CandidateHandleResult.Forwarded)
        assertEquals(2, (shopResult as CandidateHandleResult.Forwarded).session.forwardCount)
        assertEquals(2, smsTransport.sentMessages.size)
        assertEquals(brother, smsTransport.sentMessages[1].to)
    }

    @Test
    fun fanOut_fourDestinationsOnePartOtp_sendsToAll() {
        val destinations =
            (1..4).map { index ->
                DestinationSnapshot(
                    id = "p$index",
                    name = "Person $index",
                    phone = CanonicalPhone("+91987654321$index"),
                    policy = ForwardingPolicy(otpBanks = true),
                )
            }
        engine.armTimedWindow(destinations, 900)

        val result = engine.handleCandidate("VM-HDFCBK", "Your OTP is 654321")
        assertTrue(result is CandidateHandleResult.Forwarded)
        assertEquals(4, smsTransport.sentMessages.size)
        assertEquals(4, (result as CandidateHandleResult.Forwarded).session.forwardCount)
    }

    @Test
    fun untilStop_doesNotAutoExpire() {
        val policy = ForwardingPolicy(otpBanks = true)
        val result = engine.armTimedWindow(
            listOf(DestinationSnapshot("one", "One", requester, policy)),
            ForwardingEngine.DURATION_UNTIL_STOP,
        )
        assertTrue(result is TimedArmResult.Success)
        val session = (result as TimedArmResult.Success).session
        assertTrue(session.untilStop)

        clock.advanceMillis(24 * 60 * 60 * 1_000L)
        engine.refreshSessions()

        assertNotNull(sessionRepository.getActiveSession())
    }

    private fun defaultDestinations(policy: ForwardingPolicy = banksPolicy): List<DestinationSnapshot> =
        listOf(
            DestinationSnapshot(
                id = "one",
                name = "One",
                phone = requester,
                policy = policy,
            ),
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
