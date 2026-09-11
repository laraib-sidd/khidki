package dev.laraib.khidki.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.PhoneNormalizeResult
import dev.laraib.khidki.domain.phone.PhoneNormalizer
import dev.laraib.khidki.ui.ConfigurationValidator
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.KhidkiViewModel
import dev.laraib.khidki.ui.components.ConfirmDeleteDialog
import dev.laraib.khidki.ui.components.UiUtils
import dev.laraib.khidki.ui.theme.StatusGreen
import dev.laraib.khidki.ui.theme.StatusGreenBg
import dev.laraib.khidki.ui.theme.StatusRed
import dev.laraib.khidki.ui.theme.StatusRedBg

private data class RegexPreset(val label: String, val pattern: String)

private val senderPresets = listOf(
    RegexPreset("All banks", "^(VK|VM|AD|QP)-.*"),
    RegexPreset("HDFC", "HDFC"),
    RegexPreset("SBI", "SBI"),
    RegexPreset("ICICI", "ICICI"),
    RegexPreset("Any", ".*"),
)

private val contentPresets = listOf(
    RegexPreset("OTP only", "(?i)otp|\\b\\d{4,8}\\b"),
    RegexPreset("Transactions", "(?i)debited|credited|txn"),
    RegexPreset("All messages", ".*"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigsScreen(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    hasSmsPermission: Boolean,
    onCommandRevealed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Configuration?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = "Forwarding rules (${state.configurations.size}/20)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            if (state.configurations.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Text(
                        text = "No rules configured. Tap + to add a forwarding rule.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 88.dp,
                    ),
                ) {
                    items(state.configurations, key = { it.id.uuid }) { config ->
                        ConfigRuleCard(
                            config = config,
                            onDelete = { pendingDelete = config },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add rule")
        }
    }

    if (showAddSheet) {
        AddRuleBottomSheet(
            configCount = state.configurations.size,
            errorMessage = state.errorMessage,
            hasSmsPermission = hasSmsPermission,
            onDismiss = { showAddSheet = false },
            onSave = { label, requester, sender, content ->
                viewModel.saveConfiguration(label, requester, sender, content, hasSmsPermission) { command ->
                    showAddSheet = false
                    onCommandRevealed(command)
                }
            },
        )
    }

    pendingDelete?.let { config ->
        ConfirmDeleteDialog(
            ruleLabel = config.label,
            onConfirm = {
                viewModel.deleteConfiguration(config.id, hasSmsPermission)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ConfigRuleCard(
    config: Configuration,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(config.label, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (config.enabled) "Active" else "Disabled",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (config.enabled) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete rule")
                    }
                }
            }
            Text(
                text = UiUtils.maskPhoneNumber(config.requester.e164),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Sender: ${config.filterRules.senderPatterns.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Content: ${config.filterRules.contentPatterns.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "${config.windowSeconds / 60} min window",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddRuleBottomSheet(
    configCount: Int,
    errorMessage: String?,
    hasSmsPermission: Boolean,
    onDismiss: () -> Unit,
    onSave: (label: String, requester: String, sender: String, content: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var label by remember { mutableStateOf("") }
    var requester by remember { mutableStateOf("") }
    var sender by remember { mutableStateOf("^(VK|VM|AD|QP)-.*") }
    var content by remember { mutableStateOf("(?i)otp|\\b\\d{4,8}\\b") }
    val phoneNormalizer = remember { PhoneNormalizer() }
    val phoneValid = remember(requester) {
        when (phoneNormalizer.normalize(requester)) {
            is PhoneNormalizeResult.Success -> true
            else -> requester.isBlank()
        }
    }
    val patternError = remember(sender, content) {
        ConfigurationValidator.validatePatterns(sender, content)
    }
    val atLimit = configCount >= 20
    val canSave = !atLimit &&
        label.isNotBlank() &&
        requester.isNotBlank() &&
        phoneValid &&
        patternError == null &&
        hasSmsPermission

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("New forwarding rule", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                placeholder = { Text("e.g. HDFC NetBanking") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = requester,
                onValueChange = { requester = it },
                label = { Text("Requester phone") },
                placeholder = { Text("+919876543210") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                supportingText = {
                    when {
                        requester.isBlank() -> Text("E.164 format required")
                        phoneValid -> Text("Valid phone number")
                        else -> Text("Invalid phone number", color = StatusRed)
                    }
                },
            )
            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("Sender filter (regex)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("Sender presets", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                senderPresets.forEach { preset ->
                    AssistChip(
                        onClick = { sender = preset.pattern },
                        label = { Text(preset.label) },
                    )
                }
            }
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content filter (regex)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("Content presets", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                contentPresets.forEach { preset ->
                    AssistChip(
                        onClick = { content = preset.pattern },
                        label = { Text(preset.label) },
                    )
                }
            }
            ValidationBanner(patternError = patternError, atLimit = atLimit)
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(
                onClick = { onSave(label, requester, sender, content) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save & generate access code")
            }
        }
    }
}

@Composable
private fun ValidationBanner(patternError: String?, atLimit: Boolean) {
    when {
        atLimit -> {
            Card(colors = CardDefaults.cardColors(containerColor = StatusRedBg)) {
                Text(
                    text = "Maximum 20 configurations allowed",
                    modifier = Modifier.padding(12.dp),
                    color = StatusRed,
                )
            }
        }
        patternError == null -> {
            Card(colors = CardDefaults.cardColors(containerColor = StatusGreenBg)) {
                Text(
                    text = "Regex patterns valid",
                    modifier = Modifier.padding(12.dp),
                    color = StatusGreen,
                )
            }
        }
        else -> {
            Card(colors = CardDefaults.cardColors(containerColor = StatusRedBg)) {
                Text(
                    text = patternError,
                    modifier = Modifier.padding(12.dp),
                    color = StatusRed,
                )
            }
        }
    }
}
