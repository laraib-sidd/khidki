package dev.laraib.khidki.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.SessionOrigin
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.KhidkiViewModel
import dev.laraib.khidki.ui.components.DiagnosticChipType
import dev.laraib.khidki.ui.components.TimedForwardingCard
import dev.laraib.khidki.ui.components.UiUtils
import dev.laraib.khidki.ui.theme.StatusAmber
import dev.laraib.khidki.ui.theme.StatusAmberBg
import dev.laraib.khidki.ui.theme.StatusBlue
import dev.laraib.khidki.ui.theme.StatusBlueBg
import dev.laraib.khidki.ui.theme.StatusGreen
import dev.laraib.khidki.ui.theme.StatusGreenBg
import dev.laraib.khidki.ui.theme.StatusRed
import dev.laraib.khidki.ui.theme.StatusRedBg
import dev.laraib.khidki.ui.theme.TealPrimaryLight
import dev.laraib.khidki.ui.theme.TealContainerLight
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun StatusScreen(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    hostActivity: FragmentActivity,
    diagnosticEvents: List<String>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroStatusCard(
            state = state,
            onMasterToggle = { viewModel.setMasterEnabled(it, state.hasSmsPermission) },
        )

        TimedForwardingCard(
            configurations = state.configurations,
            activeSession = state.activeSession,
            masterEnabled = state.masterEnabled,
            hasSmsPermission = state.hasSmsPermission,
            hostActivity = hostActivity,
            onArm = { configId, durationSeconds ->
                viewModel.armTimedWindow(configId, durationSeconds, state.hasSmsPermission)
            },
        )

        state.activeSession?.let { session ->
            ActiveWindowCard(
                session = session,
                onCancel = { viewModel.cancelActiveSession(state.hasSmsPermission) },
            )
        } ?: EmptySessionCard()

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        ActivityFeedCard(
            events = diagnosticEvents,
            onClear = { viewModel.clearDiagnosticEvents() },
            onCopyAll = {
                val payload = diagnosticEvents.joinToString(separator = "\n")
                UiUtils.copyToClipboard(context, "khidki-logs", payload)
                Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun HeroStatusCard(
    state: KhidkiUiState,
    onMasterToggle: (Boolean) -> Unit,
) {
    val (statusTitle, statusSubtitle, statusColor, statusBg, icon) = remember(state.appStateLabel, state.hasSmsPermission) {
        when {
            !state.hasSmsPermission ->
                StatusVisual(
                    "SMS permission needed",
                    "Grant restricted SMS access to arm the engine",
                    StatusRed,
                    StatusRedBg,
                    Icons.Default.Warning,
                )
            state.appStateLabel == "PAUSED" ->
                StatusVisual(
                    "Engine paused",
                    "Inbound SMS will not be evaluated",
                    StatusAmber,
                    StatusAmberBg,
                    Icons.Default.PauseCircle,
                )
            else ->
                StatusVisual(
                    "Engine active",
                    "Ready to evaluate inbound SMS",
                    StatusGreen,
                    StatusGreenBg,
                    Icons.Default.CheckCircle,
                )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusBg),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null, tint = statusColor)
                    Column {
                        Text(statusTitle, style = MaterialTheme.typography.titleMedium, color = statusColor)
                        Text(statusSubtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Master forwarding engine", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Toggle all inbound SMS evaluation and forwarding",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = state.masterEnabled,
                    onCheckedChange = onMasterToggle,
                    enabled = state.hasSmsPermission,
                )
            }
        }
    }
}

private data class StatusVisual(
    val title: String,
    val subtitle: String,
    val color: androidx.compose.ui.graphics.Color,
    val background: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun ActiveWindowCard(
    session: AuthorizationSession,
    onCancel: () -> Unit,
) {
    var remainingMillis by remember(session.expiresAtMillis) {
        mutableLongStateOf(max(0L, session.expiresAtMillis - System.currentTimeMillis()))
    }
    val totalMillis = remember(session.armedAtMillis, session.expiresAtMillis) {
        max(1L, session.expiresAtMillis - session.armedAtMillis)
    }
    LaunchedEffect(session.expiresAtMillis) {
        while (remainingMillis > 0L) {
            delay(1_000)
            remainingMillis = max(0L, session.expiresAtMillis - System.currentTimeMillis())
        }
    }
    val progress = remainingMillis.toFloat() / totalMillis.toFloat()
    val minutes = (remainingMillis / 1_000L) / 60L
    val seconds = (remainingMillis / 1_000L) % 60L

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TealPrimaryLight, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = TealContainerLight.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val windowTitle =
                if (session.origin == SessionOrigin.TIMED) {
                    "Timed forwarding: ${session.label}"
                } else {
                    "Active forwarding window: ${session.label}"
                }
            Text(
                text = windowTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = UiUtils.maskPhoneNumber(session.requester.e164),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "%02d:%02d remaining".format(minutes, seconds),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (session.origin == SessionOrigin.TIMED) {
                Text(
                    text = "${session.forwardCount} message(s) forwarded",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "${session.windowSeconds}s window",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel active window")
            }
        }
    }
}

@Composable
private fun EmptySessionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text = "No active window. Arm timed forwarding above, or send a `req <password>` SMS.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ActivityFeedCard(
    events: List<String>,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Live diagnostic stream (${events.size})",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopyAll, enabled = events.isNotEmpty()) {
                    Text("Copy all logs")
                }
                OutlinedButton(onClick = onClear, enabled = events.isNotEmpty()) {
                    Text("Clear log")
                }
            }
            if (events.isEmpty()) {
                Text(
                    text = "No events yet. Send a test SMS to see activity.",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    events.asReversed().forEach { raw ->
                        DiagnosticEventRow(raw)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticEventRow(raw: String) {
    val chip = UiUtils.parseDiagnosticEvent(raw)
    val (chipColor, chipBg) = when (chip.type) {
        DiagnosticChipType.IN -> StatusBlue to StatusBlueBg
        DiagnosticChipType.CMD -> StatusGreen to StatusGreenBg
        DiagnosticChipType.FWD -> TealPrimaryLight to TealContainerLight.copy(alpha = 0.35f)
        DiagnosticChipType.DROP -> StatusAmber to StatusAmberBg
        DiagnosticChipType.LOG -> StatusBlue to StatusBlueBg
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "[${chip.type.name}]",
            modifier = Modifier
                .background(chipBg, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(color = chipColor),
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = chip.message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}
