package dev.laraib.khidki.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfigurationValidatorTest {
    @Test
    fun validatePatterns_rejectsInvalidRegex() {
        val error = ConfigurationValidator.validatePatterns("[unclosed", "OTP")
        assertEquals("Invalid regex pattern", error)
    }

    @Test
    fun validatePatterns_acceptsValidRegex() {
        val error = ConfigurationValidator.validatePatterns("BANK", "OTP")
        assertNull(error)
    }

    @Test
    fun validateConfigCount_rejectsAtLimit() {
        val error = ConfigurationValidator.validateConfigCount(20)
        assertEquals("Maximum 20 configurations allowed", error)
    }
}
