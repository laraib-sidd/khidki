package dev.laraib.khidki.domain.filter

import org.json.JSONArray
import org.json.JSONObject

data class CustomSenderRule(
    val label: String,
    val senderContains: String,
    val codesOnly: Boolean = true,
) {
    fun toJson(): JSONObject =
        JSONObject()
            .put("label", label)
            .put("senderContains", senderContains)
            .put("codesOnly", codesOnly)

    companion object {
        fun fromJson(json: JSONObject): CustomSenderRule =
            CustomSenderRule(
                label = json.getString("label"),
                senderContains = json.getString("senderContains"),
                codesOnly = json.optBoolean("codesOnly", true),
            )
    }
}

data class ForwardingPolicy(
    val otpShopping: Boolean = true,
    val otpBanks: Boolean = true,
    val otpUpi: Boolean = true,
    val otpGovernment: Boolean = false,
    val otpOther: Boolean = false,
    val alertBank: Boolean = false,
    val alertOrder: Boolean = false,
    val allSms: Boolean = false,
    val customSenders: List<CustomSenderRule> = emptyList(),
) {
    fun hasAnyEnabled(): Boolean = enabledCategoryCount() > 0

    fun enabledCategoryCount(): Int =
        listOf(
            otpShopping,
            otpBanks,
            otpUpi,
            otpGovernment,
            otpOther,
            alertBank,
            alertOrder,
            allSms,
        ).count { it } + customSenders.size

    fun toJson(): JSONObject =
        JSONObject()
            .put("otpShopping", otpShopping)
            .put("otpBanks", otpBanks)
            .put("otpUpi", otpUpi)
            .put("otpGovernment", otpGovernment)
            .put("otpOther", otpOther)
            .put("alertBank", alertBank)
            .put("alertOrder", alertOrder)
            .put("allSms", allSms)
            .put(
                "customSenders",
                JSONArray().apply { customSenders.forEach { put(it.toJson()) } },
            )

    companion object {
        fun defaultFirstRun(): ForwardingPolicy = ForwardingPolicy()

        fun fromJson(raw: String): ForwardingPolicy {
            if (raw.isBlank()) {
                return ForwardingPolicy()
            }
            val json = JSONObject(raw)
            val custom =
                buildList {
                    val array = json.optJSONArray("customSenders") ?: JSONArray()
                    for (index in 0 until array.length()) {
                        add(CustomSenderRule.fromJson(array.getJSONObject(index)))
                    }
                }
            return ForwardingPolicy(
                otpShopping = json.optBoolean("otpShopping", true),
                otpBanks = json.optBoolean("otpBanks", true),
                otpUpi = json.optBoolean("otpUpi", true),
                otpGovernment = json.optBoolean("otpGovernment", false),
                otpOther = json.optBoolean("otpOther", false),
                alertBank = json.optBoolean("alertBank", false),
                alertOrder = json.optBoolean("alertOrder", false),
                allSms = json.optBoolean("allSms", false),
                customSenders = custom,
            )
        }
    }
}
