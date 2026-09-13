package dev.laraib.khidki.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.DestinationProfiles
import dev.laraib.khidki.domain.session.ForwardingEngine
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.KhidkiViewModel
import dev.laraib.khidki.ui.components.ActivityFeedLine
import dev.laraib.khidki.ui.components.PrimaryActionButton
import dev.laraib.khidki.ui.components.ScreenHeader
import dev.laraib.khidki.ui.components.SetupChecklistCard
import dev.laraib.khidki.ui.components.UiUtils
import dev.laraib.khidki.ui.theme.StatusRed
import dev.laraib.khidki.ui.theme.StatusTone
import dev.laraib.khidki.ui.theme.statusToneColors
import kotlinx.coroutines.delay
import kotlin.math.max

private data class DurationOption(val label: String, val seconds: Int)

private val durationOptions =
    listOf(
        DurationOption("15m", 15 * 60),
        DurationOption("30m", 30 * 60),
        DurationOption("1h", 60 * 60),
        DurationOption("2h", 2 * 60 * 60),
    )

@Composable
fun StatusScreen(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    diagnosticEvents: List<String>,
    onOpenSettings: () -> Unit,
    onOpenRules: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDuration by remember { mutableLongStateOf(durationOptions[1].seconds.toLong()) }
    val activeSession = state.activeSession?.takeIf { it.isActive }
    val enabledPeople = DestinationProfiles.enabledWithPolicy(state.destinations)
    val canStart =
        state.hasSmsPermission &&
            enabledPeople.isNotEmpty() &&
            activeSession == null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.home_screen_title),
            subtitle = stringResource(R.string.home_screen_subtitle),
        )

        if (activeSession == null) {
            SetupChecklistCard(
                hasSmsPermission = state.hasSmsPermission,
                hasTrustedNumber = state.destinations.isNotEmpty(),
                hasCategories = enabledPeople.isNotEmpty(),
                onOpenPermissions = onRequestPermissions,
                onOpenSettings = onOpenRules,
                onOpenRules = onOpenRules,
                permissionsLabel = stringResource(R.string.home_check_permissions),
                numberLabel = stringResource(R.string.home_check_people),
                categoriesLabel = stringResource(R.string.home_check_categories),
            )
        }

        Crossfade(
            targetState = activeSession != null,
            label = "home-forwarding-state",
        ) { isActive ->
            if (isActive && activeSession != null) {
                ActiveForwardingCard(
                    session = activeSession,
                    destinationCount = activeSession.activeDestinations().size,
                    onStop = { viewModel.stopForwarding(state.hasSmsPermission) },
                )
            } else {
                StartForwardingCard(
                    canStart = canStart,
                    selectedDurationSeconds = selectedDuration.toInt(),
                    enabledPeopleCount = enabledPeople.size,
                    people = enabledPeople,
                    hasAnyPeople = state.destinations.isNotEmpty(),
                    onOpenRules = onOpenRules,
                    onDurationSelected = { selectedDuration = it.toLong() },
                    onSelectUntilStop = { selectedDuration = ForwardingEngine.DURATION_UNTIL_STOP.toLong() },
                    onStart = {
                        viewModel.startForwarding(selectedDuration.toInt(), state.hasSmsPermission)
                    },
                )
            }
        }

        val context = LocalContext.current
        RecentActivityCard(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartForwardingCard(
    canStart: Boolean,
    selectedDurationSeconds: Int,
    enabledPeopleCount: Int,
    people: List<dev.laraib.khidki.domain.model.DestinationProfile>,
    hasAnyPeople: Boolean,
    onOpenRules: () -> Unit,
    onDurationSelected: (Int) -> Unit,
    onSelectUntilStop: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!hasAnyPeople) {
                Text(
                    text = stringResource(R.string.home_no_people_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenRules) {
                    Text(stringResource(R.string.home_go_to_rules))
                }
            } else {
                Text(
                    text = stringResource(R.string.home_forwarding_to, enabledPeopleCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (people.isNotEmpty()) {
                    Text(
                        text = people.map { it.name }.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            if (hasAnyPeople) {
            Text(
                text = stringResource(R.string.home_duration_label),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                durationOptions.forEach { option ->
                    FilterChip(
                        selected = selectedDurationSeconds == option.seconds,
                        onClick = { onDurationSelected(option.seconds) },
                        label = { Text(option.label) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                FilterChip(
                    selected = selectedDurationSeconds == ForwardingEngine.DURATION_UNTIL_STOP,
                    onClick = onSelectUntilStop,
                    label = { Text(stringResource(R.string.home_duration_until_stop)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
            PrimaryActionButton(
                text = stringResource(R.string.home_start_button),
                onClick = onStart,
                enabled = canStart,
            )
            }
            if (!canStart && hasAnyPeople) {
                Text(
                    text = stringResource(R.string.home_start_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ActiveForwardingCard(
    session: AuthorizationSession,
    destinationCount: Int,
    onStop: () -> Unit,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(session.id) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val untilStop = session.untilStop
    val remainingMillis = if (untilStop) null else max(0L, session.expiresAtMillis - nowMillis)
    val totalMillis = (session.expiresAtMillis - session.armedAtMillis).coerceAtLeast(1L)
    val progress =
        if (untilStop) {
            1f
        } else {
            remainingMillis!!.toFloat() / totalMillis.toFloat()
        }
    val (accent, background) = statusToneColors(StatusTone.Success)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_active_title),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                text =
                    if (untilStop) {
                        stringResource(R.string.home_until_stop_label)
                    } else {
                        formatRemaining(remainingMillis!!)
                    },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!untilStop) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = accent,
                )
            }
            Text(
                text = stringResource(R.string.home_active_destinations, destinationCount),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.messages_forwarded, session.forwardCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_stop_button),
                    color = StatusRed,
                )
            }
        }
    }
}

@Composable
private fun RecentActivityCard(
    events: List<String>,
    advancedUnlocked: Boolean,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.recent_activity_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (advancedUnlocked && events.isNotEmpty()) {
                    androidx.compose.foundation.layout.Row {
                        TextButton(onClick = onCopyAll) {
                            Text(stringResource(R.string.copy_activity_log))
                        }
                        TextButton(onClick = onClear) {
                            Text(stringResource(R.string.clear_activity))
                        }
                    }
                }
            }
            if (events.isEmpty()) {
                Text(
                    text = stringResource(R.string.recent_activity_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                val visible = if (advancedUnlocked) events.takeLast(20) else events.takeLast(5)
                visible.reversed().forEach { line ->
                    if (advancedUnlocked) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    } else {
                        ActivityFeedLine(raw = line)
                    }
                }
            }
        }
    }
}

private fun formatRemaining(remainingMillis: Long): String {
    val totalSeconds = remainingMillis / 1_000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
