package dev.laraib.khidki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.R

@Composable
fun CommandRevealDialog(
    command: String,
    onDismiss: () -> Unit,
    onCopied: () -> Unit = {},
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.command_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.command_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = command,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    UiUtils.copyToClipboard(context, "khidki-command", command)
                    onCopied()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.command_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.command_dismiss))
            }
        },
    )
}
