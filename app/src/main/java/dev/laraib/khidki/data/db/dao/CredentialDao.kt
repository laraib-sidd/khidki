package dev.laraib.khidki.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.laraib.khidki.data.db.entities.CredentialEntity

@Dao
interface CredentialDao {
    @Query(
        """
        SELECT * FROM credentials
        WHERE requesterE164 = :requesterE164
          AND configurationId = :configurationId
          AND expiresAtMillis > :nowMillis
        ORDER BY createdAtMillis DESC
        """,
    )
    suspend fun getValidForRequesterAndConfig(
        requesterE164: String,
        configurationId: String,
        nowMillis: Long,
    ): List<CredentialEntity>

    @Query(
        """
        SELECT * FROM credentials
        WHERE requesterE164 = :requesterE164
          AND expiresAtMillis > :nowMillis
        ORDER BY createdAtMillis DESC
        """,
    )
    suspend fun getValidForRequester(
        requesterE164: String,
        nowMillis: Long,
    ): List<CredentialEntity>

    @Query(
        """
        SELECT COUNT(*) FROM credentials
        WHERE requesterE164 = :requesterE164
          AND expiresAtMillis > :nowMillis
        """,
    )
    suspend fun countLiveForRequester(requesterE164: String, nowMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CredentialEntity)

    @Query("DELETE FROM credentials WHERE expiresAtMillis <= :nowMillis")
    suspend fun deleteExpired(nowMillis: Long)
}
