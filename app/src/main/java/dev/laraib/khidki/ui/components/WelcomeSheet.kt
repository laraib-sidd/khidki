package dev.laraib.khidki.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.filter.ForwardingPolicy

private const val TOTAL_STEPS = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSheet(
    onComplete: (name: String, trustedNumber: String, policy: ForwardingPolicy) -> Unit,
    onSkip: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableIntStateOf(0) }
    var personName by remember { mutableStateOf("") }
    var trustedNumber by remember { mutableStateOf("") }
    var policy by remember { mutableStateOf(ForwardingPolicy.defaultFirstRun()) }

    ModalBottomSheet(
        onDismissRequest = onSkip,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_khidki_mark),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            OnboardingProgress(currentStep = step, totalSteps = TOTAL_STEPS)
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "welcome-step",
            ) { currentStep ->
            when (currentStep) {
                0 ->
                    WelcomeStep(
                        title = stringResource(R.string.welcome_step1_title),
                        body = stringResource(R.string.welcome_step1_body),
                    )
                1 -> {
                    WelcomeStep(
                        title = stringResource(R.string.welcome_step2_title),
                        body = stringResource(R.string.welcome_step2_body),
                    )
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text(stringResource(R.string.rules_person_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    OutlinedTextField(
                        value = trustedNumber,
                        onValueChange = { trustedNumber = it },
                        label = { Text(stringResource(R.string.rules_person_phone)) },
                        placeholder = { Text(stringResource(R.string.welcome_number_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                }
                2 -> {
                    WelcomeStep(
                        title = stringResource(R.string.welcome_step3_title),
                        body = stringResource(R.string.welcome_step3_body),
                    )
                    CategoryRow(
                        label = stringResource(R.string.rules_all_sms),
                        checked = policy.allSms,
                        onCheckedChange = { policy = policy.copy(allSms = it) },
                    )
                    CategoryRow(
                        label = stringResource(R.string.rules_otp_shopping),
                        checked = policy.otpShopping,
                        onCheckedChange = { policy = policy.copy(otpShopping = it) },
                    )
                    CategoryRow(
                        label = stringResource(R.string.rules_otp_banks),
                        checked = policy.otpBanks,
                        onCheckedChange = { policy = policy.copy(otpBanks = it) },
                    )
                    CategoryRow(
                        label = stringResource(R.string.rules_otp_upi),
                        checked = policy.otpUpi,
                        onCheckedChange = { policy = policy.copy(otpUpi = it) },
                    )
                    CategoryRow(
                        label = stringResource(R.string.rules_otp_government),
                        checked = policy.otpGovernment,
                        onCheckedChange = { policy = policy.copy(otpGovernment = it) },
                    )
                    Text(
                        text = stringResource(R.string.welcome_step3_government_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else ->
                    WelcomeStep(
                        title = stringResource(R.string.welcome_step4_title),
                        body = stringResource(R.string.welcome_step4_body),
                    )
            }
            }
            PrimaryActionButton(
                text =
                    if (step < TOTAL_STEPS - 1) {
                        stringResource(R.string.welcome_continue)
                    } else {
                        stringResource(R.string.welcome_get_started)
                    },
                onClick = {
                    if (step < TOTAL_STEPS - 1) {
                        step += 1
                    } else {
                        onComplete(personName, trustedNumber, policy)
                    }
                },
                enabled = step != 1 || (personName.isNotBlank() && trustedNumber.isNotBlank()),
            )
            if (step < TOTAL_STEPS - 1) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.welcome_skip))
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
