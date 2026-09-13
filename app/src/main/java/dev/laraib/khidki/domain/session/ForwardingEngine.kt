package dev.laraib.khidki.domain.session

import dev.laraib.khidki.domain.budget.SmsBudgetLedger
import dev.laraib.khidki.domain.filter.ForwardingPolicy
import dev.laraib.khidki.domain.filter.ForwardingPresets
import dev.laraib.khidki.domain.model.AppState
import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuditEventType
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.CandidateHandleResult
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.ConfigurationVersion
import dev.laraib.khidki.domain.model.DestinationSnapshot
import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.SessionOrigin
import dev.laraib.khidki.domain.model.TimedArmRejectReason
import dev.laraib.khidki.domain.model.TimedArmResult
import dev.laraib.khidki.domain.model.SendOutcome
import dev.laraib.khidki.domain.model.SessionState
import dev.laraib.khidki.domain.model.TerminalOutcome
import dev.laraib.khidki.domain.ports.AuditStore
import dev.laraib.khidki.domain.ports.BudgetLedger
import dev.laraib.khidki.domain.ports.Clock
import dev.laraib.khidki.domain.ports.SessionRepository
import dev.laraib.khidki.domain.ports.SmsTransport
import java.util.UUID

class ForwardingEngine(
    private val clock: Clock,
    private val sessionRepository: SessionRepository,
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

    fun armTimedWindow(
        destinations: List<DestinationSnapshot>,
        durationSeconds: Int,
    ): TimedArmResult {
        refreshSessions()

        if (appState == AppState.PAUSED) {
            return TimedArmResult.Rejected(TimedArmRejectReason.APP_PAUSED)
        }
        if (appState != AppState.READY) {
            return TimedArmResult.Rejected(TimedArmRejectReason.APP_NOT_READY)
        }
        if (destinations.isEmpty()) {
            return TimedArmResult.Rejected(TimedArmRejectReason.NO_DESTINATION)
        }
        if (!isValidDuration(durationSeconds)) {
            return TimedArmResult.Rejected(TimedArmRejectReason.DURATION_OUT_OF_RANGE)
        }
        if (destinations.none { it.policy.hasAnyEnabled() }) {
            return TimedArmResult.Rejected(TimedArmRejectReason.NO_CATEGORIES_ENABLED)
        }

        val activeSession = sessionRepository.getActiveSession()
        if (activeSession != null && activeSession.isActive) {
            return TimedArmResult.Rejected(TimedArmRejectReason.ACTIVE_SESSION_EXISTS)
        }
        if (activeSession != null && !activeSession.isActive) {
            sessionRepository.saveSession(null)
        }

        val untilStop = durationSeconds == DURATION_UNTIL_STOP
        val now = clock.nowMillis()
        val expiresAt =
            if (untilStop) {
                Long.MAX_VALUE
            } else {
                now + durationSeconds * 1_000L
            }
        val primaryDestination = destinations.first()
        val session = AuthorizationSession(
            id = UUID.randomUUID(),
            configurationId = TimedSessionDefaults.PLACEHOLDER_CONFIG_ID,
            configurationVersion = ConfigurationVersion(1),
            requester = primaryDestination.phone,
            label = TimedSessionDefaults.LABEL,
            filterRules = FilterRules(senderPatterns = emptyList(), contentPatterns = emptyList()),
            state = SessionState.ARMED,
            armedAtMillis = now,
            expiresAtMillis = expiresAt,
            bootId = clock.bootId(),
            windowSeconds = if (untilStop) 0 else durationSeconds,
            origin = SessionOrigin.TIMED,
            forwardCount = 0,
            forwardingPolicy = primaryDestination.policy,
            destinationSnapshots = destinations,
            untilStop = untilStop,
        )
        sessionRepository.saveSession(session)
        auditStore.record(
            AuditEvent(
                type = AuditEventType.TIMED_ARMED,
                atMillis = now,
                requester = primaryDestination.phone,
                sessionId = session.id,
                detail = timedArmDetail(durationSeconds, destinations.size),
            ),
        )
        return TimedArmResult.Success(session)
    }



    fun cancelTimedWindow(): Boolean {
        refreshSessions()
        val session = sessionRepository.getActiveSession() ?: return false
        if (!session.isActive || session.origin != SessionOrigin.TIMED) {
            return false
        }
        terminateSession(session, TerminalOutcome.CANCELLED)
        return true
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
        if (refreshedSession.state == SessionState.SUBMITTING) {
            return CandidateHandleResult.AlreadyForwarded
        }

        return handleTimedCandidate(refreshedSession, sender, body, receivedAtMillis)
    }

    private fun handleTimedCandidate(
        session: AuthorizationSession,
        sender: String,
        body: String,
        receivedAtMillis: Long,
    ): CandidateHandleResult {
        val matchingDestinations =
            session.activeDestinations().filter { destination ->
                ForwardingPresets.matches(sender, body, destination.policy)
            }
        if (matchingDestinations.isEmpty()) {
            return CandidateHandleResult.NoMatch
        }

        val forwardParts = budgetLedger.estimateParts(body)
        if (forwardParts > SmsBudgetLedger.MAX_PARTS_PER_MESSAGE) {
            return CandidateHandleResult.OversizedMessage
        }

        val claimed = session.copy(state = SessionState.CLAIMED, claimedAtMillis = receivedAtMillis)
        sessionRepository.saveSession(claimed)

        val submitting = claimed.copy(state = SessionState.SUBMITTING)
        sessionRepository.saveSession(submitting)

        var successCount = 0
        var budgetBlocked = false
        val now = clock.nowMillis()
        for (destination in matchingDestinations) {
            if (!budgetLedger.canReserve(forwardParts)) {
                budgetBlocked = true
                auditStore.record(
                    AuditEvent(
                        type = AuditEventType.FORWARD_FAILED,
                        atMillis = now,
                        requester = destination.phone,
                        sessionId = session.id,
                        detail = "budget-exceeded",
                    ),
                )
                continue
            }
            if (!budgetLedger.reserve(forwardParts)) {
                budgetBlocked = true
                auditStore.record(
                    AuditEvent(
                        type = AuditEventType.FORWARD_FAILED,
                        atMillis = now,
                        requester = destination.phone,
                        sessionId = session.id,
                        detail = "budget-exceeded",
                    ),
                )
                continue
            }
            val sendResult = smsTransport.send(destination.phone, body)
            if (sendResult.outcome == SendOutcome.SENT) {
                successCount += 1
                auditStore.record(
                    AuditEvent(
                        type = AuditEventType.CANDIDATE_FORWARDED,
                        atMillis = now,
                        requester = destination.phone,
                        sessionId = session.id,
                        detail = destination.name,
                    ),
                )
            } else {
                auditStore.record(
                    AuditEvent(
                        type = AuditEventType.FORWARD_FAILED,
                        atMillis = now,
                        requester = destination.phone,
                        sessionId = session.id,
                        detail = destination.name,
                    ),
                )
            }
        }

        val finalSession =
            submitting.copy(
                state = SessionState.ARMED,
                forwarded = successCount > 0 || session.forwarded,
                forwardCount = session.forwardCount + successCount,
                claimedAtMillis = null,
                submittedAtMillis = now,
                terminalOutcome = null,
            )
        sessionRepository.saveSession(finalSession)

        return when {
            successCount > 0 -> CandidateHandleResult.Forwarded(finalSession)
            budgetBlocked -> CandidateHandleResult.BudgetExceeded
            else -> CandidateHandleResult.ForwardFailed
        }
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
        val eventType = when {
            session.origin == SessionOrigin.TIMED && outcome == TerminalOutcome.CANCELLED ->
                AuditEventType.TIMED_CANCELLED
            session.origin == SessionOrigin.TIMED && outcome == TerminalOutcome.EXPIRED ->
                AuditEventType.TIMED_EXPIRED
            outcome == TerminalOutcome.CANCELLED -> AuditEventType.SESSION_CANCELLED
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

    companion object {
        const val MIN_TIMED_DURATION_SECONDS: Int = 60
        const val MAX_TIMED_DURATION_SECONDS: Int = 7_200
        const val DURATION_UNTIL_STOP: Int = -1

        fun isValidDuration(durationSeconds: Int): Boolean =
            durationSeconds == DURATION_UNTIL_STOP ||
                durationSeconds in MIN_TIMED_DURATION_SECONDS..MAX_TIMED_DURATION_SECONDS

        private fun timedArmDetail(durationSeconds: Int, destinationCount: Int): String =
            if (durationSeconds == DURATION_UNTIL_STOP) {
                "until-stop:$destinationCount"
            } else {
                "${durationSeconds}s:$destinationCount"
            }
    }
}
