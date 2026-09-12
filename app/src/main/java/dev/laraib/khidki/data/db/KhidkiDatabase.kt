package dev.laraib.khidki.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.laraib.khidki.data.db.dao.AppSettingsDao
import dev.laraib.khidki.data.db.dao.BudgetReservationDao
import dev.laraib.khidki.data.db.dao.ConfigurationDao
import dev.laraib.khidki.data.db.dao.CredentialDao
import dev.laraib.khidki.data.db.dao.DuplicateFingerprintDao
import dev.laraib.khidki.data.db.dao.HistoryDao
import dev.laraib.khidki.data.db.dao.LockoutDao
import dev.laraib.khidki.data.db.dao.SessionDao
import dev.laraib.khidki.data.db.entities.AppSettingsEntity
import dev.laraib.khidki.data.db.entities.BudgetReservationEntity
import dev.laraib.khidki.data.db.entities.ConfigurationEntity
import dev.laraib.khidki.data.db.entities.CredentialEntity
import dev.laraib.khidki.data.db.entities.DuplicateFingerprintEntity
import dev.laraib.khidki.data.db.entities.HistoryEntity
import dev.laraib.khidki.data.db.entities.LockoutEntity
import dev.laraib.khidki.data.db.entities.SessionEntity

@Database(
    entities = [
        ConfigurationEntity::class,
        CredentialEntity::class,
        SessionEntity::class,
        LockoutEntity::class,
        BudgetReservationEntity::class,
        DuplicateFingerprintEntity::class,
        HistoryEntity::class,
        AppSettingsEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class KhidkiDatabase : RoomDatabase() {
    abstract fun configurationDao(): ConfigurationDao

    abstract fun credentialDao(): CredentialDao

    abstract fun sessionDao(): SessionDao

    abstract fun lockoutDao(): LockoutDao

    abstract fun budgetReservationDao(): BudgetReservationDao

    abstract fun duplicateFingerprintDao(): DuplicateFingerprintDao

    abstract fun historyDao(): HistoryDao

    abstract fun appSettingsDao(): AppSettingsDao
}
