package dev.laraib.khidki.data.mapping

import dev.laraib.khidki.data.db.entities.BudgetReservationEntity
import dev.laraib.khidki.data.db.entities.ConfigurationEntity
import dev.laraib.khidki.data.db.entities.CredentialEntity
import dev.laraib.khidki.data.db.entities.HistoryEntity
import dev.laraib.khidki.data.db.entities.SessionEntity
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.BudgetReservation
import dev.laraib.khidki.domain.model.BudgetReservationStatus
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.domain.model.ConfigurationVersion
import dev.laraib.khidki.domain.model.CredentialPolicy
import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.HistoryEvent
import dev.laraib.khidki.domain.model.HistoryEventType
import dev.laraib.khidki.domain.model.SessionOrigin
import dev.laraib.khidki.domain.model.SessionState
import dev.laraib.khidki.domain.model.TerminalOutcome
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal fun ConfigurationEntity.toDomain(): Configuration {
    val rules = FilterRules(
        senderPatterns = decodeStringList(senderPatternsJson),
        contentPatterns = decodeStringList(contentPatternsJson),
        exclusionSenderPatterns = decodeStringList(exclusionSenderPatternsJson),
        exclusionContentPatterns = decodeStringList(exclusionContentPatternsJson),
    )
    return Configuration(
        id = ConfigurationId.parse(id),
        version = ConfigurationVersion(version),
        label = label,
        requester = CanonicalPhone(requesterE164),
        filterRules = rules,
        rules = rules,
        windowSeconds = windowSeconds,
        credentialPolicy = CredentialPolicy(lifetimeMs = credentialLifetimeMs, windowSeconds = windowSeconds),
        enabled = isEnabled,
        isEnabled = isEnabled,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )
}

internal fun Configuration.toEntity(): ConfigurationEntity =
    ConfigurationEntity(
        id = id.toString(),
        version = version.n,
        label = label,
        requesterE164 = requester.e164,
        senderPatternsJson = encodeStringList(rules.senderPatterns),
        contentPatternsJson = encodeStringList(rules.contentPatterns),
        exclusionSenderPatternsJson = encodeStringList(rules.exclusionSenderPatterns),
        exclusionContentPatternsJson = encodeStringList(rules.exclusionContentPatterns),
        windowSeconds = windowSeconds,
        credentialLifetimeMs = credentialPolicy.lifetimeMs,
        isEnabled = isEnabled,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
    )

internal fun Configuration.toSnapshotJson(): String {
    val json = JSONObject()
    json.put("id", id.toString())
    json.put("version", version.n)
    json.put("label", label)
    json.put("requesterE164", requester.e164)
    json.put("senderPatterns", JSONArray(rules.senderPatterns))
    json.put("contentPatterns", JSONArray(rules.contentPatterns))
    json.put("exclusionSenderPatterns", JSONArray(rules.exclusionSenderPatterns))
    json.put("exclusionContentPatterns", JSONArray(rules.exclusionContentPatterns))
    json.put("windowSeconds", windowSeconds)
    json.put("credentialLifetimeMs", credentialPolicy.lifetimeMs)
    json.put("isEnabled", isEnabled)
    return json.toString()
}

internal fun decodeConfigurationSnapshot(json: String): Configuration {
    val objectJson = JSONObject(json)
    val rules = FilterRules(
        senderPatterns = jsonArrayToList(objectJson.getJSONArray("senderPatterns")),
        contentPatterns = jsonArrayToList(objectJson.getJSONArray("contentPatterns")),
        exclusionSenderPatterns = jsonArrayToList(objectJson.optJSONArray("exclusionSenderPatterns")),
        exclusionContentPatterns = jsonArrayToList(objectJson.optJSONArray("exclusionContentPatterns")),
    )
    val windowSeconds = objectJson.getInt("windowSeconds")
    return Configuration(
        id = ConfigurationId.parse(objectJson.getString("id")),
        version = ConfigurationVersion(objectJson.getInt("version")),
        label = objectJson.getString("label"),
        requester = CanonicalPhone(objectJson.getString("requesterE164")),
        filterRules = rules,
        rules = rules,
        windowSeconds = windowSeconds,
        credentialPolicy = CredentialPolicy(
            lifetimeMs = objectJson.getLong("credentialLifetimeMs"),
            windowSeconds = windowSeconds,
        ),
        enabled = objectJson.getBoolean("isEnabled"),
        isEnabled = objectJson.getBoolean("isEnabled"),
        createdAtMillis = 0L,
        updatedAtMillis = 0L,
    )
}

