package dev.laraib.khidki.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.laraib.khidki.data.db.entities.ConfigurationEntity
import dev.laraib.khidki.data.db.entities.CredentialEntity
import dev.laraib.khidki.data.db.entities.SessionEntity
import dev.laraib.khidki.domain.model.SessionState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class KhidkiDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: KhidkiDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, KhidkiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createDatabase_andPersistCoreRows() = runBlocking {
        val configurationId = UUID.randomUUID().toString()
        val credentialId = UUID.randomUUID().toString()
        val sessionId = UUID.randomUUID().toString()
        val nowMillis = 1_700_000_000_000L

        database.configurationDao().upsert(
            ConfigurationEntity(
                id = configurationId,
                version = 1,
                label = "Brother",
                requesterE164 = "+919876543210",
                senderPatternsJson = "[\"^BANK\"]",
                contentPatternsJson = "[\"\\\\d{6}\"]",
                exclusionSenderPatternsJson = "[]",
                exclusionContentPatternsJson = "[]",
                windowSeconds = 120,
                credentialLifetimeMs = 86_400_000L,
                isEnabled = true,
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
            ),
        )

        database.credentialDao().insert(
            CredentialEntity(
                id = credentialId,
                configurationId = configurationId,
                requesterE164 = "+919876543210",
                verificationTag = byteArrayOf(1, 2, 3, 4),
                createdAtMillis = nowMillis,
                expiresAtMillis = nowMillis + 86_400_000L,
            ),
        )

        database.sessionDao().insert(
            SessionEntity(
                id = sessionId,
                configurationId = configurationId,
                configurationVersion = 1,
                configurationSnapshotJson = """{"id":"$configurationId","version":1}""",
                requesterE164 = "+919876543210",
                state = SessionState.ARMED.name,
                terminalOutcome = null,
                armedAtMillis = nowMillis,
                expiresAtMillis = nowMillis + 120_000L,
                claimedAtMillis = null,
                submittedAtMillis = null,
                bootId = "boot-1",
                credentialId = credentialId,
            ),
        )

        val active = database.sessionDao().getActive(nowMillis + 1_000L)
        assertNotNull(active)
        assertEquals(sessionId, active?.id)
        assertEquals(1, database.configurationDao().count())
    }
}
