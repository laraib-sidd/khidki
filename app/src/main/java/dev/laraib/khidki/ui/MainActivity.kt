package dev.laraib.khidki.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.HistoryEvent

class MainActivity : ComponentActivity() {
    private val viewModel: KhidkiViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            viewModel.refresh(hasSmsPermission())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasSmsPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                ),
            )
        }
        viewModel.refresh(hasSmsPermission())
        setContent {
            KhidkiTheme {
                KhidkiAppScreen(
                    viewModel = viewModel,
                    hasSmsPermission = hasSmsPermission(),
                    onRequestPermissions = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.SEND_SMS,
                            ),
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh(hasSmsPermission())
    }

    private fun hasSmsPermission(): Boolean {
        val receive = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
        val send = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
        return receive == PackageManager.PERMISSION_GRANTED && send == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
private fun KhidkiTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KhidkiAppScreen(
    viewModel: KhidkiViewModel,
    hasSmsPermission: Boolean,
    onRequestPermissions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var revealedCommand by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Khidki") }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Status") },
                    label = { Text("Status") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = "Configs") },
                    label = { Text("Configs") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (!hasSmsPermission) {
                Text("SMS permission required. On sideloaded installs, enable Allow restricted settings first.")
                Button(onClick = onRequestPermissions) { Text("Grant SMS") }
                Spacer(Modifier.height(8.dp))
            }
            when (tab) {
                0 -> StatusTab(state, viewModel, hasSmsPermission)
                1 -> ConfigTab(state, viewModel, hasSmsPermission) { revealedCommand = it }
                2 -> HistoryTab(state, viewModel, hasSmsPermission)
                3 -> SettingsTab(state, viewModel, hasSmsPermission)
            }
            revealedCommand?.let { cmd ->
                Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Command (copy now — not shown again)")
                        Text(cmd, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { revealedCommand = null }) { Text("Dismiss") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTab(state: KhidkiUiState, viewModel: KhidkiViewModel, hasSms: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Master")
            Switch(
                checked = state.masterEnabled,
                onCheckedChange = { viewModel.setMasterEnabled(it, hasSms) },
            )
        }
        Text("State: ${state.appStateLabel}")
        state.activeSession?.let { session ->
            Text("Active: ${session.label}")
            Text("Expires: ${session.expiresAtMillis}")
            Text("Window: ${session.windowSeconds}s")
        } ?: Text("No active window")
        if (state.errorMessage != null) {
            Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ConfigTab(
    state: KhidkiUiState,
    viewModel: KhidkiViewModel,
    hasSms: Boolean,
    onCommand: (String) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var requester by remember { mutableStateOf("") }
    var sender by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("OTP") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("New configuration")
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
        OutlinedTextField(value = requester, onValueChange = { requester = it }, label = { Text("Requester (+91…)") })
        OutlinedTextField(value = sender, onValueChange = { sender = it }, label = { Text("Sender filter (regex)") })
        OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content filter (regex)") })
        Button(
            onClick = {
                viewModel.saveConfiguration(label, requester, sender, content, hasSms, onCommand)
            },
        ) { Text("Save & generate command") }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.configurations) { config: Configuration ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(config.label, style = MaterialTheme.typography.titleMedium)
                        Text("Requester: ${maskPhone(config.requester.e164)}")
                        Text("Senders: ${config.filterRules.senderPatterns.joinToString()}")
                        Text("Content: ${config.filterRules.contentPatterns.joinToString()}")
                        Button(onClick = { viewModel.deleteConfiguration(config.id, hasSms) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(state: KhidkiUiState, viewModel: KhidkiViewModel, hasSms: Boolean) {
    Column {
        Button(onClick = { viewModel.clearHistory(hasSms) }) { Text("Clear history") }
        LazyColumn {
            items(state.history) { event: HistoryEvent ->
                Text("${event.eventType.name} @ ${event.timestampMillis}")
            }
        }
    }
}

@Composable
private fun SettingsTab(state: KhidkiUiState, viewModel: KhidkiViewModel, hasSms: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Personal APK. Not on Play.")
        Text("Install from GitHub Releases. Allow restricted settings for SMS.")
        Text("Realme: enable Auto-start and background activity for Khidki.")
        Text("Never use live bank OTPs in tests.")
        Text("Password: 8 digits, reusable until expiry.")
        Text("Configs: ${state.configurations.size} / 20")
    }
}

private fun maskPhone(e164: String): String {
    if (e164.length < 6) return "••••"
    return e164.take(3) + "•••" + e164.takeLast(2)
}
