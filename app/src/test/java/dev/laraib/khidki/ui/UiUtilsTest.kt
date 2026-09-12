package dev.laraib.khidki.ui

import dev.laraib.khidki.ui.components.DiagnosticChipType
import dev.laraib.khidki.ui.components.UiUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiUtilsTest {
    @Test
    fun maskPhoneNumber_masksMiddleDigits() {
        assertEquals("+919 •••• 10", UiUtils.maskPhoneNumber("+919876543210"))
    }

    @Test
    fun humanizeDiagnosticEvent_mapsInboundToFriendlyText() {
        assertEquals("Message received", UiUtils.humanizeDiagnosticEvent("IN from +919876543210"))
    }

    @Test
    fun humanizeDiagnosticEvent_mapsForwardToFriendlyText() {
        val result = UiUtils.humanizeDiagnosticEvent("FWD candidate to requester")
        assertEquals("Matching message forwarded", result)
    }

    @Test
    fun parseDiagnosticEvent_detectsDropType() {
        val chip = UiUtils.parseDiagnosticEvent("DROP no active session")
        assertEquals(DiagnosticChipType.DROP, chip.type)
    }

    @Test
    fun maskPhoneNumber_shortInput_returnsPlaceholder() {
        assertTrue(UiUtils.maskPhoneNumber("123").contains("•"))
    }
}
