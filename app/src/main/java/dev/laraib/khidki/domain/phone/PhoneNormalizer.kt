package dev.laraib.khidki.domain.phone

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.PhoneNormalizeResult

class PhoneNormalizer(
    private val defaultRegion: String = DEFAULT_REGION,
) {
    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    fun normalize(raw: String): PhoneNormalizeResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return PhoneNormalizeResult.Reject.InvalidFormat
        }
        if (trimmed.any { it.isLetter() }) {
            return PhoneNormalizeResult.Reject.NotPhone
        }

        return try {
            if (trimmed.startsWith("+")) {
                parseInternational(trimmed)
            } else {
                parseNational(trimmed)
            }
        } catch (_: NumberParseException) {
            PhoneNormalizeResult.Reject.InvalidFormat
        }
    }

    fun normalizeOrRaw(raw: String): String =
        when (val result = normalize(raw)) {
            is PhoneNormalizeResult.Success -> result.phone.e164
            else -> raw.trim()
        }

    private fun parseInternational(raw: String): PhoneNormalizeResult {
        val parsed = phoneUtil.parse(raw, null)
        if (!phoneUtil.isValidNumber(parsed)) {
            return PhoneNormalizeResult.Reject.InvalidFormat
        }
        return PhoneNormalizeResult.Success(CanonicalPhone(phoneUtil.format(parsed, PhoneNumberFormat.E164)))
    }

    private fun parseNational(raw: String): PhoneNormalizeResult {
        if (!raw.all { it.isDigit() || it == ' ' || it == '-' }) {
            return PhoneNormalizeResult.Reject.InvalidFormat
        }

        val digitsOnly = raw.filter { it.isDigit() }
        if (digitsOnly.length == 10 && digitsOnly.first() !in '6'..'9') {
            return PhoneNormalizeResult.Reject.AmbiguousNational
        }

        val parsed = phoneUtil.parse(raw, defaultRegion)
        if (!phoneUtil.isValidNumber(parsed)) {
            return PhoneNormalizeResult.Reject.InvalidFormat
        }

        val region = phoneUtil.getRegionCodeForNumber(parsed)
        if (region != defaultRegion) {
            return PhoneNormalizeResult.Reject.AmbiguousNational
        }

        return PhoneNormalizeResult.Success(CanonicalPhone(phoneUtil.format(parsed, PhoneNumberFormat.E164)))
    }

    companion object {
        const val DEFAULT_REGION: String = "IN"
    }
}
