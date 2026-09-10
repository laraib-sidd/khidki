package dev.laraib.khidki.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.laraib.khidki.data.db.entities.BudgetReservationEntity

@Dao
interface BudgetReservationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: BudgetReservationEntity)

    @Query(
        """
        SELECT COALESCE(SUM(reservedParts), 0) FROM budget_reservations
        WHERE createdAtMillis >= :windowStartMillis
          AND status IN ('PENDING', 'CONSUMED', 'UNCERTAIN')
        """,
    )
    suspend fun sumReservedPartsSince(windowStartMillis: Long): Int

    @Query("UPDATE budget_reservations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String): Int

    @Query("SELECT * FROM budget_reservations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetReservationEntity?
}
