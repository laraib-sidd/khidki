package dev.laraib.khidki.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = ConfigurationEntity::class,
            parentColumns = ["id"],
            childColumns = ["configurationId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["state"]),
        Index(value = ["expiresAtMillis"]),
        Index(value = ["bootId"]),
        Index(value = ["configurationId"]),
    ],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val configurationId: String,
    val configurationVersion: Int,
    val configurationSnapshotJson: String,
    val requesterE164: String,
    val state: String,
    val terminalOutcome: String?,
    val armedAtMillis: Long,
    val expiresAtMillis: Long,
    val claimedAtMillis: Long?,
    val submittedAtMillis: Long?,
    val bootId: String,
    val credentialId: String? = null,
)
