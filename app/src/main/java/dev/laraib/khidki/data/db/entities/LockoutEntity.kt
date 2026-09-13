package dev.laraib.khidki.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lockouts")
data class LockoutEntity(
    @PrimaryKey val requesterE164: String,
    val failureCount: Int,
    val windowStartMillis: Long,
    val lockedUntilMillis: Long,
)
