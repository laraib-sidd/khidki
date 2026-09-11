package dev.laraib.khidki.platform.diagnostics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticEventBusTest {
    @After
    fun tearDown() {
        DiagnosticEventBus.clear()
    }

    @Test
    fun record_appendsFormattedEvents() {
        DiagnosticEventBus.record("RECV SMS from +91•••12")
        val events = DiagnosticEventBus.events.value
        assertEquals(1, events.size)
        assertTrue(events.first().contains("RECV SMS from +91•••12"))
    }

    @Test
    fun record_evictsOldestWhenCapacityExceeded() {
        repeat(55) { index ->
            DiagnosticEventBus.record("event-$index")
        }
        val events = DiagnosticEventBus.events.value
        assertEquals(50, events.size)
        assertTrue(events.first().contains("event-5"))
        assertTrue(events.last().contains("event-54"))
    }
}
