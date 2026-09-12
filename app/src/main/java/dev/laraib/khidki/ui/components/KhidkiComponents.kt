package dev.laraib.khidki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.ui.theme.StatusTone
import dev.laraib.khidki.ui.theme.statusToneColors

@Composable
fun OnboardingProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Step ${currentStep + 1} of $totalSteps",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EmptyStateCard(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SetupChecklistCard(
    hasSmsPermission: Boolean,
    hasTrustedNumber: Boolean,
    hasCategories: Boolean,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRules: () -> Unit,
    permissionsLabel: String,
    numberLabel: String,
    categoriesLabel: String,
    modifier: Modifier = Modifier,
) {
    if (hasSmsPermission && hasTrustedNumber && hasCategories) {
        return
    }
    val (accent, background) = statusToneColors(StatusTone.Warning)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Before you start",
                style = MaterialTheme.typography.titleSmall,
                color = accent,
            )
            SetupChecklistRow(
                done = hasSmsPermission,
                label = permissionsLabel,
                actionLabel = "Fix",
                onAction = onOpenPermissions,
            )
            SetupChecklistRow(
                done = hasTrustedNumber,
                label = numberLabel,
                actionLabel = "Add",
                onAction = onOpenSettings,
            )
            SetupChecklistRow(
                done = hasCategories,
                label = categoriesLabel,
                actionLabel = "Choose",
                onAction = onOpenRules,
            )
        }
    }
}

@Composable
private fun SetupChecklistRow(
    done: Boolean,
    label: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val (doneAccent, _) = statusToneColors(StatusTone.Success)
    val (pendingAccent, _) = statusToneColors(StatusTone.Warning)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (done) doneAccent else pendingAccent.copy(alpha = 0.5f)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!done) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ActivityFeedLine(
    raw: String,
    modifier: Modifier = Modifier,
) {
    val chip = UiUtils.parseDiagnosticEvent(raw)
    val (accent, background) = activityTone(chip.type)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = activityBadge(chip.type),
            modifier = Modifier
                .background(background, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
        Text(
            text = UiUtils.humanizeDiagnosticEvent(raw),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun activityBadge(type: DiagnosticChipType): String =
    when (type) {
        DiagnosticChipType.IN -> "IN"
        DiagnosticChipType.FWD -> "SENT"
        DiagnosticChipType.DROP -> "SKIP"
        DiagnosticChipType.CMD -> "CMD"
        DiagnosticChipType.LOG -> "INFO"
    }

@Composable
private fun activityTone(type: DiagnosticChipType): Pair<Color, Color> =
    when (type) {
        DiagnosticChipType.FWD -> statusToneColors(StatusTone.Success)
        DiagnosticChipType.IN -> statusToneColors(StatusTone.Info)
        DiagnosticChipType.DROP -> statusToneColors(StatusTone.Warning)
        DiagnosticChipType.CMD -> statusToneColors(StatusTone.Accent)
        DiagnosticChipType.LOG -> statusToneColors(StatusTone.Info)
    }

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}
