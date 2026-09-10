package dev.laraib.khidki.data.adapter

import dev.laraib.khidki.data.db.KhidkiDatabase
import dev.laraib.khidki.data.mapping.toDomain
import dev.laraib.khidki.data.mapping.toEntity
import dev.laraib.khidki.data.ports.PersistenceAuditStore
import dev.laraib.khidki.data.ports.PersistenceConfigurationRepository
import dev.laraib.khidki.data.ports.PersistenceCredentialVerifier
import dev.laraib.khidki.data.ports.PersistenceSessionRepository
import dev.laraib.khidki.data.repository.RoomAuditStore
import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuditEventType
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.HistoryEvent
import dev.laraib.khidki.domain.model.HistoryEventType
import dev.laraib.khidki.domain.model.TerminalOutcome
import dev.laraib.khidki.domain.ports.AuditStore
import dev.laraib.khidki.domain.ports.ConfigurationRepository
import dev.laraib.khidki.domain.ports.CredentialVerifier
import dev.laraib.khidki.domain.ports.SessionRepository
import java.util.UUID

class BlockingConfigurationRepository(
    private val delegate: PersistenceConfigurationRepository,
) : ConfigurationRepository {
    override fun findByRequester(requester: CanonicalPhone): Configuration? =
        Blocking.io { delegate.getEnabledForRequester(requester).firstOrNull() }

    override fun findAll(): List<Configuration> = Blocking.io { delegate.getAll() }
}

class BlockingSessionRepository(
    private val delegate: PersistenceSessionRepository,
    private val database: KhidkiDatabase,
    private val nowMillis: () -> Long,
) : SessionRepository {
    override fun getActiveSession(): AuthorizationSession? =
        Blocking.io { delegate.getActiveSession(nowMillis()) }

    override fun saveSession(session: AuthorizationSession?) {
        Blocking.io {
            if (session == null) {
                return@io
            }
            val dao = database.sessionDao()
            val existing = dao.getById(session.id.toString())
            val entity = session.toEntity()
            if (existing == null) {
                dao.insert(entity)
            } else {
                dao.update(entity)
            }
        }
    }

    override fun terminateSession(sessionId: UUID, outcome: TerminalOutcome): AuthorizationSession? =
        Blocking.io {
            delegate.markTerminal(sessionId, outcome, nowMillis())
            database.sessionDao().getById(sessionId.toString())?.toDomain()
        }
}

class BlockingAuditStore(
    private val delegate: PersistenceAuditStore,
) : AuditStore {
    override fun record(event: AuditEvent) {
        Blocking.io {
            delegate.append(event.toHistoryEvent())
        }
    }
}

class RoomDomainCredentialVerifier(
    private val database: KhidkiDatabase,
    private val keystore: PersistenceCredentialVerifier,
    private val nowMillis: () -> Long,
) : CredentialVerifier {
    override fun verify(requester: CanonicalPhone, password: String): Boolean =
        Blocking.io {
            val credentials = database.credentialDao().getValidForRequester(
                requesterE164 = requester.e164,
                nowMillis = nowMillis(),
            )
            credentials.any { entity ->
                keystore.verify(password, entity.verificationTag)
            }
        }

    override fun isKnownRequester(requester: CanonicalPhone): Boolean =
        Blocking.io {
            database.credentialDao().countLiveForRequester(
                requesterE164 = requester.e164,
                nowMillis = nowMillis(),
            ) > 0 ||
                database.configurationDao().getEnabledForRequester(requester.e164).isNotEmpty()
        }
}

private fun AuditEvent.toHistoryEvent(): HistoryEvent {
    val type = when (this.type) {
        AuditEventType.COMMAND_RECEIVED -> HistoryEventType.SESSION_ARMED
        AuditEventType.COMMAND_REJECTED -> HistoryEventType.AUTH_FAILURE
        AuditEventType.SESSION_ARMED -> HistoryEventType.SESSION_ARMED
        AuditEventType.SESSION_EXPIRED -> HistoryEventType.SESSION_TERMINAL
        AuditEventType.SESSION_CANCELLED -> HistoryEventType.SESSION_TERMINAL
        AuditEventType.CANDIDATE_MATCHED -> HistoryEventType.SESSION_CLAIMED
        AuditEventType.CANDIDATE_FORWARDED -> HistoryEventType.SESSION_SUBMITTED
        AuditEventType.CANDIDATE_REJECTED -> HistoryEventType.DUPLICATE_REJECTED
        AuditEventType.ACK_SENT -> HistoryEventType.SESSION_ARMED
        AuditEventType.ACK_FAILED -> HistoryEventType.BUDGET_REJECTED
        AuditEventType.FORWARD_SENT -> HistoryEventType.SESSION_SUBMITTED
        AuditEventType.FORWARD_FAILED -> HistoryEventType.SESSION_TERMINAL
    }
    val detail = buildMap {
        detail?.let { put("detail", it) }
        sessionId?.let { put("sessionId", it.toString()) }
    }
    return HistoryEvent(
        id = UUID.randomUUID(),
        eventType = type,
        timestampMillis = atMillis,
        requester = requester,
        configurationId = null,
        sessionId = sessionId,
        detail = detail,
    )
}
