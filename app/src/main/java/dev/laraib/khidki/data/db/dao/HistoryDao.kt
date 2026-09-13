package dev.laraib.khidki.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.laraib.khidki.data.db.entities.HistoryEntity

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: HistoryEntity)

    @Query(
        """
        SELECT * FROM history
        ORDER BY timestampMillis DESC
        LIMIT :limit
        """,
    )
    suspend fun listRecent(limit: Int): List<HistoryEntity>

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}
