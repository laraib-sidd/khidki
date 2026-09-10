package dev.laraib.khidki.data.repository

import androidx.room.withTransaction
import dev.laraib.khidki.data.db.KhidkiDatabase
import dev.laraib.khidki.data.db.entities.SessionEntity
import dev.laraib.khidki.data.mapping.matchesPassword
import dev.laraib.khidki.data.mapping.toDomain
import dev.laraib.khidki.data.mapping.toSnapshotJson
import dev.laraib.khidki.domain.model.ArmRejectReason
import dev.laraib.khidki.domain.model.ArmSessionResult
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.domain.model.ConfigurationVersion
import dev.laraib.khidki.domain.model.SessionState
import dev.laraib.khidki.domain.model.TerminalOutcome
import dev.laraib.khidki.data.ports.PersistenceCredentialVerifier
import dev.laraib.khidki.data.ports.PersistenceLockoutStore
import dev.laraib.khidki.data.ports.PersistenceSessionRepository
import java.util.UUID

class RoomSessionRepository(
    private val database: KhidkiDatabase,
    private val lockoutStore: PersistenceLockoutStore,
    private val credentialVerifier: PersistenceCredentialVerifier,
) : PersistenceSessionRepository {
    private val sessionDao = database.sessionDao()
    private val configurationDao = database.configurationDao()
    private val credentialDao = database.credentialDao()

    override suspend fun getActiveSession(nowMillis: Long): AuthorizationSession? {
        expireTimedOutSessions(nowMillis)
        return sessionDao.getActive(nowMillis)?.toDomain()
    }

    override suspend fun armSession(
        requester: CanonicalPhone,
        password: String,
        configurationId: ConfigurationId,
        nowMillis: Long,
        bootId: String,
    ): ArmSessionResult =
        database.withTransaction {
            if (lockoutStore.isLocked(requester, nowMillis)) {
                return@withTransaction ArmSessionResult.Rejected(ArmRejectReason.LOCKED_OUT)
            }

            val active = sessionDao.getActive(nowMillis)
            if (active != null) {
                return@withTransaction ArmSessionResult.Rejected(ArmRejectReason.ACTIVE_SESSION_EXISTS)
            }

            val configuration = configurationDao.getById(configurationId.toString())
                ?: return@withTransaction ArmSessionResult.Rejected(ArmRejectReason.CONFIGURATION_NOT_FOUND)

            if (!configuration.isEnabled) {
                return@withTransaction ArmSessionResult.Rejected(ArmRejectReason.CONFIGURATION_DISABLED)
            }

            if (configuration.requesterE164 != requester.e164) {
                return@withTransaction ArmSessionResult.Rejected(ArmRejectReason.UNKNOWN_REQUESTER)
            }

            val credentials = credentialDao.getValidForRequesterAndConfig(
                requesterE164 = requester.e164,
                configurationId = configurationId.toString(),
                nowMillis = nowMillis,
            )
            val matchedCredential = credentials.firstOrNull { credential ->
                credential.matchesPassword(password) { candidate, tag ->
                    credentialVerifier.verify(candidate, tag)
                }
            } ?: return@withTransaction ArmSessionResult.Rejected(ArmRejectReason.INVALID_PASSWORD)

            lockoutStore.clearFailures(requester)

            val sessionId = UUID.randomUUID()
            val expiresAtMillis = nowMillis + configuration.windowSeconds * 1000L
            val snapshotJson = configuration.toDomain().toSnapshotJson()
            val sessionEntity = SessionEntity(
                id = sessionId.toString(),
                configurationId = configuration.id,
                configurationVersion = configuration.version,
                configurationSnapshotJson = snapshotJson,
                requesterE164 = requester.e164,
                state = SessionState.ARMED.name,
                terminalOutcome = null,
                armedAtMillis = nowMillis,
                expiresAtMillis = expiresAtMillis,
                claimedAtMillis = null,
                submittedAtMillis = null,
                bootId = bootId,
                credentialId = matchedCredential.id,
            )
            sessionDao.insert(sessionEntity)
            ArmSessionResult.Success(sessionEntity.toDomain())
        }

    override suspend fun updateState(
        sessionId: UUID,
        expectedState: SessionState,
        newState: SessionState,
        nowMillis: Long,
    ): Boolean {
        val session = sessionDao.getById(sessionId.toString()) ?: return false
        if (session.state != expectedState.name) {
            return false
        }

        val updated = when (newState) {
            SessionState.CLAIMED -> session.copy(
                state = newState.name,
                claimedAtMillis = nowMillis,
            )
            else -> session.copy(state = newState.name)
        }
        sessionDao.update(updated)
        return true
    }

    override suspend fun markTerminal(
        sessionId: UUID,
        outcome: TerminalOutcome,
        nowMillis: Long,
    ): Boolean =
        sessionDao.markTerminal(
            id = sessionId.toString(),
            outcome = outcome.name,
            nowMillis = nowMillis,
        ) > 0

    override suspend fun expireSessionsForBootChange(newBootId: String, nowMillis: Long) {
        sessionDao.expireForBootChange(newBootId, nowMillis)
    }

    override suspend fun expireTimedOutSessions(nowMillis: Long) {
        sessionDao.expireTimedOut(nowMillis)
    }
}
