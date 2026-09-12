package dev.laraib.khidki.ui.screens

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
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.filter.ForwardingPolicy
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.KhidkiViewModel
import dev.laraib.khidki.ui.components.ScreenHeader
import dev.laraib.khidki.ui.theme.StatusTone
import dev.laraib.khidki.ui.theme.statusToneColors

@Composable
fun ConfigsScreen(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    modifier: Modifier = Modifier,
) {
    val hasSmsPermission = state.hasSmsPermission
    var showAddSender by remember { mutableStateOf(false) }
    val policy = state.forwardingPolicy

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.rules_what_to_forward),
                subtitle = stringResource(
                    R.string.rules_summary,
                    policy.enabledCategoryCount(),
                    policy.customSenders.size,
                ),
            )
            if (!policy.hasAnyEnabled()) {
                val (accent, background) = statusToneColors(StatusTone.Warning)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = background),
                ) {
                    Text(
                        text = stringResource(R.string.rules_none_enabled),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                    )
                }
            }
            CategorySection(title = stringResource(R.string.rules_otp_section)) {
                CategoryToggle(
                    label = stringResource(R.string.rules_otp_shopping),
                    checked = policy.otpShopping,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCategory({ p -> p.copy(otpShopping = enabled) }, hasSmsPermission)
                    },
                )
                CategoryToggle(
                    label = stringResource(R.string.rules_otp_banks),
                    checked = policy.otpBanks,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCategory({ p -> p.copy(otpBanks = enabled) }, hasSmsPermission)
                    },
                )
                CategoryToggle(
                    label = stringResource(R.string.rules_otp_upi),
                    checked = policy.otpUpi,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCategory({ p -> p.copy(otpUpi = enabled) }, hasSmsPermission)
                    },
                )
                CategoryToggle(
                    label = stringResource(R.string.rules_otp_government),
                    checked = policy.otpGovernment,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCategory({ p -> p.copy(otpGovernment = enabled) }, hasSmsPermission)
                    },
                )
                CategoryToggle(
                    label = stringResource(R.string.rules_otp_other),
                    checked = policy.otpOther,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCategory({ p -> p.copy(otpOther = enabled) }, hasSmsPermission)
                    },
                )
            }
            CategorySection(title = stringResource(R.string.rules_alerts_section)) {
                CategoryToggle(
                    label = stringResource(R.string.rules_alert_bank),
                    checked = policy.alertBank,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCategory({ p -> p.copy(alertBank = enabled) }, hasSmsPermission)
                    },
                )
                CategoryToggle(
                    label = stringResource(R.string.rules_alert_order),
                    checked = policy.alertOrder,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCategory({ p -> p.copy(alertOrder = enabled) }, hasSmsPermission)
                    },
                )
            }
            CategorySection(title = stringResource(R.string.rules_catchall_section)) {
                CategoryToggle(
                    label = stringResource(R.string.rules_all_sms),
                    checked = policy.allSms,
                    onCheckedChange = { enabled ->
                        viewModel.toggleCategory({ p -> p.copy(allSms = enabled) }, hasSmsPermission)
                    },
                )
                if (policy.customSenders.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rules_custom_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                } else {
                    policy.customSenders.forEachIndexed { index, sender ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = sender.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = stringResource(
                                        R.string.rules_custom_sender_hint,
                                        sender.senderContains,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { viewModel.removeCustomSender(index, hasSmsPermission) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.rules_remove_sender))
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddSender = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.rules_add_sender))
        }
    }

    if (showAddSender) {
        AddSenderSheet(
            onDismiss = { showAddSender = false },
            onSave = { label, contains, codesOnly ->
                viewModel.addCustomSender(label, contains, codesOnly, hasSmsPermission)
                showAddSender = false
            },
        )
    }
}

@Composable
private fun CategorySection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            content()
        }
    }
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
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSenderSheet(
    onDismiss: () -> Unit,
    onSave: (label: String, senderContains: String, codesOnly: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var label by remember { mutableStateOf("") }
    var senderContains by remember { mutableStateOf("") }
    var codesOnly by remember { mutableStateOf(true) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.rules_add_sender_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.rules_sender_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = senderContains,
                onValueChange = { senderContains = it },
                label = { Text(stringResource(R.string.rules_sender_contains)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
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
                onClick = { onSave(label, senderContains, codesOnly) },
                modifier = Modifier.fillMaxWidth(),
                enabled = label.isNotBlank() && senderContains.isNotBlank(),
            ) {
                Text(stringResource(R.string.rules_save_sender))
            }
        }
    }
}
