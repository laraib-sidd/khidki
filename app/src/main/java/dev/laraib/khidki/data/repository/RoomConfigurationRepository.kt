package dev.laraib.khidki.data.repository

import androidx.room.withTransaction
import dev.laraib.khidki.data.db.KhidkiDatabase
import dev.laraib.khidki.data.mapping.toDomain
import dev.laraib.khidki.data.mapping.toEntity
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.data.ports.PersistenceConfigurationRepository

class RoomConfigurationRepository(
    private val database: KhidkiDatabase,
) : PersistenceConfigurationRepository {
    private val configurationDao = database.configurationDao()

    override suspend fun count(): Int = configurationDao.count()

    override suspend fun getAll(): List<Configuration> =
        configurationDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: ConfigurationId): Configuration? =
        configurationDao.getById(id.toString())?.toDomain()

    override suspend fun getEnabledForRequester(requester: CanonicalPhone): List<Configuration> =
        configurationDao.getEnabledForRequester(requester.e164).map { it.toDomain() }

    override suspend fun upsert(configuration: Configuration) {
        configurationDao.upsert(configuration.toEntity())
    }

    override suspend fun delete(id: ConfigurationId) {
        database.withTransaction {
            configurationDao.deleteById(id.toString())
        }
    }
}
