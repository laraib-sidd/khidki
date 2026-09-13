package dev.laraib.khidki.data.repository

import androidx.room.withTransaction
import dev.laraib.khidki.data.db.KhidkiDatabase
import dev.laraib.khidki.data.db.entities.BudgetReservationEntity
import dev.laraib.khidki.data.mapping.toDomain
import dev.laraib.khidki.domain.model.BudgetReservation
import dev.laraib.khidki.domain.model.BudgetReservationStatus
import dev.laraib.khidki.data.ports.PersistenceBudgetLedger
import java.util.UUID

class RoomBudgetLedger(
    private val database: KhidkiDatabase,
    private val rollingWindowMs: Long = ROLLING_WINDOW_MS,
    private val maxPartsPerWindow: Int = MAX_PARTS_PER_WINDOW,
    private val maxPartsPerMessage: Int = MAX_PARTS_PER_MESSAGE,
) : PersistenceBudgetLedger {
    private val budgetDao = database.budgetReservationDao()

    override suspend fun reserveParts(
        parts: Int,
        sessionId: UUID?,
        nowMillis: Long,
    ): BudgetReservation? {
        if (parts <= 0 || parts > maxPartsPerMessage) {
            return null
        }

        return database.withTransaction {
            val windowStart = nowMillis - rollingWindowMs
            val usedParts = budgetDao.sumReservedPartsSince(windowStart)
            if (usedParts + parts > maxPartsPerWindow) {
                return@withTransaction null
            }

            val reservation = BudgetReservationEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId?.toString(),
                reservedParts = parts,
                createdAtMillis = nowMillis,
                status = BudgetReservationStatus.PENDING.name,
            )
            budgetDao.insert(reservation)
            reservation.toDomain()
        }
    }

    override suspend fun markConsumed(reservationId: UUID) {
        budgetDao.updateStatus(reservationId.toString(), BudgetReservationStatus.CONSUMED.name)
    }

    override suspend fun markUncertain(reservationId: UUID) {
        budgetDao.updateStatus(reservationId.toString(), BudgetReservationStatus.UNCERTAIN.name)
    }

    override suspend fun countReservedPartsInWindow(nowMillis: Long): Int {
        val windowStart = nowMillis - rollingWindowMs
        return budgetDao.sumReservedPartsSince(windowStart)
    }

    private companion object {
        const val ROLLING_WINDOW_MS = 24L * 60L * 60L * 1000L
        const val MAX_PARTS_PER_WINDOW = 20
        const val MAX_PARTS_PER_MESSAGE = 3
    }
}
