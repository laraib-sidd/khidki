package dev.laraib.khidki.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.laraib.khidki.data.db.entities.SessionEntity

@Dao
interface SessionDao {
    @Query(
        """
        SELECT * FROM sessions
        WHERE state IN ('ARMED', 'CLAIMED', 'SUBMITTING')
          AND expiresAtMillis > :nowMillis
        ORDER BY armedAtMillis DESC
        LIMIT 1
        """,
    )
    suspend fun getActive(nowMillis: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: SessionEntity)

    @Update
    suspend fun update(entity: SessionEntity)

    @Query(
        """
        UPDATE sessions
        SET state = :newState
        WHERE id = :id AND state = :expectedState
        """,
    )
    suspend fun updateStateIfExpected(
        id: String,
        expectedState: String,
        newState: String,
    ): Int

    @Query(
        """
        UPDATE sessions
        SET state = 'SUBMITTED',
            terminalOutcome = :outcome,
            submittedAtMillis = :nowMillis
        WHERE id = :id
          AND state IN ('ARMED', 'CLAIMED', 'SUBMITTING')
        """,
    )
    suspend fun markTerminal(
        id: String,
        outcome: String,
        nowMillis: Long,
    ): Int

    @Query(
        """
        UPDATE sessions
        SET state = 'SUBMITTED',
            terminalOutcome = 'EXPIRED',
            submittedAtMillis = :nowMillis
        WHERE state IN ('ARMED', 'CLAIMED', 'SUBMITTING')
          AND bootId != :newBootId
        """,
    )
    suspend fun expireForBootChange(newBootId: String, nowMillis: Long): Int

    @Query(
        """
        UPDATE sessions
        SET state = 'SUBMITTED',
            terminalOutcome = 'EXPIRED',
            submittedAtMillis = :nowMillis
        WHERE state IN ('ARMED', 'CLAIMED', 'SUBMITTING')
          AND expiresAtMillis <= :nowMillis
        """,
    )
    suspend fun expireTimedOut(nowMillis: Long): Int
}
