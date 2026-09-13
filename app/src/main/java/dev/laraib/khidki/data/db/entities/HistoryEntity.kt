package dev.laraib.khidki.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["eventType"]),
    ],
)
data class HistoryEntity(
    @PrimaryKey val id: String,
    val eventType: String,
    val timestampMillis: Long,
    val requesterE164: String?,
    val configurationId: String?,
    val sessionId: String?,
    val detailJson: String?,
)
