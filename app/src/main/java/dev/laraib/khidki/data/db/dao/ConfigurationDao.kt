package dev.laraib.khidki.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.laraib.khidki.data.db.entities.ConfigurationEntity

@Dao
interface ConfigurationDao {
    @Query("SELECT COUNT(*) FROM configurations")
    suspend fun count(): Int

    @Query("SELECT * FROM configurations ORDER BY label COLLATE NOCASE ASC")
    suspend fun getAll(): List<ConfigurationEntity>

    @Query("SELECT * FROM configurations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConfigurationEntity?

    @Query(
        """
        SELECT * FROM configurations
        WHERE requesterE164 = :requesterE164 AND isEnabled = 1
        ORDER BY updatedAtMillis DESC
        """,
    )
    suspend fun getEnabledForRequester(requesterE164: String): List<ConfigurationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConfigurationEntity)

    @Query("DELETE FROM configurations WHERE id = :id")
    suspend fun deleteById(id: String)
}
