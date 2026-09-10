package dev.laraib.khidki.data.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dev.laraib.khidki.data.ports.PersistenceCredentialVerifier
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class KeystoreCredentialVerifier : PersistenceCredentialVerifier {
    private val keyAlias = "khidki_credential_hmac_v1"

    override fun computeTag(password: String): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(loadOrCreateKey())
        return mac.doFinal(password.toByteArray(Charsets.US_ASCII))
    }

    override fun verify(password: String, storedTag: ByteArray): Boolean {
        val computed = computeTag(password)
        return constantTimeEquals(computed, storedTag)
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEYSTORE)
        val parameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_SIGN,
        )
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val HMAC_ALGORITHM = "HmacSHA256"

        fun constantTimeEquals(left: ByteArray, right: ByteArray): Boolean {
            if (left.size != right.size) {
                return false
            }
            var result = 0
            for (index in left.indices) {
                result = result or (left[index].toInt() xor right[index].toInt())
            }
            return result == 0
        }
    }
}
