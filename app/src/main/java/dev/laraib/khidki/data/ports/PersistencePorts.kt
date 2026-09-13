package dev.laraib.khidki.data.ports

import dev.laraib.khidki.domain.model.ArmRejectReason
import dev.laraib.khidki.domain.model.ArmSessionResult
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.BudgetReservation
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.domain.model.HistoryEvent
import dev.laraib.khidki.domain.model.IncomingSms
import dev.laraib.khidki.domain.model.SessionState
import dev.laraib.khidki.domain.model.TerminalOutcome
import java.util.UUID

interface PersistenceCredentialVerifier {
    fun computeTag(password: String): ByteArray

    fun verify(password: String, storedTag: ByteArray): Boolean
}

interface PersistenceConfigurationRepository {
    suspend fun count(): Int

    suspend fun getAll(): List<Configuration>

    suspend fun getById(id: ConfigurationId): Configuration?

    suspend fun getEnabledForRequester(requester: CanonicalPhone): List<Configuration>

    suspend fun upsert(configuration: Configuration)

    suspend fun delete(id: ConfigurationId)
}

interface PersistenceSessionRepository {
    suspend fun getActiveSession(nowMillis: Long): AuthorizationSession?

    suspend fun armSession(
        requester: CanonicalPhone,
        password: String,
        configurationId: ConfigurationId,
        nowMillis: Long,
        bootId: String,
    ): ArmSessionResult

    suspend fun updateState(
        sessionId: UUID,
        expectedState: SessionState,
        newState: SessionState,
        nowMillis: Long,
    ): Boolean

    suspend fun markTerminal(
        sessionId: UUID,
        outcome: TerminalOutcome,
        nowMillis: Long,
    ): Boolean

    suspend fun expireSessionsForBootChange(newBootId: String, nowMillis: Long)

    suspend fun expireTimedOutSessions(nowMillis: Long)
}

interface PersistenceLockoutStore {
    suspend fun isLocked(requester: CanonicalPhone, nowMillis: Long): Boolean

    suspend fun recordFailure(requester: CanonicalPhone, nowMillis: Long): Boolean

    suspend fun clearFailures(requester: CanonicalPhone)
}

interface PersistenceBudgetLedger {
    suspend fun reserveParts(
        parts: Int,
        sessionId: UUID?,
        nowMillis: Long,
    ): BudgetReservation?

    suspend fun markConsumed(reservationId: UUID)

    suspend fun markUncertain(reservationId: UUID)

    suspend fun countReservedPartsInWindow(nowMillis: Long): Int
}

interface PersistenceAuditStore {
    suspend fun append(event: HistoryEvent)

    suspend fun listRecent(limit: Int): List<HistoryEvent>
}

interface PersistenceRequestAuthenticator {
    suspend fun authenticate(
        requester: CanonicalPhone,
        password: String,
        configurationId: ConfigurationId,
        nowMillis: Long,
    ): ArmRejectReason?
}

interface PersistenceRuleMatcher {
    fun matchesCandidate(
        configuration: Configuration,
        candidate: IncomingSms.Candidate,
        trustedRequesters: Set<CanonicalPhone>,
    ): Boolean
}

interface PersistenceSmsTransport {
    suspend fun sendAck(
        destination: CanonicalPhone,
        label: String,
        windowSeconds: Int,
        allowance: Int,
    ): Boolean

    suspend fun forward(
        destination: CanonicalPhone,
        body: String,
        sessionId: UUID,
    ): PersistenceForwardResult
}

data class PersistenceForwardResult(
    val accepted: Boolean,
    val reservationId: UUID?,
    val uncertain: Boolean = false,
)
