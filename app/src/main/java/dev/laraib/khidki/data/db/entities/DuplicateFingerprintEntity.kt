package dev.laraib.khidki.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "duplicate_fingerprints",
    indices = [
        Index(value = ["recordedAtMillis"]),
        Index(value = ["sessionId"]),
    ],
)
data class DuplicateFingerprintEntity(
    @PrimaryKey val digestHex: String,
    val sessionId: String,
    val recordedAtMillis: Long,
)
