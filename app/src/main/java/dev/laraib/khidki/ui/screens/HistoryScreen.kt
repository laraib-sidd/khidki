package dev.laraib.khidki.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.domain.model.HistoryEvent
import dev.laraib.khidki.domain.model.HistoryEventType
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.KhidkiViewModel
import dev.laraib.khidki.ui.components.EmptyStateCard
import dev.laraib.khidki.ui.components.UiUtils
import androidx.compose.ui.res.stringResource
import dev.laraib.khidki.R
import dev.laraib.khidki.ui.theme.StatusTone
import dev.laraib.khidki.ui.theme.statusToneColors

@Composable
fun HistoryScreen(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    onOpenHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hasSmsPermission = state.hasSmsPermission
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.history_title, state.history.size),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedButton(
                onClick = { showClearConfirm = true },
                enabled = state.history.isNotEmpty(),
            ) {
                Text(stringResource(R.string.history_clear))
            }
        }

        if (state.history.isEmpty()) {
            EmptyStateCard(
                message = stringResource(R.string.history_empty),
                actionLabel = stringResource(R.string.history_empty_action),
                onAction = onOpenHome,
                modifier = Modifier.padding(12.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            ) {
                items(state.history, key = { it.id }) { event ->
                    HistoryEventCard(event)
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory(hasSmsPermission)
                        showClearConfirm = false
                    },
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun HistoryEventCard(event: HistoryEvent) {
    val (badgeColor, badgeBg) = historyBadgeColors(event.eventType)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = UiUtils.formatTimestamp(event.timestampMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = UiUtils.historyEventLabel(event.eventType),
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                )
            }
            Text(
                text = UiUtils.historyEventSummary(event),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun historyBadgeColors(type: HistoryEventType): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> =
    when (type) {
        HistoryEventType.SESSION_ARMED, HistoryEventType.TIMED_ARMED ->
            statusToneColors(StatusTone.Success)
        HistoryEventType.SESSION_SUBMITTED, HistoryEventType.SESSION_CLAIMED ->
            statusToneColors(StatusTone.Info)
        HistoryEventType.AUTH_FAILURE, HistoryEventType.AUTH_LOCKOUT ->
            statusToneColors(StatusTone.Error)
        HistoryEventType.DUPLICATE_REJECTED, HistoryEventType.BUDGET_REJECTED,
        HistoryEventType.TIMED_CANCELLED, HistoryEventType.TIMED_EXPIRED,
        ->
            statusToneColors(StatusTone.Warning)
        HistoryEventType.SESSION_TERMINAL, HistoryEventType.CONFIG_CHANGED ->
            statusToneColors(StatusTone.Info)
    }
