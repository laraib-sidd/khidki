package dev.laraib.khidki.domain.model

import dev.laraib.khidki.domain.filter.ForwardingPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DestinationProfileTest {
    @Test
    fun roundTripJson() {
        val profile =
            DestinationProfile(
                name = "Mom",
                phoneE164 = "+919876543210",
                policy = ForwardingPolicy(otpBanks = true),
                enabled = true,
            )
        val encoded = DestinationProfiles.encode(listOf(profile))
        val decoded = DestinationProfiles.decode(encoded)
        assertEquals(1, decoded.size)
        assertEquals("Mom", decoded[0].name)
        assertEquals("+919876543210", decoded[0].phoneE164)
        assertTrue(decoded[0].policy.otpBanks)
    }

    @Test
    fun snapshotRoundTrip() {
        val snapshot =
            DestinationSnapshot(
                id = "abc",
                name = "Brother",
                phone = CanonicalPhone("+919111111111"),
                policy = ForwardingPolicy(otpShopping = true),
            )
        val encoded = DestinationSnapshot.encodeList(listOf(snapshot))
        val decoded = DestinationSnapshot.decodeList(encoded)
        assertEquals(1, decoded.size)
        assertEquals("Brother", decoded[0].name)
        assertTrue(decoded[0].policy.otpShopping)
    }
}
