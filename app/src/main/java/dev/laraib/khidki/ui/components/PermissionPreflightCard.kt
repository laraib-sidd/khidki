package dev.laraib.khidki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.ui.theme.StatusAmber
import dev.laraib.khidki.ui.theme.StatusAmberBg

@Composable
fun PermissionPreflightCard(
    steps: List<String>,
    onOpenAppInfo: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StatusAmberBg),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Sideload setup required",
                style = MaterialTheme.typography.titleMedium,
                color = StatusAmber,
            )
            Text(
                text = "Android 15+ blocks SMS permissions until restricted settings are allowed. " +
                    "Complete these steps before granting SMS.",
                style = MaterialTheme.typography.bodyMedium,
            )
            steps.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. $step",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) {
                Text("Open App Info")
            }
            OutlinedButton(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("Grant SMS Permissions")
            }
        }
    }
}
