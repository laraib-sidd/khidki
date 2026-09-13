package dev.laraib.khidki.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "configurations",
    indices = [
        Index(value = ["requesterE164"]),
        Index(value = ["isEnabled"]),
    ],
)
data class ConfigurationEntity(
    @PrimaryKey val id: String,
    val version: Int,
    val label: String,
    val requesterE164: String,
    val senderPatternsJson: String,
    val contentPatternsJson: String,
    val exclusionSenderPatternsJson: String,
    val exclusionContentPatternsJson: String,
    val windowSeconds: Int,
    val credentialLifetimeMs: Long,
    val isEnabled: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
