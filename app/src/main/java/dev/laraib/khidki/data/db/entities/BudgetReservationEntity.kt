package dev.laraib.khidki.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_reservations",
    indices = [
        Index(value = ["createdAtMillis"]),
        Index(value = ["status"]),
        Index(value = ["sessionId"]),
    ],
)
data class BudgetReservationEntity(
    @PrimaryKey val id: String,
    val sessionId: String?,
    val reservedParts: Int,
    val createdAtMillis: Long,
    val status: String,
)
