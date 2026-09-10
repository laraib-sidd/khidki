package dev.laraib.khidki.domain.auth

import dev.laraib.khidki.domain.model.AuthResult
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.ports.ConfigurationRepository
import dev.laraib.khidki.domain.ports.CredentialVerifier
import dev.laraib.khidki.domain.ports.RequestAuthenticator

class DefaultRequestAuthenticator(
    private val configurationRepository: ConfigurationRepository,
    private val credentialVerifier: CredentialVerifier,
    private val lockoutTracker: AuthLockoutTracker,
) : RequestAuthenticator {
    override fun authenticate(requester: CanonicalPhone, password: String): AuthResult {
        if (!credentialVerifier.isKnownRequester(requester)) {
            return AuthResult.UnknownRequester
        }
        if (lockoutTracker.isLocked(requester)) {
            return AuthResult.LockedOut
        }
        if (!credentialVerifier.verify(requester, password)) {
            lockoutTracker.recordFailure(requester)
            return AuthResult.InvalidPassword
        }
        lockoutTracker.recordSuccess(requester)
        if (configurationRepository.findByRequester(requester) == null) {
            return AuthResult.UnknownRequester
        }
        return AuthResult.Success
    }
}
