package dev.laraib.khidki.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credentials",
    foreignKeys = [
        ForeignKey(
            entity = ConfigurationEntity::class,
            parentColumns = ["id"],
            childColumns = ["configurationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["configurationId"]),
        Index(value = ["requesterE164"]),
        Index(value = ["expiresAtMillis"]),
    ],
)
data class CredentialEntity(
    @PrimaryKey val id: String,
    val configurationId: String,
    val requesterE164: String,
    val verificationTag: ByteArray,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
)
