package dev.laraib.khidki.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.domain.filter.Re2RuleMatcher
import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.RuleMatchOutcome
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.theme.StatusGreen
import dev.laraib.khidki.ui.theme.StatusGreenBg
import dev.laraib.khidki.ui.theme.StatusRed
import dev.laraib.khidki.ui.theme.StatusRedBg

@Composable
fun SettingsScreen(
    state: KhidkiUiState,
    hasSmsPermission: Boolean,
    onOpenAppInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DeviceHealthCard(
            hasSmsPermission = hasSmsPermission,
            onOpenAppInfo = onOpenAppInfo,
        )
        SecurityInvariantsCard(configCount = state.configurations.size)
        RegexTesterCard()
        AboutCard()
    }
}

@Composable
private fun DeviceHealthCard(
    hasSmsPermission: Boolean,
    onOpenAppInfo: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Device & permissions", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (hasSmsPermission) {
                    "SMS permission: Granted"
                } else {
                    "SMS permission: Restricted / denied"
                },
                color = if (hasSmsPermission) StatusGreen else StatusRed,
            )
            if (!hasSmsPermission) {
                Button(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) {
                    Text("Open app info")
                }
            }
            Text(
                text = "Realme GT 6T: enable Auto-start and allow background activity for Khidki " +
                    "so SMS forwarding survives battery optimization.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SecurityInvariantsCard(configCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Security & privacy", style = MaterialTheme.typography.titleMedium)
            invariantLine("Zero Internet: no NETWORK permission in manifest")
            invariantLine("Access code: 8-digit CSPRNG, expires with window")
            invariantLine("Lockout: 5 failed attempts per requester")
            invariantLine("Budget: max 20 SMS parts forwarded per window")
            invariantLine("Storage: local Room DB + hardware-backed Keystore")
            invariantLine("Forwarding rules: $configCount / 20 configured")
        }
    }
}

@Composable
private fun invariantLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun RegexTesterCard() {
    var sampleSender by remember { mutableStateOf("VK-HDFCBK") }
    var sampleBody by remember { mutableStateOf("Your OTP is 928371 for login") }
    var senderPattern by remember { mutableStateOf("^(VK|VM|AD|QP)-.*") }
    var contentPattern by remember { mutableStateOf("(?i)otp|\\b\\d{4,8}\\b") }
    val matcher = remember { Re2RuleMatcher() }
    val rules = remember(senderPattern, contentPattern) {
        FilterRules(
            senderPatterns = listOf(senderPattern),
            contentPatterns = listOf(contentPattern),
        )
    }
    val valid = remember(rules) { matcher.isValid(rules) }
    val matchResult = remember(rules, sampleSender, sampleBody, valid) {
        if (!valid) {
            null
        } else {
            matcher.matches(rules, sampleSender, sampleBody, emptySet())
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Live regex tester", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Paste a sample SMS and test your filters before creating a rule.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = sampleSender,
                onValueChange = { sampleSender = it },
                label = { Text("Sample sender") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = sampleBody,
                onValueChange = { sampleBody = it },
                label = { Text("Sample body") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = senderPattern,
                onValueChange = { senderPattern = it },
                label = { Text("Sender regex") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = contentPattern,
                onValueChange = { contentPattern = it },
                label = { Text("Content regex") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            when {
                !valid -> ResultBanner("Invalid regex pattern", StatusRedBg, StatusRed)
                matchResult?.outcome == RuleMatchOutcome.MATCHED ->
                    ResultBanner("MATCH — would forward", StatusGreenBg, StatusGreen)
                else -> ResultBanner("NO MATCH — would drop", StatusRedBg, StatusRed)
            }
        }
    }
}

@Composable
private fun ResultBanner(text: String, background: androidx.compose.ui.graphics.Color, foreground: androidx.compose.ui.graphics.Color) {
    Card(colors = CardDefaults.cardColors(containerColor = background)) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = foreground,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AboutCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text("Khidki v1.0.0-preview", style = MaterialTheme.typography.bodyMedium)
            Text("Personal sideload APK — not on Play Store.", style = MaterialTheme.typography.bodySmall)
            Text("Install from GitHub Releases preview channel.", style = MaterialTheme.typography.bodySmall)
            Text("Never use live bank OTPs in tests.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
