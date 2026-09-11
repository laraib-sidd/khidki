package dev.laraib.khidki.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.ui.theme.CardBackgroundLight
import dev.laraib.khidki.ui.theme.TealPrimaryLight

@Composable
fun CommandRevealDialog(
    command: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forwarding command ready") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Share this exact command with the requester. " +
                        "The 8-digit password is only generated once.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = command,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackgroundLight)
                        .padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TealPrimaryLight,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    UiUtils.copyToClipboard(context, "khidki-command", command)
                    Toast.makeText(context, "Command copied", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text("Copy command")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
    )
}
