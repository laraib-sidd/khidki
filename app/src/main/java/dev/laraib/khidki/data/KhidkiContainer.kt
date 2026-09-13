package dev.laraib.khidki.data

import android.content.Context
import androidx.room.Room
import dev.laraib.khidki.data.db.KhidkiDatabase
import dev.laraib.khidki.data.db.KhidkiMigrations
import dev.laraib.khidki.data.db.TimedSessionPlaceholderSeeder
import dev.laraib.khidki.data.keystore.KeystoreCredentialVerifier
import dev.laraib.khidki.data.prefs.AppPreferences
import dev.laraib.khidki.data.ports.PersistenceAuditStore
import dev.laraib.khidki.data.ports.PersistenceBudgetLedger
import dev.laraib.khidki.data.ports.PersistenceConfigurationRepository
import dev.laraib.khidki.data.ports.PersistenceCredentialVerifier
import dev.laraib.khidki.data.ports.PersistenceLockoutStore
import dev.laraib.khidki.data.ports.PersistenceSessionRepository
import dev.laraib.khidki.data.repository.RoomAuditStore
import dev.laraib.khidki.data.repository.RoomBudgetLedger
import dev.laraib.khidki.data.repository.RoomConfigurationRepository
import dev.laraib.khidki.data.repository.RoomCredentialStore
import dev.laraib.khidki.data.repository.RoomDuplicateFingerprintStore
import dev.laraib.khidki.data.repository.RoomLockoutStore
import dev.laraib.khidki.data.repository.RoomSessionRepository

class KhidkiContainer private constructor(
    val database: KhidkiDatabase,
    val appPreferences: AppPreferences,
    val credentialVerifier: PersistenceCredentialVerifier,
    val configurationRepository: PersistenceConfigurationRepository,
    val sessionRepository: PersistenceSessionRepository,
    val lockoutStore: PersistenceLockoutStore,
    val budgetLedger: PersistenceBudgetLedger,
    val auditStore: PersistenceAuditStore,
    val credentialStore: RoomCredentialStore,
    val duplicateFingerprintStore: RoomDuplicateFingerprintStore,
) {
    companion object {
        @Volatile
        private var instance: KhidkiContainer? = null

        fun get(context: Context): KhidkiContainer =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        fun build(context: Context): KhidkiContainer {
            val database = Room.databaseBuilder(
                context,
                KhidkiDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(
                    KhidkiMigrations.MIGRATION_1_2,
                    KhidkiMigrations.MIGRATION_2_3,
                    KhidkiMigrations.MIGRATION_3_4,
                )
                .build()
            TimedSessionPlaceholderSeeder.ensureSeeded(database)

            val credentialVerifier = KeystoreCredentialVerifier()
            val lockoutStore = RoomLockoutStore(database.lockoutDao())

            return KhidkiContainer(
                database = database,
                appPreferences = AppPreferences(context),
                credentialVerifier = credentialVerifier,
                configurationRepository = RoomConfigurationRepository(database),
                sessionRepository = RoomSessionRepository(
                    database = database,
                    lockoutStore = lockoutStore,
                    credentialVerifier = credentialVerifier,
                ),
                lockoutStore = lockoutStore,
                budgetLedger = RoomBudgetLedger(database),
                auditStore = RoomAuditStore(database.historyDao()),
                credentialStore = RoomCredentialStore(
                    credentialDao = database.credentialDao(),
                    credentialVerifier = credentialVerifier,
                ),
                duplicateFingerprintStore = RoomDuplicateFingerprintStore(
                    duplicateFingerprintDao = database.duplicateFingerprintDao(),
                ),
            )
        }

        private const val DATABASE_NAME = "khidki.db"
    }
}
