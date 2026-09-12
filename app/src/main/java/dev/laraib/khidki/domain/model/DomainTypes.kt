package dev.laraib.khidki.domain.model

import dev.laraib.khidki.domain.filter.ForwardingPolicy
import java.util.UUID

@JvmInline
value class CanonicalPhone(val e164: String) {
    init {
        require(e164.isNotBlank()) { "CanonicalPhone must not be blank" }
    }
}

data class ConfigurationId(val uuid: UUID) {
    companion object {
        fun parse(raw: String): ConfigurationId = ConfigurationId(UUID.fromString(raw))
    }

    override fun toString(): String = uuid.toString()
}

data class ConfigurationVersion(val n: Int) {
    init {
        require(n >= 1) { "ConfigurationVersion must be >= 1" }
    }
}

enum class AppState {
    PAUSED,
    READY,
    BLOCKED_PERMISSION,
    BLOCKED_CAPABILITY,
}

enum class SessionOrigin {
    REQUEST,
    TIMED,
}

enum class SessionState {
    ARMED,
    CLAIMED,
    SUBMITTING,
    SUBMITTED,
}

enum class TerminalOutcome {
    COMPLETED,
    EXPIRED,
    CANCELLED,
    FAILED,
    UNCERTAIN,
}

sealed class IncomingSms {
    data class Command(
        val from: CanonicalPhone,
        val raw: String,
        val password: String? = null,
    ) : IncomingSms()

    data class Candidate(
        val from: String,
        val body: String,
        val receivedAtMillis: Long,
    ) : IncomingSms()
}

data class FilterRules(
    val senderPatterns: List<String>,
    val contentPatterns: List<String>,
    val senderExclusions: List<String> = emptyList(),
    val contentExclusions: List<String> = emptyList(),
    val exclusionSenderPatterns: List<String> = emptyList(),
    val exclusionContentPatterns: List<String> = emptyList(),
) {
    fun effectiveSenderExclusions(): List<String> =
        if (senderExclusions.isNotEmpty()) senderExclusions else exclusionSenderPatterns

    fun effectiveContentExclusions(): List<String> =
        if (contentExclusions.isNotEmpty()) contentExclusions else exclusionContentPatterns

    fun allPatterns(): List<String> =
        senderPatterns +
            contentPatterns +
            effectiveSenderExclusions() +
            effectiveContentExclusions()
}

data class CredentialPolicy(
    val lifetimeMillis: Long = DEFAULT_LIFETIME_MILLIS,
    val windowSeconds: Int = DEFAULT_WINDOW_SECONDS,
    val lifetimeMs: Long = lifetimeMillis,
) {
    companion object {
        const val DEFAULT_LIFETIME_MILLIS: Long = 24L * 60L * 60L * 1000L
        const val DEFAULT_WINDOW_SECONDS: Int = 120
        const val MIN_WINDOW_SECONDS: Int = 30
        const val MAX_WINDOW_SECONDS: Int = 300
    }

    init {
        require(windowSeconds in MIN_WINDOW_SECONDS..MAX_WINDOW_SECONDS) {
            "windowSeconds must be between $MIN_WINDOW_SECONDS and $MAX_WINDOW_SECONDS"
        }
        require(lifetimeMillis > 0) { "lifetimeMillis must be positive" }
    }
}

