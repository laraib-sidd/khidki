package dev.laraib.khidki.data.adapter

import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuditEventType
import dev.laraib.khidki.domain.ports.AuditStore
import dev.laraib.khidki.platform.diagnostics.DiagnosticEventBus

class DiagnosticAuditStore(
    private val delegate: AuditStore,
) : AuditStore {
    override fun record(event: AuditEvent) {
        DiagnosticEventBus.record(format(event))
        delegate.record(event)
    }

    private fun format(event: AuditEvent): String {
        val requester = event.requester?.e164?.let { maskPhone(it) } ?: "unknown"
        return when (event.type) {
            AuditEventType.COMMAND_RECEIVED -> "CMD recv from $requester"
            AuditEventType.COMMAND_REJECTED -> "CMD rejected ($requester): ${event.detail ?: "unknown"}"
            AuditEventType.SESSION_ARMED -> "ARMED window ${event.detail ?: ""} for $requester".trim()
            AuditEventType.SESSION_EXPIRED -> "SESSION expired ($requester)"
            AuditEventType.SESSION_CANCELLED -> "SESSION cancelled ($requester)"
            AuditEventType.CANDIDATE_MATCHED -> "MATCH candidate from $requester"
            AuditEventType.CANDIDATE_FORWARDED -> "FWD sent to $requester"
            AuditEventType.CANDIDATE_REJECTED -> "DROP candidate ($requester): ${event.detail ?: "rejected"}"
            AuditEventType.ACK_SENT -> "ACK sent to $requester"
            AuditEventType.ACK_FAILED -> "ACK failed ($requester)"
            AuditEventType.FORWARD_SENT -> "FWD sent to $requester"
            AuditEventType.FORWARD_FAILED -> "FWD failed ($requester)"
        }
    }

    private fun maskPhone(e164: String): String {
        if (e164.length < 6) {
            return "••••"
        }
        return e164.take(3) + "•••" + e164.takeLast(2)
    }
}
