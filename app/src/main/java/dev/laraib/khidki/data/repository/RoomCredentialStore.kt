package dev.laraib.khidki.data.repository

import dev.laraib.khidki.data.db.dao.CredentialDao
import dev.laraib.khidki.data.db.entities.CredentialEntity
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.data.ports.PersistenceCredentialVerifier
import java.util.UUID

class RoomCredentialStore(
    private val credentialDao: CredentialDao,
    private val credentialVerifier: PersistenceCredentialVerifier,
    private val maxLiveCredentialsPerRequester: Int = MAX_LIVE_CREDENTIALS_PER_REQUESTER,
) {
    suspend fun createCredential(
        requester: CanonicalPhone,
        configurationId: ConfigurationId,
        password: String,
        createdAtMillis: Long,
        expiresAtMillis: Long,
    ): UUID {
        val liveCount = credentialDao.countLiveForRequester(requester.e164, createdAtMillis)
        require(liveCount < maxLiveCredentialsPerRequester) {
            "Maximum live credentials reached for requester"
        }

        val credentialId = UUID.randomUUID()
        credentialDao.insert(
            CredentialEntity(
                id = credentialId.toString(),
                configurationId = configurationId.toString(),
                requesterE164 = requester.e164,
                verificationTag = credentialVerifier.computeTag(password),
                createdAtMillis = createdAtMillis,
                expiresAtMillis = expiresAtMillis,
            ),
        )
        return credentialId
    }

    suspend fun purgeExpired(nowMillis: Long) {
        credentialDao.deleteExpired(nowMillis)
    }

    suspend fun revokeForConfiguration(configurationId: ConfigurationId) {
        credentialDao.deleteForConfiguration(configurationId.toString())
    }

    private companion object {
        const val MAX_LIVE_CREDENTIALS_PER_REQUESTER = 5
    }
}
