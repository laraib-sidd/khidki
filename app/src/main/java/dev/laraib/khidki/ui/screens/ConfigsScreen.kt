package dev.laraib.khidki.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.filter.ForwardingPolicy
import dev.laraib.khidki.domain.model.DestinationProfile
import dev.laraib.khidki.domain.model.DestinationProfiles
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.KhidkiViewModel
import dev.laraib.khidki.ui.components.ConfirmDeleteDialog
import dev.laraib.khidki.ui.components.ScreenHeader
import dev.laraib.khidki.ui.components.UiUtils
import dev.laraib.khidki.ui.theme.StatusTone
import dev.laraib.khidki.ui.theme.statusToneColors

@Composable
fun ConfigsScreen(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    modifier: Modifier = Modifier,
) {
    val hasSmsPermission = state.hasSmsPermission
    var showAddPerson by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<DestinationProfile?>(null) }
    var pendingDelete by remember { mutableStateOf<DestinationProfile?>(null) }
    val enabledCount = DestinationProfiles.enabledWithPolicy(state.destinations).size

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.rules_people_title),
                subtitle = stringResource(R.string.rules_people_summary, state.destinations.size, enabledCount),
            )
            if (state.destinations.isEmpty()) {
                val (accent, background) = statusToneColors(StatusTone.Warning)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = background),
                ) {
                    Text(
                        text = stringResource(R.string.rules_people_empty),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                    )
                }
            } else {
                state.destinations.forEach { profile ->
                    PersonCard(
                        profile = profile,
                        onClick = { editingProfile = profile },
                        onToggleEnabled = { enabled ->
                            viewModel.toggleDestinationEnabled(profile.id, enabled, hasSmsPermission)
                        },
                        onDelete = { pendingDelete = profile },
                    )
                }
            }
        }
        if (state.destinations.size < DestinationProfile.MAX_DESTINATIONS) {
            FloatingActionButton(
                onClick = { showAddPerson = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.rules_add_person))
            }
        }
    }

    if (showAddPerson) {
        PersonEditSheet(
            title = stringResource(R.string.rules_add_person_title),
            initialName = "",
            initialPhone = "",
            initialPolicy = ForwardingPolicy.defaultFirstRun(),
            initialEnabled = true,
            onDismiss = { showAddPerson = false },
            onSave = { name, phone, policy, enabled ->
                viewModel.addDestination(name, phone, policy, hasSmsPermission)
                showAddPerson = false
            },
        )
    }

    editingProfile?.let { profile ->
        PersonEditSheet(
            title = stringResource(R.string.rules_edit_person_title, profile.name),
            initialName = profile.name,
            initialPhone = profile.phoneE164,
            initialPolicy = profile.policy,
            initialEnabled = profile.enabled,
            onDismiss = { editingProfile = null },
            onSave = { name, phone, policy, enabled ->
                val latestPolicy =
                    state.destinations.find { it.id == profile.id }?.policy ?: policy
                viewModel.updateDestination(
                    profile.id,
                    name,
                    phone,
                    latestPolicy,
                    enabled,
                    hasSmsPermission,
                )
                editingProfile = null
            },
            onAddCustomSender = { label, contains, codesOnly ->
                viewModel.addCustomSenderToDestination(profile.id, label, contains, codesOnly, hasSmsPermission)
            },
            onRemoveCustomSender = { index ->
                viewModel.removeCustomSenderFromDestination(profile.id, index, hasSmsPermission)
            },
        )
    }

    pendingDelete?.let { profile ->
        ConfirmDeleteDialog(
            ruleLabel = profile.name,
            onConfirm = {
                viewModel.removeDestination(profile.id, hasSmsPermission)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun PersonCard(
    profile: DestinationProfile,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = UiUtils.maskPhoneNumber(profile.phoneE164),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        stringResource(
                            R.string.rules_person_filters,
                            profile.policy.enabledCategoryCount(),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = profile.enabled, onCheckedChange = onToggleEnabled)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.rules_remove_person))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonEditSheet(
    title: String,
    initialName: String,
    initialPhone: String,
    initialPolicy: ForwardingPolicy,
    initialEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, policy: ForwardingPolicy, enabled: Boolean) -> Unit,
    onAddCustomSender: ((label: String, senderContains: String, codesOnly: Boolean) -> Unit)? = null,
    onRemoveCustomSender: ((index: Int) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf(initialPhone) }
    var policy by remember { mutableStateOf(initialPolicy) }
    var enabled by remember { mutableStateOf(initialEnabled) }
    var customLabel by remember { mutableStateOf("") }
    var customSender by remember { mutableStateOf("") }
    var codesOnly by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.rules_person_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.rules_person_phone)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.rules_person_enabled))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            Text(
                text = stringResource(R.string.rules_filters_for_person),
                style = MaterialTheme.typography.titleSmall,
            )
            PolicySection(
                policy = policy,
                onPolicyChange = { policy = it },
            )
            if (onAddCustomSender != null && onRemoveCustomSender != null) {
                Text(
                    text = stringResource(R.string.rules_custom_senders),
                    style = MaterialTheme.typography.titleSmall,
                )
                policy.customSenders.forEachIndexed { index, sender ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = sender.label)
                            Text(
                                text = sender.senderContains,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onRemoveCustomSender(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
                OutlinedTextField(
                    value = customLabel,
                    onValueChange = { customLabel = it },
                    label = { Text(stringResource(R.string.rules_sender_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = customSender,
                    onValueChange = { customSender = it },
                    label = { Text(stringResource(R.string.rules_sender_contains)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.rules_codes_only))
                    Switch(checked = codesOnly, onCheckedChange = { codesOnly = it })
                }
                TextButton(
                    onClick = {
                        onAddCustomSender(customLabel, customSender, codesOnly)
                        customLabel = ""
                        customSender = ""
                    },
                    enabled = customLabel.isNotBlank() && customSender.isNotBlank(),
                ) {
                    Text(stringResource(R.string.rules_save_sender))
                }
            }
            TextButton(
                onClick = { onSave(name, phone, policy, enabled) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && phone.isNotBlank() && policy.hasAnyEnabled(),
            ) {
                Text(stringResource(R.string.rules_save_person))
            }
        }
    }
}

@Composable
private fun PolicySection(
    policy: ForwardingPolicy,
    onPolicyChange: (ForwardingPolicy) -> Unit,
) {
    CategoryToggle(
        label = stringResource(R.string.rules_all_sms),
        checked = policy.allSms,
        onCheckedChange = { onPolicyChange(policy.copy(allSms = it)) },
    )
    CategoryToggle(
        label = stringResource(R.string.rules_otp_shopping),
        checked = policy.otpShopping,
        onCheckedChange = { onPolicyChange(policy.copy(otpShopping = it)) },
    )
    CategoryToggle(
        label = stringResource(R.string.rules_otp_banks),
        checked = policy.otpBanks,
        onCheckedChange = { onPolicyChange(policy.copy(otpBanks = it)) },
    )
    CategoryToggle(
        label = stringResource(R.string.rules_otp_upi),
        checked = policy.otpUpi,
        onCheckedChange = { onPolicyChange(policy.copy(otpUpi = it)) },
    )
    CategoryToggle(
        label = stringResource(R.string.rules_otp_government),
        checked = policy.otpGovernment,
        onCheckedChange = { onPolicyChange(policy.copy(otpGovernment = it)) },
    )
    CategoryToggle(
        label = stringResource(R.string.rules_otp_other),
        checked = policy.otpOther,
        onCheckedChange = { onPolicyChange(policy.copy(otpOther = it)) },
    )
    CategoryToggle(
        label = stringResource(R.string.rules_alert_bank),
        checked = policy.alertBank,
        onCheckedChange = { onPolicyChange(policy.copy(alertBank = it)) },
    )
    CategoryToggle(
        label = stringResource(R.string.rules_alert_order),
        checked = policy.alertOrder,
        onCheckedChange = { onPolicyChange(policy.copy(alertOrder = it)) },
    )
}

@Composable
private fun CategoryToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
