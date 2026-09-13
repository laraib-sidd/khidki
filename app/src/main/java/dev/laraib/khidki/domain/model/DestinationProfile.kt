package dev.laraib.khidki.domain.model

import dev.laraib.khidki.domain.filter.ForwardingPolicy
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class DestinationProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phoneE164: String,
    val policy: ForwardingPolicy,
    val enabled: Boolean = true,
) {
    fun toSnapshot(): DestinationSnapshot =
        DestinationSnapshot(
            id = id,
            name = name,
            phone = CanonicalPhone(phoneE164),
            policy = policy,
        )

    fun toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("phoneE164", phoneE164)
            .put("policy", policy.toJson())
            .put("enabled", enabled)

    companion object {
        const val MAX_DESTINATIONS: Int = 5

        fun fromJson(json: JSONObject): DestinationProfile =
            DestinationProfile(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.getString("name"),
                phoneE164 = json.getString("phoneE164"),
                policy = ForwardingPolicy.fromJson(json.getJSONObject("policy").toString()),
                enabled = json.optBoolean("enabled", true),
            )
    }
}

data class DestinationSnapshot(
    val id: String,
    val name: String,
    val phone: CanonicalPhone,
    val policy: ForwardingPolicy,
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("phoneE164", phone.e164)
            .put("policy", policy.toJson())

    companion object {
        fun fromJson(json: JSONObject): DestinationSnapshot =
            DestinationSnapshot(
                id = json.getString("id"),
                name = json.getString("name"),
                phone = CanonicalPhone(json.getString("phoneE164")),
                policy = ForwardingPolicy.fromJson(json.getJSONObject("policy").toString()),
            )

        fun encodeList(snapshots: List<DestinationSnapshot>): String =
            JSONArray().apply { snapshots.forEach { put(it.toJson()) } }.toString()

        fun decodeList(raw: String): List<DestinationSnapshot> {
            if (raw.isBlank() || raw == "[]") {
                return emptyList()
            }
            val array = JSONArray(raw)
            return buildList(array.length()) {
                for (index in 0 until array.length()) {
                    add(fromJson(array.getJSONObject(index)))
                }
            }
        }
    }
}

object DestinationProfiles {
    fun encode(profiles: List<DestinationProfile>): String =
        JSONArray().apply { profiles.forEach { put(it.toJson()) } }.toString()

    fun decode(raw: String?): List<DestinationProfile> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }
        val array = JSONArray(raw)
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                add(DestinationProfile.fromJson(array.getJSONObject(index)))
            }
        }
    }

    fun enabledWithPolicy(profiles: List<DestinationProfile>): List<DestinationProfile> =
        profiles.filter { it.enabled && it.policy.hasAnyEnabled() }
}
