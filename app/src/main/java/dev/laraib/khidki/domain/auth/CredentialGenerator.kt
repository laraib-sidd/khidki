package dev.laraib.khidki.domain.auth

import java.security.SecureRandom

class CredentialGenerator(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun generate(): String {
        val value = secureRandom.nextInt(PASSWORD_SPACE)
        return value.toString().padStart(PASSWORD_LENGTH, '0')
    }

    companion object {
        const val PASSWORD_LENGTH: Int = 8
        private const val PASSWORD_SPACE: Int = 100_000_000
    }
}
