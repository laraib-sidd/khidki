package dev.laraib.khidki.domain.ports

import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuthResult
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.RuleMatchResult
import dev.laraib.khidki.domain.model.SendResult
import dev.laraib.khidki.domain.model.TerminalOutcome
import java.util.UUID

interface Clock {
    fun nowMillis(): Long

    fun bootId(): String

    fun monotonicNanos(): Long
}

interface ConfigurationRepository {
    fun findByRequester(requester: CanonicalPhone): Configuration?

    fun findById(id: ConfigurationId): Configuration?

    fun findAll(): List<Configuration>
}

interface CredentialVerifier {
    fun verify(requester: CanonicalPhone, password: String): Boolean

    fun isKnownRequester(requester: CanonicalPhone): Boolean
}

interface RequestAuthenticator {
    fun authenticate(requester: CanonicalPhone, password: String): AuthResult
}

interface RuleMatcher {
    fun isValid(rules: FilterRules): Boolean

    fun matches(
        rules: FilterRules,
        sender: String,
        body: String,
        trustedRequesters: Set<String> = emptySet(),
    ): RuleMatchResult
}

interface SessionRepository {
    fun getActiveSession(): AuthorizationSession?

    fun saveSession(session: AuthorizationSession?)

    fun terminateSession(
        sessionId: UUID,
        outcome: TerminalOutcome,
    ): AuthorizationSession?
}

interface BudgetLedger {
    fun estimateParts(body: String): Int

    fun canReserve(parts: Int): Boolean

    fun reserve(parts: Int): Boolean

    fun partsUsedInWindow(): Int
}

interface SmsTransport {
    fun send(to: CanonicalPhone, body: String): SendResult
}

interface AuditStore {
    fun record(event: AuditEvent)
}
