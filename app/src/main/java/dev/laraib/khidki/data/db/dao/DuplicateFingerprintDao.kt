package dev.laraib.khidki.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.laraib.khidki.data.db.entities.DuplicateFingerprintEntity

@Dao
interface DuplicateFingerprintDao {
    @Query("SELECT COUNT(*) > 0 FROM duplicate_fingerprints WHERE digestHex = :digestHex")
    suspend fun exists(digestHex: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: DuplicateFingerprintEntity): Long

    @Query("DELETE FROM duplicate_fingerprints WHERE recordedAtMillis < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
