package dev.laraib.khidki.domain.session

import dev.laraib.khidki.domain.budget.SmsBudgetLedger
import dev.laraib.khidki.domain.model.AppState
import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuditEventType
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.AuthResult
import dev.laraib.khidki.domain.model.CandidateHandleResult
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.CommandHandleResult
import dev.laraib.khidki.domain.model.RuleMatchOutcome
import dev.laraib.khidki.domain.model.SendOutcome
import dev.laraib.khidki.domain.model.SessionState
import dev.laraib.khidki.domain.model.TerminalOutcome
import dev.laraib.khidki.domain.ports.AuditStore
import dev.laraib.khidki.domain.ports.BudgetLedger
import dev.laraib.khidki.domain.ports.Clock
import dev.laraib.khidki.domain.ports.ConfigurationRepository
import dev.laraib.khidki.domain.ports.RequestAuthenticator
import dev.laraib.khidki.domain.ports.RuleMatcher
import dev.laraib.khidki.domain.ports.SessionRepository
import dev.laraib.khidki.domain.ports.SmsTransport
import java.util.UUID

class ForwardingEngine(
    private val clock: Clock,
    private val sessionRepository: SessionRepository,
    private val configurationRepository: ConfigurationRepository,
    private val requestAuthenticator: RequestAuthenticator,
    private val ruleMatcher: RuleMatcher,
    private val budgetLedger: BudgetLedger,
    private val smsTransport: SmsTransport,
    private val auditStore: AuditStore,
) {
    private var appState: AppState = AppState.READY

    fun appState(): AppState = appState

    fun setAppState(state: AppState) {
        if (state == AppState.PAUSED && appState != AppState.PAUSED) {
            cancelActiveSession(TerminalOutcome.CANCELLED)
        }
        appState = state
    }

    fun handleCommand(requester: CanonicalPhone, password: String): CommandHandleResult {
        refreshSessions()

        auditStore.record(
            AuditEvent(
                type = AuditEventType.COMMAND_RECEIVED,
                atMillis = clock.nowMillis(),
                requester = requester,
            ),
        )

        if (appState == AppState.PAUSED) {
            rejectCommand(requester, "paused")
            return CommandHandleResult.AppPaused
        }
        if (appState != AppState.READY) {
            rejectCommand(requester, "not-ready")
            return CommandHandleResult.AppNotReady
        }

        when (val auth = requestAuthenticator.authenticate(requester, password)) {
            AuthResult.UnknownRequester -> {
                rejectCommand(requester, "unknown-requester")
                return CommandHandleResult.UnknownRequester
            }

            AuthResult.LockedOut -> {
                rejectCommand(requester, "locked-out")
                return CommandHandleResult.LockedOut
            }

            AuthResult.InvalidPassword -> {
                rejectCommand(requester, "invalid-password")
                return CommandHandleResult.AuthFailed
            }

            AuthResult.Success -> Unit
        }

        val configuration = configurationRepository.findByRequester(requester)
            ?: run {
                rejectCommand(requester, "missing-configuration")
                return CommandHandleResult.UnknownRequester
            }
        if (!configuration.enabled) {
            rejectCommand(requester, "disabled-configuration")
            return CommandHandleResult.UnknownRequester
        }

        val activeSession = sessionRepository.getActiveSession()
        if (activeSession != null && activeSession.isActive) {
            auditStore.record(
                AuditEvent(
                    type = AuditEventType.COMMAND_REJECTED,
                    atMillis = clock.nowMillis(),
                    requester = requester,
                    sessionId = activeSession.id,
                    detail = "active-session",
                ),
            )
            return CommandHandleResult.IgnoredActiveSession
        }
        if (activeSession != null && !activeSession.isActive) {
            sessionRepository.saveSession(null)
        }

        val windowSeconds = configuration.windowSeconds
        val ackBody = buildAckBody(configuration.label, windowSeconds)
        val ackParts = budgetLedger.estimateParts(ackBody)
        if (!budgetLedger.reserve(ackParts)) {
            rejectCommand(requester, "budget-exceeded")
            return CommandHandleResult.BudgetExceeded
        }

        val ackResult = smsTransport.send(requester, ackBody)
        if (ackResult.outcome != SendOutcome.SENT) {
            auditStore.record(
                AuditEvent(
                    type = AuditEventType.ACK_FAILED,
                    atMillis = clock.nowMillis(),
                    requester = requester,
                ),
            )
            return CommandHandleResult.AckFailed
        }

        auditStore.record(
            AuditEvent(
                type = AuditEventType.ACK_SENT,
                atMillis = clock.nowMillis(),
                requester = requester,
                detail = ackBody,
            ),
        )

        val now = clock.nowMillis()
        val session = AuthorizationSession(
            id = UUID.randomUUID(),
            configurationId = configuration.id,
            configurationVersion = configuration.version,
            requester = requester,
            label = configuration.label,
            filterRules = configuration.filterRules,
            state = SessionState.ARMED,
            armedAtMillis = now,
            expiresAtMillis = now + windowSeconds * 1_000L,
            bootId = clock.bootId(),
            windowSeconds = windowSeconds,
            configurationSnapshot = configuration,
        )
        sessionRepository.saveSession(session)
        auditStore.record(
            AuditEvent(
                type = AuditEventType.SESSION_ARMED,
                atMillis = now,
                requester = requester,
                sessionId = session.id,
            ),
        )
        return CommandHandleResult.SessionCreated(session)
    }

    fun handleCandidate(
        sender: String,
        body: String,
        receivedAtMillis: Long = clock.nowMillis(),
    ): CandidateHandleResult {
        if (appState == AppState.PAUSED) {
            return CandidateHandleResult.AppPaused
        }

        val session = sessionRepository.getActiveSession() ?: return CandidateHandleResult.NoActiveSession
        if (session.isExpired(receivedAtMillis)) {
            terminateSession(session, TerminalOutcome.EXPIRED)
            return CandidateHandleResult.SessionExpired
        }

        refreshSessions()

        val refreshedSession = sessionRepository.getActiveSession()
            ?: return CandidateHandleResult.SessionExpired
        if (refreshedSession.forwarded || refreshedSession.state == SessionState.SUBMITTED) {
            return CandidateHandleResult.AlreadyForwarded
        }
        if (refreshedSession.state == SessionState.SUBMITTING) {
            return CandidateHandleResult.AlreadyForwarded
        }

        val match = ruleMatcher.matches(
            rules = refreshedSession.filterRules,
            sender = sender,
            body = body,
            trustedRequesters = setOf(refreshedSession.requester.e164),
        )
        when (match.outcome) {
            RuleMatchOutcome.INVALID_RULES -> return CandidateHandleResult.NoMatch
            RuleMatchOutcome.EXCLUDED -> return CandidateHandleResult.NoMatch
            RuleMatchOutcome.NO_MATCH -> return CandidateHandleResult.NoMatch
            RuleMatchOutcome.BODY_TOO_LONG -> return CandidateHandleResult.OversizedMessage
            RuleMatchOutcome.MATCHED -> Unit
        }

        val forwardParts = budgetLedger.estimateParts(body)
        if (forwardParts > SmsBudgetLedger.MAX_PARTS_PER_MESSAGE) {
            return CandidateHandleResult.OversizedMessage
        }
        if (!budgetLedger.canReserve(forwardParts)) {
            return CandidateHandleResult.BudgetExceeded
        }

        val claimed = refreshedSession.copy(state = SessionState.CLAIMED, claimedAtMillis = receivedAtMillis)
        sessionRepository.saveSession(claimed)

        if (!budgetLedger.reserve(forwardParts)) {
            sessionRepository.saveSession(refreshedSession)
            return CandidateHandleResult.BudgetExceeded
        }

        val submitting = claimed.copy(state = SessionState.SUBMITTING)
        sessionRepository.saveSession(submitting)

        val sendResult = smsTransport.send(refreshedSession.requester, body)
        val finalSession = if (sendResult.outcome == SendOutcome.SENT) {
            submitting.copy(
                state = SessionState.SUBMITTED,
                forwarded = true,
                terminalOutcome = TerminalOutcome.COMPLETED,
                submittedAtMillis = clock.nowMillis(),
            )
        } else {
            submitting.copy(
                state = SessionState.SUBMITTED,
                forwarded = false,
                terminalOutcome = TerminalOutcome.FAILED,
                submittedAtMillis = clock.nowMillis(),
            )
        }
        sessionRepository.saveSession(finalSession)

        if (sendResult.outcome == SendOutcome.SENT) {
            auditStore.record(
                AuditEvent(
                    type = AuditEventType.CANDIDATE_FORWARDED,
                    atMillis = clock.nowMillis(),
                    requester = refreshedSession.requester,
                    sessionId = refreshedSession.id,
                ),
            )
            return CandidateHandleResult.Forwarded(finalSession)
        }

        auditStore.record(
            AuditEvent(
                type = AuditEventType.FORWARD_FAILED,
                atMillis = clock.nowMillis(),
                requester = refreshedSession.requester,
                sessionId = refreshedSession.id,
            ),
        )
        return CandidateHandleResult.ForwardFailed
    }

    fun cancelActiveWindow(): Boolean {
        refreshSessions()
        val session = sessionRepository.getActiveSession() ?: return false
        if (!session.isActive) {
            return false
        }
        terminateSession(session, TerminalOutcome.CANCELLED)
        return true
    }

    fun refreshSessions() {
        val session = sessionRepository.getActiveSession() ?: return

        if (session.state == SessionState.ARMED && session.bootId != clock.bootId()) {
            terminateSession(session, TerminalOutcome.EXPIRED)
            return
        }

        if (session.isExpired(clock.nowMillis()) && session.state != SessionState.SUBMITTED) {
            terminateSession(session, TerminalOutcome.EXPIRED)
        }
    }

    private fun cancelActiveSession(outcome: TerminalOutcome) {
        val session = sessionRepository.getActiveSession() ?: return
        if (session.isActive) {
            terminateSession(session, outcome)
        }
    }

    private fun terminateSession(session: AuthorizationSession, outcome: TerminalOutcome) {
        sessionRepository.terminateSession(session.id, outcome)
        val eventType = when (outcome) {
            TerminalOutcome.CANCELLED -> AuditEventType.SESSION_CANCELLED
            TerminalOutcome.EXPIRED -> AuditEventType.SESSION_EXPIRED
            else -> AuditEventType.SESSION_EXPIRED
        }
        auditStore.record(
            AuditEvent(
                type = eventType,
                atMillis = clock.nowMillis(),
                requester = session.requester,
                sessionId = session.id,
                detail = outcome.name,
            ),
        )
    }

    private fun rejectCommand(requester: CanonicalPhone, detail: String) {
        auditStore.record(
            AuditEvent(
                type = AuditEventType.COMMAND_REJECTED,
                atMillis = clock.nowMillis(),
                requester = requester,
                detail = detail,
            ),
        )
    }

    companion object {
        fun buildAckBody(label: String, windowSeconds: Int): String = "$label ${windowSeconds}s 1"
    }
}
