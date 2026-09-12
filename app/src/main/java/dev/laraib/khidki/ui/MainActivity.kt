package dev.laraib.khidki.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import dev.laraib.khidki.R
import dev.laraib.khidki.platform.permission.PermissionGate
import dev.laraib.khidki.ui.components.CommandRevealDialog
import dev.laraib.khidki.ui.components.PermissionPreflightCard
import dev.laraib.khidki.ui.components.WelcomeSheet
import dev.laraib.khidki.ui.screens.ConfigsScreen
import dev.laraib.khidki.ui.screens.HistoryScreen
import dev.laraib.khidki.ui.screens.SettingsScreen
import dev.laraib.khidki.ui.screens.StatusScreen
import dev.laraib.khidki.ui.theme.KhidkiTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: KhidkiViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            refreshPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        refreshPermissionState()
        setContent {
            KhidkiTheme {
                KhidkiAppScreen(
                    viewModel = viewModel,
                    hostActivity = this,
                    onOpenAppInfo = { openAppInfo() },
                    onRequestPermissions = { requestRuntimePermissions() },
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

    private fun requestRuntimePermissions() {
        val permissions =
            buildList {
                add(Manifest.permission.RECEIVE_SMS)
                add(Manifest.permission.SEND_SMS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KhidkiAppScreen(
    viewModel: KhidkiViewModel,
    hostActivity: FragmentActivity,
    onOpenAppInfo: () -> Unit,
    onRequestPermissions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val diagnosticEvents by viewModel.diagnosticEvents.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var revealedCommand by remember { mutableStateOf<String?>(null) }
    var showWelcome by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.welcomeCompleted) {
        showWelcome = !state.welcomeCompleted
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearErrorMessage()
            }
        }
    }

    LaunchedEffect(tab) {
        viewModel.clearErrorMessage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_khidki_mark),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(stringResource(R.string.app_name))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
                    label = { Text(stringResource(R.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.nav_configs)) },
                    label = { Text(stringResource(R.string.nav_configs)) },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = stringResource(R.string.nav_history)) },
                    label = { Text(stringResource(R.string.nav_history)) },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                    label = { Text(stringResource(R.string.nav_settings)) },
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
                    steps = PermissionGate.setupSteps(),
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
                        onAuthCancelled = {
                            scope.launch {
                                snackbarHostState.showSnackbar(hostActivity.getString(R.string.auth_cancelled))
                            }
                        },
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
                        onUnlockAdvanced = { viewModel.unlockAdvanced() },
                    )
                }
            }
        }
    }

    if (showWelcome) {
        WelcomeSheet(
            onComplete = {
                viewModel.completeWelcome()
                showWelcome = false
            },
            onDismiss = {
                viewModel.completeWelcome()
                showWelcome = false
            },
        )
    }

    revealedCommand?.let { command ->
        CommandRevealDialog(
            command = command,
            onDismiss = { revealedCommand = null },
        )
    }
}