internal fun SessionEntity.toDomain(): AuthorizationSession {
    val snapshot = decodeConfigurationSnapshot(configurationSnapshotJson)
    val sessionOrigin = SessionOrigin.valueOf(origin)
    return AuthorizationSession(
        id = UUID.fromString(id),
        configurationId = ConfigurationId.parse(configurationId),
        configurationVersion = ConfigurationVersion(configurationVersion),
        configurationSnapshot = snapshot,
        requester = CanonicalPhone(requesterE164),
        label = snapshot.label,
        filterRules = snapshot.filterRules,
        windowSeconds = snapshot.windowSeconds,
        state = SessionState.valueOf(state),
        terminalOutcome = terminalOutcome?.let(TerminalOutcome::valueOf),
        armedAtMillis = armedAtMillis,
        expiresAtMillis = expiresAtMillis,
        claimedAtMillis = claimedAtMillis,
        submittedAtMillis = submittedAtMillis,
        bootId = bootId,
        credentialId = credentialId?.let(UUID::fromString),
        origin = sessionOrigin,
        forwardCount = forwardCount,
        forwarded = when (sessionOrigin) {
            SessionOrigin.TIMED -> forwardCount > 0
            SessionOrigin.REQUEST ->
                state == SessionState.SUBMITTED.name &&
                    terminalOutcome == TerminalOutcome.COMPLETED.name
        },
    )
}

internal fun AuthorizationSession.toEntity(): SessionEntity {
    val snapshot = configurationSnapshot ?: activeConfiguration()
    return SessionEntity(
        id = id.toString(),
        configurationId = configurationId.toString(),
        configurationVersion = configurationVersion.n,
        configurationSnapshotJson = snapshot.toSnapshotJson(),
        requesterE164 = requester.e164,
        state = state.name,
        terminalOutcome = terminalOutcome?.name,
        armedAtMillis = armedAtMillis,
        expiresAtMillis = expiresAtMillis,
        claimedAtMillis = claimedAtMillis,
        submittedAtMillis = submittedAtMillis,
        bootId = bootId,
        credentialId = credentialId?.toString(),
        origin = origin.name,
        forwardCount = forwardCount,
    )
}

internal fun BudgetReservationEntity.toDomain(): BudgetReservation =
    BudgetReservation(
        id = UUID.fromString(id),
        sessionId = sessionId?.let(UUID::fromString),
        reservedParts = reservedParts,
        createdAtMillis = createdAtMillis,
        status = BudgetReservationStatus.valueOf(status),
    )

internal fun HistoryEntity.toDomain(): HistoryEvent =
    HistoryEvent(
        id = UUID.fromString(id),
        eventType = HistoryEventType.valueOf(eventType),
        timestampMillis = timestampMillis,
        requester = requesterE164?.let(::CanonicalPhone),
        configurationId = configurationId?.let(ConfigurationId::parse),
        sessionId = sessionId?.let(UUID::fromString),
        detail = decodeDetailMap(detailJson),
    )

internal fun HistoryEvent.toEntity(): HistoryEntity =
    HistoryEntity(
        id = id.toString(),
        eventType = eventType.name,
        timestampMillis = timestampMillis,
        requesterE164 = requester?.e164,
        configurationId = configurationId?.toString(),
        sessionId = sessionId?.toString(),
        detailJson = encodeDetailMap(detail),
    )

internal fun CredentialEntity.matchesPassword(
    password: String,
    verify: (String, ByteArray) -> Boolean,
): Boolean = verify(password, verificationTag)

private fun encodeStringList(values: List<String>): String = JSONArray(values).toString()

private fun decodeStringList(raw: String): List<String> =
    if (raw.isBlank()) {
        emptyList()
    } else {
        jsonArrayToList(JSONArray(raw))
    }

private fun jsonArrayToList(array: JSONArray?): List<String> {
    if (array == null) {
        return emptyList()
    }
    return buildList(array.length()) {
        for (index in 0 until array.length()) {
            add(array.getString(index))
        }
    }
}

private fun encodeDetailMap(detail: Map<String, String>): String? {
    if (detail.isEmpty()) {
        return null
    }
    val json = JSONObject()
    detail.forEach { (key, value) -> json.put(key, value) }
    return json.toString()
}

private fun decodeDetailMap(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) {
        return emptyMap()
    }
    val json = JSONObject(raw)
    return buildMap {
        json.keys().forEach { key ->
            put(key, json.getString(key))
        }
    }
}
