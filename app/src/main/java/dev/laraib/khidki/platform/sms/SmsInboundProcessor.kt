package dev.laraib.khidki.platform.sms

import dev.laraib.khidki.data.adapter.Blocking
import dev.laraib.khidki.data.repository.RoomDuplicateFingerprintStore
import dev.laraib.khidki.domain.model.CandidateHandleResult
import dev.laraib.khidki.domain.session.ForwardingEngine
import dev.laraib.khidki.platform.diagnostics.DiagnosticEventBus

class SmsInboundProcessor(
    private val engine: ForwardingEngine,
    private val duplicateFingerprintStore: RoomDuplicateFingerprintStore? = null,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    fun process(
        sender: String,
        body: String,
        receivedAtMillis: Long,
        subscriptionId: Int = UNKNOWN_SUBSCRIPTION,
    ) {
        if (body.length > MAX_BODY) {
            DiagnosticEventBus.record("DROP oversized body from ${maskSender(sender)}")
            return
        }
        if (OutboundSmsGuard.wasRecentlySent(body)) {
            DiagnosticEventBus.record("DROP loop-guard from ${maskSender(sender)}")
            return
        }

        DiagnosticEventBus.record("IN from ${maskSender(sender)} (${body.length} chars)")

        if (isDuplicateCandidate(sender, body, receivedAtMillis, subscriptionId)) {
            DiagnosticEventBus.record("DROP duplicate fingerprint from ${maskSender(sender)}")
            return
        }
        val result = engine.handleCandidate(sender, body, receivedAtMillis)
        DiagnosticEventBus.record("CANDIDATE result: ${result::class.simpleName}")
        if (result is CandidateHandleResult.Forwarded) {
            recordFingerprint(sender, body, receivedAtMillis, subscriptionId, result.session.id)
        }
    }

    private fun maskSender(sender: String): String {
        val digits = sender.filter { it.isDigit() }
        if (digits.length < 6) {
            return "••••"
        }
        return digits.takeLast(4).let { "••••$it" }
    }

    private fun isDuplicateCandidate(
        sender: String,
        body: String,
        receivedAtMillis: Long,
        subscriptionId: Int,
    ): Boolean {
        val store = duplicateFingerprintStore ?: return false
        return Blocking.io {
            store.isDuplicate(sender, body, receivedAtMillis, subscriptionId)
        }
    }

    private fun recordFingerprint(
        sender: String,
        body: String,
        receivedAtMillis: Long,
        subscriptionId: Int,
        sessionId: java.util.UUID,
    ) {
        val store = duplicateFingerprintStore ?: return
        Blocking.io {
            store.record(
                sender = sender,
                body = body,
                pduTimestampMillis = receivedAtMillis,
                subscriptionId = subscriptionId,
                sessionId = sessionId,
                recordedAtMillis = nowMillis(),
            )
        }
    }

    companion object {
        const val MAX_BODY = 8192
        const val UNKNOWN_SUBSCRIPTION = -1
    }
}
