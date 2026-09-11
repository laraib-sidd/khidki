package dev.laraib.khidki.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.laraib.khidki.platform.permission.PermissionGate
import dev.laraib.khidki.ui.components.CommandRevealDialog
import dev.laraib.khidki.ui.components.PermissionPreflightCard
import dev.laraib.khidki.ui.screens.ConfigsScreen
import dev.laraib.khidki.ui.screens.HistoryScreen
import dev.laraib.khidki.ui.screens.SettingsScreen
import dev.laraib.khidki.ui.screens.StatusScreen
import dev.laraib.khidki.ui.theme.KhidkiTheme

class MainActivity : AppCompatActivity() {
    private val viewModel: KhidkiViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            refreshPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        setContent {
            KhidkiTheme {
                KhidkiAppScreen(
                    viewModel = viewModel,
                    hostActivity = this,
                    onOpenAppInfo = { openAppInfo() },
                    onRequestPermissions = { requestSmsPermissions() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        viewModel.refresh(PermissionGate.hasSmsPermissions(this))
    }

    private fun openAppInfo() {
        startActivity(PermissionGate.createAppDetailsIntent(packageName))
    }

    private fun requestSmsPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KhidkiAppScreen(
    viewModel: KhidkiViewModel,
    hostActivity: AppCompatActivity,
    onOpenAppInfo: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val diagnosticEvents by viewModel.diagnosticEvents.collectAsState()
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!state.hasSmsPermission) {
                PermissionPreflightCard(
                    steps = PermissionGate.sideloadSetupSteps(),
                    onOpenAppInfo = onOpenAppInfo,
                    onRequestPermissions = onRequestPermissions,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> StatusScreen(
                        state = state,
                        viewModel = viewModel,
                        hostActivity = hostActivity,
                        diagnosticEvents = diagnosticEvents,
                    )
                    1 -> ConfigsScreen(
                        state = state,
                        viewModel = viewModel,
                        onCommandRevealed = { revealedCommand = it },
                    )
                    2 -> HistoryScreen(
                        state = state,
                        viewModel = viewModel,
                    )
                    3 -> SettingsScreen(
                        state = state,
                        onOpenAppInfo = onOpenAppInfo,
                    )
                }
            }
        }
    }

    revealedCommand?.let { command ->
        CommandRevealDialog(
            command = command,
            onDismiss = { revealedCommand = null },
        )
    }
}
