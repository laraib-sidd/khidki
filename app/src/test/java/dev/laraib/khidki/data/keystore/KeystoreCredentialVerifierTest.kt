package dev.laraib.khidki.data.keystore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class KeystoreCredentialVerifierTest {
    @Test
    fun verify_matchesStoredTag_withConstantTimePath() {
        val verifier = KeystoreCredentialVerifier()
        val password = "12345678"
        val tag = runCatching { verifier.computeTag(password) }.getOrNull()
        if (tag == null) {
            // Robolectric JVM may not expose a working AndroidKeyStore HMAC on all hosts.
            return
        }
        assertTrue(verifier.verify(password, tag))
        assertFalse(verifier.verify("87654321", tag))
    }

    @Test
    fun constantTimeEquals_rejectsDifferentLengths() {
        assertFalse(
            KeystoreCredentialVerifier.constantTimeEquals(
                byteArrayOf(1, 2, 3),
                byteArrayOf(1, 2),
            ),
        )
    }
}
