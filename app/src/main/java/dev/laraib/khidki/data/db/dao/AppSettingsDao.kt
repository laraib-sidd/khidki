package dev.laraib.khidki.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.laraib.khidki.data.db.entities.AppSettingsEntity

@Dao
interface AppSettingsDao {
    @Query("SELECT value FROM app_settings WHERE key = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingsEntity)

    @Query("DELETE FROM app_settings WHERE key = :key")
    suspend fun delete(key: String)
}
