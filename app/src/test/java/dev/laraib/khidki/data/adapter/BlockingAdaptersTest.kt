package dev.laraib.khidki.data.adapter

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.laraib.khidki.data.db.KhidkiDatabase
import dev.laraib.khidki.data.repository.RoomBudgetLedger
import dev.laraib.khidki.data.repository.RoomLockoutStore
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.platform.clock.AndroidSystemClock
import dev.laraib.khidki.data.prefs.AppPreferences
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class BlockingAdaptersTest {
    private lateinit var context: Context
    private lateinit var database: KhidkiDatabase
    private lateinit var clock: AndroidSystemClock

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, KhidkiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = AndroidSystemClock(AppPreferences(context))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun persistentLockoutTracker_survivesNewInstance() {
        val requester = CanonicalPhone("+919876543210")
        val store = RoomLockoutStore(database.lockoutDao())
        val tracker1 = PersistentLockoutTracker(store, clock)
        repeat(5) { tracker1.recordFailure(requester) }
        assertTrue(tracker1.isLocked(requester))

        val tracker2 = PersistentLockoutTracker(store, clock)
        assertTrue(tracker2.isLocked(requester))
    }

    @Test
    fun blockingDomainBudgetLedger_persistsReservations() {
        val roomLedger = RoomBudgetLedger(database)
        val ledger1 = BlockingDomainBudgetLedger(roomLedger, clock)
        assertTrue(ledger1.reserve(3))
        assertEqualsParts(3, ledger1.partsUsedInWindow())

        val ledger2 = BlockingDomainBudgetLedger(roomLedger, clock)
        assertEqualsParts(3, ledger2.partsUsedInWindow())
        assertFalse(ledger2.canReserve(18))
    }

    private fun assertEqualsParts(expected: Int, actual: Int) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
