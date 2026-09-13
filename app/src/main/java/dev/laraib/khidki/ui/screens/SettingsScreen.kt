package dev.laraib.khidki.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.BuildConfig
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.filter.Re2RuleMatcher
import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.RuleMatchOutcome
import dev.laraib.khidki.ui.KhidkiUiState
import dev.laraib.khidki.ui.KhidkiViewModel
import dev.laraib.khidki.ui.theme.StatusTone
import dev.laraib.khidki.ui.theme.statusToneColors

@Composable
fun SettingsScreen(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    onOpenAppInfo: () -> Unit,
    onUnlockAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PermissionsCard(
            hasSmsPermission = state.hasSmsPermission,
            hasNotificationPermission = state.hasNotificationPermission,
            onOpenAppInfo = onOpenAppInfo,
        )
        PrivacyCard()
        SecurityCard()
        AboutCard(
            advancedUnlocked = state.advancedUnlocked,
            onUnlockAdvanced = onUnlockAdvanced,
        )
        AnimatedVisibility(visible = state.advancedUnlocked) {
            RegexTesterCard()
        }
    }
}

@Composable
private fun PermissionsCard(
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    onOpenAppInfo: () -> Unit,
) {
    val (smsAccent, _) = statusToneColors(if (hasSmsPermission) StatusTone.Success else StatusTone.Error)
    val (notifAccent, _) = statusToneColors(if (hasNotificationPermission) StatusTone.Success else StatusTone.Warning)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_permissions_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    if (hasSmsPermission) {
                        stringResource(R.string.settings_sms_granted)
                    } else {
                        stringResource(R.string.settings_sms_denied)
                    },
                color = smsAccent,
            )
            Text(
                text =
                    if (hasNotificationPermission) {
                        stringResource(R.string.settings_notifications_granted)
                    } else {
                        stringResource(R.string.settings_notifications_denied)
                    },
                color = notifAccent,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!hasSmsPermission) {
                Button(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.open_app_info))
                }
            }
            Text(
                text = stringResource(R.string.settings_realme_tips),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PrivacyCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_privacy_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_privacy_body),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.settings_privacy_risk),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecurityCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_security_title),
                style = MaterialTheme.typography.titleMedium,
            )
            securityLine(stringResource(R.string.settings_security_no_internet))
            securityLine(stringResource(R.string.settings_security_local))
            securityLine(stringResource(R.string.settings_security_window))
        }
    }
}

@Composable
private fun securityLine(text: String) {
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
        if (!valid) null else matcher.matches(rules, sampleSender, sampleBody, emptySet())
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_regex_tester_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_regex_tester_hint),
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
                label = { Text("Sender pattern") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = contentPattern,
                onValueChange = { contentPattern = it },
                label = { Text("Content pattern") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            when {
                !valid -> ResultBanner("Invalid pattern", StatusTone.Error)
                matchResult?.outcome == RuleMatchOutcome.MATCHED ->
                    ResultBanner("Would forward this message", StatusTone.Success)
                else -> ResultBanner("Would not forward", StatusTone.Warning)
            }
        }
    }
}

@Composable
private fun ResultBanner(text: String, tone: StatusTone) {
    val (foreground, background) = statusToneColors(tone)
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
private fun AboutCard(
    advancedUnlocked: Boolean,
    onUnlockAdvanced: () -> Unit,
) {
    var versionTapCount by remember { mutableIntStateOf(0) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                modifier =
                    Modifier.clickable {
                        if (advancedUnlocked) return@clickable
                        versionTapCount += 1
                        if (versionTapCount >= 3) {
                            onUnlockAdvanced()
                        }
                    },
            )
            Text(
                text = stringResource(R.string.settings_about_build, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodySmall,
            )
            if (!advancedUnlocked) {
                Text(
                    text = stringResource(R.string.settings_advanced_unlock_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.settings_about_tagline),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.settings_about_safety),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
