package dev.laraib.khidki.data.repository

import dev.laraib.khidki.data.db.dao.HistoryDao
import dev.laraib.khidki.data.mapping.toDomain
import dev.laraib.khidki.data.mapping.toEntity
import dev.laraib.khidki.domain.model.HistoryEvent
import dev.laraib.khidki.data.ports.PersistenceAuditStore

class RoomAuditStore(
    private val historyDao: HistoryDao,
) : PersistenceAuditStore {
    override suspend fun append(event: HistoryEvent) {
        requireNoSensitiveBodies(event)
        historyDao.insert(event.toEntity())
    }

    override suspend fun listRecent(limit: Int): List<HistoryEvent> =
        historyDao.listRecent(limit).map { it.toDomain() }

    private fun requireNoSensitiveBodies(event: HistoryEvent) {
        val forbiddenKeys = setOf("body", "password", "otp", "raw", "message")
        require(event.detail.keys.none { key -> forbiddenKeys.contains(key.lowercase()) }) {
            "Audit history must not store message bodies or secrets"
        }
    }
}
