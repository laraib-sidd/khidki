package dev.laraib.khidki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSheet(
    onComplete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.titleLarge,
            )
            when (step) {
                0 -> WelcomeStep(
                    title = stringResource(R.string.welcome_step1_title),
                    body = stringResource(R.string.welcome_step1_body),
                )
                1 -> WelcomeStep(
                    title = stringResource(R.string.welcome_step2_title),
                    body = stringResource(R.string.welcome_step2_body),
                )
                else -> WelcomeStep(
                    title = stringResource(R.string.welcome_step3_title),
                    body = stringResource(R.string.welcome_step3_body),
                )
            }
            Button(
                onClick = {
                    if (step < 2) {
                        step += 1
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (step < 2) {
                        stringResource(R.string.welcome_continue)
                    } else {
                        stringResource(R.string.welcome_get_started)
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}
