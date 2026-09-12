package dev.laraib.khidki.ui.screens

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.SessionOrigin
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.KhidkiViewModel
import dev.laraib.khidki.ui.components.TimedForwardingCard
import dev.laraib.khidki.ui.components.UiUtils
import dev.laraib.khidki.ui.theme.StatusTone
import dev.laraib.khidki.ui.theme.statusToneColors
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun StatusScreen(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    hostActivity: FragmentActivity,
    diagnosticEvents: List<String>,
    onAuthCancelled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
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
            onAuthCancelled = onAuthCancelled,
        )

        state.activeSession?.let { session ->
            ActiveWindowCard(
                session = session,
                onCancel = { viewModel.cancelActiveSession(state.hasSmsPermission) },
            )
        } ?: EmptySessionCard()

        val context = LocalContext.current
        ActivityFeedCard(
            events = diagnosticEvents,
            advancedUnlocked = state.advancedUnlocked,
            onClear = { viewModel.clearDiagnosticEvents() },
            onCopyAll = {
                val payload = diagnosticEvents.joinToString(separator = "\n")
                UiUtils.copyToClipboard(context, "khidki-activity", payload)
            },
        )
    }
}

@Composable
private fun HeroStatusCard(
    state: KhidkiUiState,
    onMasterToggle: (Boolean) -> Unit,
) {
    val hasActiveSession = state.activeSession?.isActive == true
    val visual = remember(state.hasSmsPermission, state.masterEnabled, hasActiveSession) {
        when {
            !state.hasSmsPermission ->
                StatusVisual(
                    titleRes = R.string.setup_needed,
                    subtitleRes = R.string.setup_needed_subtitle,
                    tone = StatusTone.Error,
                    icon = Icons.Default.Warning,
                )
            !state.masterEnabled ->
                StatusVisual(
                    titleRes = R.string.forwarding_off,
                    subtitleRes = R.string.forwarding_off_subtitle,
                    tone = StatusTone.Warning,
                    icon = Icons.Default.PauseCircle,
                )
            hasActiveSession ->
                StatusVisual(
                    titleRes = R.string.forwarding_on,
                    subtitleRes = R.string.window_active_subtitle,
                    tone = StatusTone.Success,
                    icon = Icons.Default.CheckCircle,
                )
            else ->
                StatusVisual(
                    titleRes = R.string.forwarding_on,
                    subtitleRes = R.string.waiting_for_window,
                    tone = StatusTone.Success,
                    icon = Icons.Default.CheckCircle,
                )
        }
    }
    val (accent, background) = statusToneColors(visual.tone)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(visual.icon, contentDescription = null, tint = accent)
                Column {
                    Text(
                        text = stringResource(visual.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                    )
                    Text(
                        text = stringResource(visual.subtitleRes),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.forwarding_switch_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.forwarding_switch_hint),
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
    val titleRes: Int,
    val subtitleRes: Int,
    val tone: StatusTone,
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
    val (accent, background) = statusToneColors(StatusTone.Accent)

    val title =
        if (session.origin == SessionOrigin.TIMED) {
            stringResource(R.string.timed_window_title, session.label)
        } else {
            stringResource(R.string.sms_window_title, session.label)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = UiUtils.maskPhoneNumber(session.requester.e164),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.time_remaining, minutes, seconds),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (session.origin == SessionOrigin.TIMED) {
                Text(
                    text = stringResource(R.string.messages_forwarded, session.forwardCount),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.cancel_active_window))
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
            text = stringResource(R.string.no_active_window),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ActivityFeedCard(
    events: List<String>,
    advancedUnlocked: Boolean,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
) {
    var showTechnical by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.recent_activity_title),
                style = MaterialTheme.typography.titleSmall,
            )
            if (events.isEmpty()) {
                Text(
                    text = stringResource(R.string.recent_activity_empty),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    events.asReversed().take(10).forEach { raw ->
                        Text(
                            text = UiUtils.humanizeDiagnosticEvent(raw),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (advancedUnlocked) {
                    TextButton(onClick = { showTechnical = !showTechnical }) {
                        Text(
                            if (showTechnical) {
                                stringResource(R.string.hide_technical_details)
                            } else {
                                stringResource(R.string.show_technical_details)
                            },
                        )
                    }
                    if (showTechnical) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            events.asReversed().forEach { raw ->
                                Text(
                                    text = raw,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onCopyAll, enabled = events.isNotEmpty()) {
                                Text(stringResource(R.string.copy_activity_log))
                            }
                            OutlinedButton(onClick = onClear, enabled = events.isNotEmpty()) {
                                Text(stringResource(R.string.clear_activity))
                            }
                        }
                    }
                }
            }
        }
    }
}