data class Configuration(
    val id: ConfigurationId,
    val version: ConfigurationVersion,
    val label: String,
    val requester: CanonicalPhone,
    val filterRules: FilterRules,
    val rules: FilterRules = filterRules,
    val credentialPolicy: CredentialPolicy = CredentialPolicy(),
    val windowSeconds: Int = credentialPolicy.windowSeconds,
    val enabled: Boolean = true,
    val isEnabled: Boolean = enabled,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

data class AuthorizationSession(
    val id: UUID,
    val configurationId: ConfigurationId,
    val configurationVersion: ConfigurationVersion,
    val requester: CanonicalPhone,
    val state: SessionState,
    val armedAtMillis: Long,
    val expiresAtMillis: Long,
    val bootId: String,
    val label: String,
    val filterRules: FilterRules,
    val windowSeconds: Int,
    val configurationSnapshot: Configuration? = null,
    val terminalOutcome: TerminalOutcome? = null,
    val claimedAtMillis: Long? = null,
    val submittedAtMillis: Long? = null,
    val credentialId: UUID? = null,
    val forwarded: Boolean = false,
    val origin: SessionOrigin = SessionOrigin.REQUEST,
    val forwardCount: Int = 0,
    val forwardingPolicy: ForwardingPolicy = ForwardingPolicy(),
) {
    val isActive: Boolean
        get() = when (origin) {
            SessionOrigin.TIMED -> terminalOutcome == null
            SessionOrigin.REQUEST -> terminalOutcome == null && state != SessionState.SUBMITTED
        }

    fun isExpired(nowMillis: Long): Boolean = nowMillis >= expiresAtMillis

    fun activeConfiguration(): Configuration =
        configurationSnapshot ?: Configuration(
            id = configurationId,
            version = configurationVersion,
            label = label,
            requester = requester,
            filterRules = filterRules,
            credentialPolicy = CredentialPolicy(windowSeconds = windowSeconds),
            enabled = true,
        )
}

sealed class CommandParseResult {
    data class Success(val password: String) : CommandParseResult()

    sealed class Reject : CommandParseResult() {
        data object TooLong : Reject()

        data object Multiline : Reject()

        data object NonAscii : Reject()

        data object WrongKeyword : Reject()

        data object MissingToken : Reject()

        data object ExtraTokens : Reject()

        data object WrongLength : Reject()

        data object NonDigit : Reject()

        data object UnicodeDigit : Reject()
    }
}

sealed class PhoneNormalizeResult {
    data class Success(val phone: CanonicalPhone) : PhoneNormalizeResult()

    sealed class Reject : PhoneNormalizeResult() {
        data object InvalidFormat : Reject()

        data object AmbiguousNational : Reject()

        data object NotPhone : Reject()
    }
}

enum class RuleMatchOutcome {
    MATCHED,
    NO_MATCH,
    EXCLUDED,
    INVALID_RULES,
    BODY_TOO_LONG,
}

data class RuleMatchResult(val outcome: RuleMatchOutcome)

sealed class CommandHandleResult {
    data class SessionCreated(val session: AuthorizationSession) : CommandHandleResult()

    data object IgnoredActiveSession : CommandHandleResult()

    data object UnknownRequester : CommandHandleResult()

    data object LockedOut : CommandHandleResult()

    data object AuthFailed : CommandHandleResult()

    data object AppPaused : CommandHandleResult()

    data object AppNotReady : CommandHandleResult()

    data object BudgetExceeded : CommandHandleResult()

    data object AckFailed : CommandHandleResult()
}

sealed class CandidateHandleResult {
    data class Forwarded(val session: AuthorizationSession) : CandidateHandleResult()

    data object NoMatch : CandidateHandleResult()

    data object NoActiveSession : CandidateHandleResult()

    data object SessionExpired : CandidateHandleResult()

    data object AlreadyForwarded : CandidateHandleResult()

    data object AppPaused : CandidateHandleResult()

    data object OversizedMessage : CandidateHandleResult()

    data object BudgetExceeded : CandidateHandleResult()

    data object ForwardFailed : CandidateHandleResult()
}

sealed class AuthResult {
    data object Success : AuthResult()

    data object UnknownRequester : AuthResult()

    data object LockedOut : AuthResult()

    data object InvalidPassword : AuthResult()
}

enum class AuditEventType {
    COMMAND_RECEIVED,
    COMMAND_REJECTED,
    SESSION_ARMED,
    SESSION_EXPIRED,
    SESSION_CANCELLED,
    TIMED_ARMED,
    TIMED_CANCELLED,
    TIMED_EXPIRED,
    CANDIDATE_MATCHED,
    CANDIDATE_FORWARDED,
    CANDIDATE_REJECTED,
    ACK_SENT,
    ACK_FAILED,
    FORWARD_SENT,
    FORWARD_FAILED,
}

data class AuditEvent(
    val type: AuditEventType,
    val atMillis: Long,
    val requester: CanonicalPhone? = null,
    val sessionId: UUID? = null,
    val detail: String? = null,
)

enum class SendOutcome {
    SENT,
    FAILED,
}

data class SendResult(
    val outcome: SendOutcome,
    val parts: Int,
)

data class HistoryEvent(
    val id: UUID,
    val eventType: HistoryEventType,
    val timestampMillis: Long,
    val requester: CanonicalPhone?,
    val configurationId: ConfigurationId?,
    val sessionId: UUID?,
    val detail: Map<String, String>,
)

enum class HistoryEventType {
    SESSION_ARMED,
    SESSION_CLAIMED,
    SESSION_SUBMITTED,
    SESSION_TERMINAL,
    TIMED_ARMED,
    TIMED_CANCELLED,
    TIMED_EXPIRED,
    AUTH_FAILURE,
    AUTH_LOCKOUT,
    CONFIG_CHANGED,
    BUDGET_REJECTED,
    DUPLICATE_REJECTED,
}

enum class BudgetReservationStatus {
    PENDING,
    CONSUMED,
    UNCERTAIN,
}

data class BudgetReservation(
    val id: UUID,
    val sessionId: UUID?,
    val reservedParts: Int,
    val createdAtMillis: Long,
    val status: BudgetReservationStatus,
)

sealed class ArmSessionResult {
    data class Success(val session: AuthorizationSession) : ArmSessionResult()

    data class Rejected(val reason: ArmRejectReason) : ArmSessionResult()
}

enum class ArmRejectReason {
    UNKNOWN_REQUESTER,
    INVALID_PASSWORD,
    LOCKED_OUT,
    ACTIVE_SESSION_EXISTS,
    CONFIGURATION_DISABLED,
    CONFIGURATION_NOT_FOUND,
}

sealed class TimedArmResult {
    data class Success(val session: AuthorizationSession) : TimedArmResult()

    data class Rejected(val reason: TimedArmRejectReason) : TimedArmResult()
}

enum class TimedArmRejectReason {
    CONFIGURATION_NOT_FOUND,
    CONFIGURATION_DISABLED,
    ACTIVE_SESSION_EXISTS,
    DURATION_OUT_OF_RANGE,
    APP_PAUSED,
    APP_NOT_READY,
    NO_DESTINATION,
    NO_CATEGORIES_ENABLED,
}
