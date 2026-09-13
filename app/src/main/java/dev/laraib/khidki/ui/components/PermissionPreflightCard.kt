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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.R
import dev.laraib.khidki.ui.theme.StatusTone
import dev.laraib.khidki.ui.theme.statusToneColors

@Composable
fun PermissionPreflightCard(
    steps: List<String>,
    onOpenAppInfo: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (accent, background) = statusToneColors(StatusTone.Warning)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.setup_required_title),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
            )
            Text(
                text = stringResource(R.string.setup_required_body),
                style = MaterialTheme.typography.bodyMedium,
            )
            steps.forEachIndexed { index, step ->
                Text(
                    text = "${index + 1}. $step",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.open_app_info))
            }
            OutlinedButton(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.grant_permissions))
            }
        }
    }
}
