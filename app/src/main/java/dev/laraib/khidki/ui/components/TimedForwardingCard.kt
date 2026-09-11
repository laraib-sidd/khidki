package dev.laraib.khidki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.domain.model.SessionOrigin
import dev.laraib.khidki.platform.auth.DeviceCredentialGate

data class TimedDurationPreset(
    val label: String,
    val seconds: Int,
)

private val durationPresets =
    listOf(
        TimedDurationPreset("15m", 900),
        TimedDurationPreset("30m", 1_800),
        TimedDurationPreset("1h", 3_600),
        TimedDurationPreset("2h", 7_200),
    )

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TimedForwardingCard(
    configurations: List<Configuration>,
    activeSession: AuthorizationSession?,
    masterEnabled: Boolean,
    hasSmsPermission: Boolean,
    hostActivity: FragmentActivity,
    onArm: (ConfigurationId, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledConfigs = remember(configurations) { configurations.filter { it.isEnabled } }
    var selectedConfigId by remember(enabledConfigs) {
        mutableStateOf(enabledConfigs.firstOrNull()?.id)
    }
    var selectedDurationSeconds by remember { mutableIntStateOf(durationPresets.first().seconds) }
    var configMenuExpanded by remember { mutableStateOf(false) }

    val timedSessionActive =
        activeSession?.origin == SessionOrigin.TIMED && activeSession.isActive
    val anotherSessionActive =
        activeSession != null && activeSession.isActive && activeSession.origin != SessionOrigin.TIMED
    val canConfigure =
        enabledConfigs.isNotEmpty() && !timedSessionActive && !anotherSessionActive
    val canArm =
        canConfigure &&
            masterEnabled &&
            hasSmsPermission &&
            DeviceCredentialGate.hasDeviceCredential(hostActivity)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.timed_forwarding_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.timed_forwarding_subtitle),
                style = MaterialTheme.typography.bodySmall,
            )

            when {
                enabledConfigs.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.timed_forwarding_no_config),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                timedSessionActive -> {
                    Text(
                        text = stringResource(R.string.timed_forwarding_active_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                anotherSessionActive -> {
                    Text(
                        text = stringResource(R.string.timed_forwarding_blocked_active_session),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    val selectedConfig =
                        enabledConfigs.firstOrNull { it.id == selectedConfigId } ?: enabledConfigs.first()
                    ExposedDropdownMenuBox(
                        expanded = configMenuExpanded,
                        onExpandedChange = { configMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedConfig.label,
                            onValueChange = {},
                            readOnly = true,
                            enabled = canConfigure,
                            label = { Text(stringResource(R.string.timed_forwarding_config_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = configMenuExpanded) },
                            modifier =
                                Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = configMenuExpanded,
                            onDismissRequest = { configMenuExpanded = false },
                        ) {
                            enabledConfigs.forEach { config ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${config.label} · ${UiUtils.maskPhoneNumber(config.requester.e164)}")
                                    },
                                    onClick = {
                                        selectedConfigId = config.id
                                        configMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.timed_forwarding_duration_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        durationPresets.forEach { preset ->
                            FilterChip(
                                selected = selectedDurationSeconds == preset.seconds,
                                onClick = { selectedDurationSeconds = preset.seconds },
                                label = { Text(preset.label) },
                                enabled = canConfigure,
                            )
                        }
                    }

                    val credentialTitle = stringResource(R.string.timed_forwarding_credential_title)
                    val credentialSubtitle = stringResource(R.string.timed_forwarding_credential_subtitle)
                    Button(
                        onClick = {
                            val configId = selectedConfigId ?: return@Button
                            DeviceCredentialGate.authenticate(
                                activity = hostActivity,
                                title = credentialTitle,
                                subtitle = credentialSubtitle,
                                onSuccess = {
                                    onArm(configId, selectedDurationSeconds)
                                },
                                onFailure = {},
                            )
                        },
                        enabled = canArm,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.timed_forwarding_arm_button))
                    }

                    if (!hasSmsPermission) {
                        Text(
                            text = stringResource(R.string.timed_forwarding_sms_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (!masterEnabled) {
                        Text(
                            text = stringResource(R.string.timed_forwarding_master_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (!DeviceCredentialGate.hasDeviceCredential(hostActivity)) {
                        Text(
                            text = stringResource(R.string.timed_forwarding_no_credential),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
