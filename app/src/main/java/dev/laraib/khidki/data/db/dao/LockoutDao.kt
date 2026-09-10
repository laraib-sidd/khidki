package dev.laraib.khidki.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.laraib.khidki.data.db.entities.LockoutEntity

@Dao
interface LockoutDao {
    @Query("SELECT * FROM lockouts WHERE requesterE164 = :requesterE164 LIMIT 1")
    suspend fun getByRequester(requesterE164: String): LockoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LockoutEntity)

    @Query("DELETE FROM lockouts WHERE requesterE164 = :requesterE164")
    suspend fun deleteByRequester(requesterE164: String)
}
