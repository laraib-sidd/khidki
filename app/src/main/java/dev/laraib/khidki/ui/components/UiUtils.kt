package dev.laraib.khidki.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import dev.laraib.khidki.domain.model.HistoryEvent
import dev.laraib.khidki.domain.model.HistoryEventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagnosticChipType {
    IN,
    CMD,
    FWD,
    DROP,
    LOG,
}

data class DiagnosticChip(
    val type: DiagnosticChipType,
    val message: String,
)

object UiUtils {
    fun maskPhoneNumber(e164: String): String {
        val trimmed = e164.trim()
        if (trimmed.length < 8) return "••••"
        return "${trimmed.take(4)} •••• ${trimmed.takeLast(2)}"
    }

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return "—"
        val now = System.currentTimeMillis()
        val diff = now - millis
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateStr = formatter.format(Date(millis))

        return when {
            diff < 60_000L -> "Just now ($dateStr)"
            diff < 3_600_000L -> "${diff / 60_000L}m ago ($dateStr)"
            diff < 86_400_000L -> "${diff / 3_600_000L}h ago ($dateStr)"
            else -> {
                val fullFormatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                fullFormatter.format(Date(millis))
            }
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    fun parseDiagnosticEvent(raw: String): DiagnosticChip {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("IN ", ignoreCase = true) ->
                DiagnosticChip(DiagnosticChipType.IN, trimmed)
            trimmed.startsWith("CMD", ignoreCase = true) ->
                DiagnosticChip(DiagnosticChipType.CMD, trimmed)
            trimmed.contains("FORWARD", ignoreCase = true) ||
                trimmed.startsWith("FWD", ignoreCase = true) ->
                DiagnosticChip(DiagnosticChipType.FWD, trimmed)
            trimmed.startsWith("DROP", ignoreCase = true) ||
                trimmed.startsWith("CANDIDATE", ignoreCase = true) ->
                DiagnosticChip(DiagnosticChipType.DROP, trimmed)
            else -> DiagnosticChip(DiagnosticChipType.LOG, trimmed)
        }
    }

    fun historyEventLabel(type: HistoryEventType): String =
        when (type) {
            HistoryEventType.SESSION_ARMED -> "Session armed"
            HistoryEventType.SESSION_CLAIMED -> "Session claimed"
            HistoryEventType.SESSION_SUBMITTED -> "Session request received"
            HistoryEventType.SESSION_TERMINAL -> "Session closed"
            HistoryEventType.AUTH_FAILURE -> "Authentication failed"
            HistoryEventType.AUTH_LOCKOUT -> "Authentication lockout"
            HistoryEventType.CONFIG_CHANGED -> "Configuration changed"
            HistoryEventType.BUDGET_REJECTED -> "Budget limit reached"
            HistoryEventType.DUPLICATE_REJECTED -> "Duplicate blocked"
        }

    fun historyEventSummary(event: HistoryEvent): String {
        val detail = event.detail.entries
            .joinToString(" · ") { "${it.key}: ${it.value}" }
            .ifBlank { "No additional details" }
        val requester = event.requester?.e164?.let { maskPhoneNumber(it) }
        return buildString {
            append(historyEventLabel(event.eventType))
            if (requester != null) {
                append(" · ")
                append(requester)
            }
            append("\n")
            append(detail)
        }
    }
}
