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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.model.CredentialPolicy
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
import dev.laraib.khidki.ui.theme.StatusAmber
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
    onCommandRevealed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSmsPermission = state.hasSmsPermission
    var showAddSheet by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<Configuration?>(null) }
    var pendingDelete by remember { mutableStateOf<Configuration?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.rules_title, state.configurations.size),
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
                        text = stringResource(R.string.rules_empty),
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
                            onEdit = { editingConfig = config },
                            onDelete = { pendingDelete = config },
                            onEnabledChange = { enabled ->
                                viewModel.setConfigurationEnabled(config.id, enabled, hasSmsPermission)
                            },
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

    editingConfig?.let { config ->
        EditRuleBottomSheet(
            config = config,
            errorMessage = state.errorMessage,
            hasSmsPermission = hasSmsPermission,
            onDismiss = { editingConfig = null },
            onSave = { label, requester, sender, content, windowSeconds ->
                viewModel.updateConfiguration(
                    id = config.id,
                    label = label,
                    requesterRaw = requester,
                    senderPattern = sender,
                    contentPattern = content,
                    windowSeconds = windowSeconds,
                    hasSmsPermission = hasSmsPermission,
                    onCommand = { command ->
                        editingConfig = null
                        onCommandRevealed(command)
                    },
                )
            },
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigRuleCard(
    config: Configuration,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
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
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = onEnabledChange,
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit rule")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete rule")
                    }
                }
            }
            Text(
                text = UiUtils.maskPhoneNumber(config.requester.e164),
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                config.filterRules.senderPatterns.forEach { pattern ->
                    AssistChip(
                        onClick = {},
                        label = { Text(truncatePattern(pattern)) },
                        enabled = false,
                    )
                }
                config.filterRules.contentPatterns.forEach { pattern ->
                    AssistChip(
                        onClick = {},
                        label = { Text(truncatePattern(pattern)) },
                        enabled = false,
                    )
                }
            }
            Text(
                text = "${config.windowSeconds / 60} min window",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun truncatePattern(pattern: String): String =
    if (pattern.length <= 24) pattern else pattern.take(21) + "..."

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditRuleBottomSheet(
    config: Configuration,
    errorMessage: String?,
    hasSmsPermission: Boolean,
    onDismiss: () -> Unit,
    onSave: (label: String, requester: String, sender: String, content: String, windowSeconds: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var label by remember(config.id) { mutableStateOf(config.label) }
    var requester by remember(config.id) { mutableStateOf(config.requester.e164) }
    var sender by remember(config.id) { mutableStateOf(config.filterRules.senderPatterns.firstOrNull() ?: "") }
    var content by remember(config.id) { mutableStateOf(config.filterRules.contentPatterns.firstOrNull() ?: "") }
    var windowMinutes by remember(config.id) {
        mutableStateOf((config.windowSeconds / 60).coerceAtLeast(1).toString())
    }
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
    val windowSeconds = windowMinutes.toIntOrNull()?.times(60)
    val windowValid =
        windowSeconds != null &&
            windowSeconds in CredentialPolicy.MIN_WINDOW_SECONDS..CredentialPolicy.MAX_WINDOW_SECONDS
    val requesterChanged = remember(requester, config.requester.e164) {
        phoneNormalizer.normalize(requester).let { result ->
            result is PhoneNormalizeResult.Success && result.phone.e164 != config.requester.e164
        }
    }
    val canSave =
        label.isNotBlank() &&
            requester.isNotBlank() &&
            phoneValid &&
            patternError == null &&
            windowValid

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
            Text(stringResource(R.string.edit_rule_title), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = requester,
                onValueChange = { requester = it },
                label = { Text("Requester phone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                supportingText = {
                    if (requesterChanged) {
                        Text(
                            "Changing the number voids credentials and generates a new access code",
                            color = StatusAmber,
                        )
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
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content filter (regex)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = windowMinutes,
                onValueChange = { windowMinutes = it.filter { ch -> ch.isDigit() } },
                label = { Text("Req window (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = {
                    Text(
                        "Used for req SMS ack window (${CredentialPolicy.MIN_WINDOW_SECONDS / 60}–" +
                            "${CredentialPolicy.MAX_WINDOW_SECONDS / 60} min)",
                    )
                },
            )
            ValidationBanner(patternError = patternError, atLimit = false)
            if (!windowValid) {
                Text(
                    "Window must be ${CredentialPolicy.MIN_WINDOW_SECONDS / 60}–" +
                        "${CredentialPolicy.MAX_WINDOW_SECONDS / 60} minutes",
                    color = StatusRed,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(
                onClick = {
                    val seconds = windowSeconds ?: return@TextButton
                    onSave(label, requester, sender, content, seconds)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (requesterChanged) {
                        stringResource(R.string.edit_rule_save_regenerate)
                    } else {
                        stringResource(R.string.edit_rule_save)
                    },
                )
            }
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
        patternError == null

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
