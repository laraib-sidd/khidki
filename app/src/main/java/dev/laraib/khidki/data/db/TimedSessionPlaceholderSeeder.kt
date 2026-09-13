package dev.laraib.khidki.data.db

import dev.laraib.khidki.data.db.entities.ConfigurationEntity
import dev.laraib.khidki.domain.session.TimedSessionDefaults
import kotlinx.coroutines.runBlocking

object TimedSessionPlaceholderSeeder {
    fun ensureSeeded(database: KhidkiDatabase) {
        runBlocking {
            val id = TimedSessionDefaults.PLACEHOLDER_CONFIG_ID.toString()
            val dao = database.configurationDao()
            if (dao.getById(id) == null) {
                dao.upsert(
                    ConfigurationEntity(
                        id = id,
                        version = 1,
                        label = TimedSessionDefaults.LABEL,
                        requesterE164 = "+10000000000",
                        senderPatternsJson = "[]",
                        contentPatternsJson = "[]",
                        exclusionSenderPatternsJson = "[]",
                        exclusionContentPatternsJson = "[]",
                        windowSeconds = 120,
                        credentialLifetimeMs = 86_400_000L,
                        isEnabled = false,
                        createdAtMillis = 0L,
                        updatedAtMillis = 0L,
                    ),
                )
            }
        }
    }
}
