package dev.laraib.khidki.platform.sms

import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.CommandParseResult
import dev.laraib.khidki.domain.model.PhoneNormalizeResult
import dev.laraib.khidki.domain.phone.PhoneNormalizer
import dev.laraib.khidki.domain.protocol.RequestParser
import dev.laraib.khidki.domain.session.ForwardingEngine
import dev.laraib.khidki.platform.diagnostics.DiagnosticEventBus

class SmsInboundProcessor(
    private val engine: ForwardingEngine,
    private val phoneNormalizer: PhoneNormalizer = PhoneNormalizer(),
    private val requestParser: RequestParser = RequestParser,
) {
    fun process(sender: String, body: String, receivedAtMillis: Long) {
        if (body.length > MAX_BODY) {
            DiagnosticEventBus.record("DROP oversized body from ${maskSender(sender)}")
            return
        }
        if (OutboundSmsGuard.wasRecentlySent(body)) {
            DiagnosticEventBus.record("DROP loop-guard from ${maskSender(sender)}")
            return
        }

        DiagnosticEventBus.record("IN from ${maskSender(sender)} (${body.length} chars)")

        when (val parsed = requestParser.parse(body)) {
            is CommandParseResult.Success -> {
                val requester = normalizeSender(sender) ?: run {
                    DiagnosticEventBus.record("DROP command: invalid sender ${maskSender(sender)}")
                    return
                }
                val result = engine.handleCommand(requester, parsed.password)
                DiagnosticEventBus.record("CMD result: ${result::class.simpleName}")
            }

            is CommandParseResult.Reject -> {
                val result = engine.handleCandidate(sender, body, receivedAtMillis)
                DiagnosticEventBus.record("CANDIDATE result: ${result::class.simpleName}")
            }
        }
    }

    private fun maskSender(sender: String): String {
        val digits = sender.filter { it.isDigit() }
        if (digits.length < 6) {
            return "••••"
        }
        return digits.takeLast(4).let { "••••$it" }
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
