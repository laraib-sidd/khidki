package dev.laraib.khidki.domain.phone

import dev.laraib.khidki.domain.model.PhoneNormalizeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNormalizerTest {
    private val normalizer = PhoneNormalizer()

    @Test
    fun normalize_indianMobileWithCountryCode() {
        val result = normalizer.normalize("+919876543210")
        assertTrue(result is PhoneNormalizeResult.Success)
        assertEquals("+919876543210", (result as PhoneNormalizeResult.Success).phone.e164)
    }

    @Test
    fun normalize_indianMobileNational() {
        val result = normalizer.normalize("9876543210")
        assertTrue(result is PhoneNormalizeResult.Success)
        assertEquals("+919876543210", (result as PhoneNormalizeResult.Success).phone.e164)
    }

    @Test
    fun normalize_rejectAlphanumericSender() {
        val result = normalizer.normalize("VM-HDFCBK")
        assertEquals(PhoneNormalizeResult.Reject.NotPhone, result)
    }

    @Test
    fun normalize_rejectInvalidFormat() {
        val result = normalizer.normalize("123")
        assertEquals(PhoneNormalizeResult.Reject.InvalidFormat, result)
    }

    @Test
    fun normalize_rejectEmpty() {
        val result = normalizer.normalize("   ")
        assertEquals(PhoneNormalizeResult.Reject.InvalidFormat, result)
    }

    @Test
    fun normalize_rejectAmbiguousNationalForForeignNumber() {
        val result = normalizer.normalize("4155552671")
        assertEquals(PhoneNormalizeResult.Reject.AmbiguousNational, result)
    }

    @Test
    fun normalizeOrRaw_keepsAlphanumericSender() {
        assertEquals("VM-HDFCBK", normalizer.normalizeOrRaw("VM-HDFCBK"))
    }

    @Test
    fun normalizeOrRaw_canonicalizesPhone() {
        assertEquals("+919876543210", normalizer.normalizeOrRaw("9876543210"))
    }
}
