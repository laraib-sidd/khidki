package dev.laraib.khidki.platform.sms

import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.CommandParseResult
import dev.laraib.khidki.domain.model.PhoneNormalizeResult
import dev.laraib.khidki.domain.phone.PhoneNormalizer
import dev.laraib.khidki.domain.protocol.RequestParser
import dev.laraib.khidki.domain.session.ForwardingEngine

class SmsInboundProcessor(
    private val engine: ForwardingEngine,
    private val phoneNormalizer: PhoneNormalizer = PhoneNormalizer(),
    private val requestParser: RequestParser = RequestParser,
) {
    fun process(sender: String, body: String, receivedAtMillis: Long) {
        if (body.length > MAX_BODY) {
            return
        }
        if (OutboundSmsGuard.wasRecentlySent(body)) {
            return
        }

        when (val parsed = requestParser.parse(body)) {
            is CommandParseResult.Success -> {
                val requester = normalizeSender(sender) ?: return
                engine.handleCommand(requester, parsed.password)
            }

            is CommandParseResult.Reject -> {
                engine.handleCandidate(sender, body, receivedAtMillis)
            }
        }
    }

    private fun normalizeSender(sender: String): CanonicalPhone? =
        when (val result = phoneNormalizer.normalize(sender)) {
            is PhoneNormalizeResult.Success -> result.phone
            else -> null
        }

    companion object {
        const val MAX_BODY = 8192
    }
}
