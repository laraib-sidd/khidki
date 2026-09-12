package dev.laraib.khidki.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.laraib.khidki.KhidkiRuntime
import dev.laraib.khidki.data.adapter.Blocking
import dev.laraib.khidki.domain.auth.CredentialGenerator
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.Configuration
import dev.laraib.khidki.domain.model.ConfigurationId
import dev.laraib.khidki.domain.model.ConfigurationVersion
import dev.laraib.khidki.domain.model.CredentialPolicy
import dev.laraib.khidki.domain.model.FilterRules
import dev.laraib.khidki.domain.model.HistoryEvent
import dev.laraib.khidki.domain.model.PhoneNormalizeResult
import dev.laraib.khidki.domain.model.TimedArmRejectReason
import dev.laraib.khidki.domain.model.TimedArmResult
import dev.laraib.khidki.domain.phone.PhoneNormalizer
import dev.laraib.khidki.platform.diagnostics.DiagnosticEventBus
import dev.laraib.khidki.platform.permission.PermissionGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class KhidkiUiState(
    val masterEnabled: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val appStateLabel: String = "—",
    val activeSession: AuthorizationSession? = null,
    val configurations: List<Configuration> = emptyList(),
    val history: List<HistoryEvent> = emptyList(),
    val errorMessage: String? = null,
    val welcomeCompleted: Boolean = false,
    val advancedUnlocked: Boolean = false,
)

class KhidkiViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime: KhidkiRuntime = KhidkiRuntime.get(application)
    private val container = runtime.container
    private val phoneNormalizer = PhoneNormalizer()
    private val credentialGenerator = CredentialGenerator()

    private val _uiState = MutableStateFlow(KhidkiUiState())
    val uiState: StateFlow<KhidkiUiState> = _uiState.asStateFlow()

    val diagnosticEvents: StateFlow<List<String>> = DiagnosticEventBus.events.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun refresh(hasSmsPermission: Boolean) {
        runtime.refreshAppState(hasSmsPermission)
        val hasNotifications =
            PermissionGate.hasNotificationPermission(getApplication())
        viewModelScope.launch {
            val configs = Blocking.io { container.configurationRepository.getAll() }
            val history = Blocking.io { container.auditStore.listRecent(100) }
            val session = Blocking.io {
                container.sessionRepository.getActiveSession(runtime.clock.nowMillis())
            }
            _uiState.value = _uiState.value.copy(
                masterEnabled = runtime.appPreferences.isMasterEnabled,
                hasSmsPermission = hasSmsPermission,
                hasNotificationPermission = hasNotifications,
                appStateLabel = runtime.engine.appState().name,
                activeSession = session,
                configurations = configs,
                history = history,
                welcomeCompleted = runtime.appPreferences.welcomeCompleted,
                advancedUnlocked = runtime.appPreferences.advancedUnlocked,
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun completeWelcome() {
        runtime.appPreferences.welcomeCompleted = true
        _uiState.value = _uiState.value.copy(welcomeCompleted = true)
    }

    fun unlockAdvanced() {
        runtime.appPreferences.advancedUnlocked = true
        _uiState.value = _uiState.value.copy(advancedUnlocked = true)
    }

    fun setConfigurationEnabled(
        id: ConfigurationId,
        enabled: Boolean,
        hasSmsPermission: Boolean,
    ) {
        viewModelScope.launch {
            val existing =
                _uiState.value.configurations.find { it.id == id }
                    ?: return@launch
            val updated =
                existing.copy(
                    enabled = enabled,
                    isEnabled = enabled,
                    updatedAtMillis = runtime.clock.nowMillis(),
                )
            Blocking.io { container.configurationRepository.upsert(updated) }
            refresh(hasSmsPermission)
        }
    }

    fun setMasterEnabled(enabled: Boolean, hasSmsPermission: Boolean) {
        runtime.appPreferences.isMasterEnabled = enabled
        runtime.refreshAppState(hasSmsPermission)
        refresh(hasSmsPermission)
    }

    fun cancelActiveSession(hasSmsPermission: Boolean) {
        val cancelled =
            runtime.engine.cancelTimedWindow() || runtime.engine.cancelActiveWindow()
        if (cancelled) {
            DiagnosticEventBus.record("UI cancelled active window")
        }
        refresh(hasSmsPermission)
    }

    fun armTimedWindow(
        configId: ConfigurationId,
        durationSeconds: Int,
        hasSmsPermission: Boolean,
    ) {
        viewModelScope.launch {
            when (val result = runtime.engine.armTimedWindow(configId, durationSeconds)) {
                is TimedArmResult.Success -> {
                    DiagnosticEventBus.record("UI armed timed window (${durationSeconds}s)")
                    refresh(hasSmsPermission)
                }
                is TimedArmResult.Rejected -> {
                    _uiState.value =
                        _uiState.value.copy(
                            errorMessage = timedArmErrorMessage(result.reason),
                        )
                }
            }
        }
    }

    fun updateConfiguration(
        id: ConfigurationId,
        label: String,
        requesterRaw: String,
        senderPattern: String,
        contentPattern: String,
        windowSeconds: Int,
        hasSmsPermission: Boolean,
        onCommand: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val existing =
                _uiState.value.configurations.find { it.id == id }
                    ?: run {
                        _uiState.value = _uiState.value.copy(errorMessage = "Configuration not found")
                        return@launch
                    }
            val patternError = ConfigurationValidator.validatePatterns(senderPattern, contentPattern)
            if (patternError != null) {
                _uiState.value = _uiState.value.copy(errorMessage = patternError)
                return@launch
            }
            if (windowSeconds !in CredentialPolicy.MIN_WINDOW_SECONDS..CredentialPolicy.MAX_WINDOW_SECONDS) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMessage =
                            "Window must be between ${CredentialPolicy.MIN_WINDOW_SECONDS} and " +
                                "${CredentialPolicy.MAX_WINDOW_SECONDS} seconds",
                    )
                return@launch
            }
            val requester =
                normalizePhone(requesterRaw)
                    ?: run {
                        _uiState.value = _uiState.value.copy(errorMessage = "Invalid requester number")
                        return@launch
                    }
            val requesterChanged = existing.requester.e164 != requester.e164
            val now = runtime.clock.nowMillis()
            val updated =
                existing.copy(
                    label = label.ifBlank { "Config" },
                    requester = requester,
                    filterRules =
                        FilterRules(
                            senderPatterns = listOf(senderPattern),
                            contentPatterns = listOf(contentPattern),
                        ),
                    rules =
                        FilterRules(
                            senderPatterns = listOf(senderPattern),
                            contentPatterns = listOf(contentPattern),
                        ),
                    credentialPolicy =
                        existing.credentialPolicy.copy(
                            windowSeconds = windowSeconds,
                        ),
                    windowSeconds = windowSeconds,
                    version = ConfigurationVersion(existing.version.n + 1),
                    updatedAtMillis = now,
                )
            Blocking.io { container.configurationRepository.upsert(updated) }
            if (requesterChanged) {
                Blocking.io { container.credentialStore.revokeForConfiguration(id) }
                val password = credentialGenerator.generate()
                val expiresAt = now + updated.credentialPolicy.lifetimeMs
                Blocking.io {
                    container.credentialStore.createCredential(
                        requester = requester,
                        configurationId = id,
                        password = password,
                        createdAtMillis = now,
                        expiresAtMillis = expiresAt,
                    )
                }
                onCommand("req $password")
            }
            refresh(hasSmsPermission)
        }
    }

    fun saveConfiguration(
        label: String,
        requesterRaw: String,
        senderPattern: String,
        contentPattern: String,
        hasSmsPermission: Boolean,
        onCommand: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val configCountError = ConfigurationValidator.validateConfigCount(_uiState.value.configurations.size)
            if (configCountError != null) {
                _uiState.value = _uiState.value.copy(errorMessage = configCountError)
                return@launch
            }
            val patternError = ConfigurationValidator.validatePatterns(senderPattern, contentPattern)
            if (patternError != null) {
                _uiState.value = _uiState.value.copy(errorMessage = patternError)
                return@launch
            }
            val requester = normalizePhone(requesterRaw)
                ?: run {
                    _uiState.value = _uiState.value.copy(errorMessage = "Invalid requester number")
                    return@launch
                }
            val now = runtime.clock.nowMillis()
            val config = Configuration(
                id = ConfigurationId(UUID.randomUUID()),
                version = ConfigurationVersion(1),
                label = label.ifBlank { "Config" },
                requester = requester,
                filterRules = FilterRules(
                    senderPatterns = listOf(senderPattern),
                    contentPatterns = listOf(contentPattern),
                ),
                credentialPolicy = CredentialPolicy(),
                windowSeconds = CredentialPolicy.DEFAULT_WINDOW_SECONDS,
                enabled = true,
                createdAtMillis = now,
                updatedAtMillis = now,
            )
            Blocking.io { container.configurationRepository.upsert(config) }
            val password = credentialGenerator.generate()
            val expiresAt = now + config.credentialPolicy.lifetimeMs
            Blocking.io {
                container.credentialStore.createCredential(
                    requester = requester,
                    configurationId = config.id,
                    password = password,
                    createdAtMillis = now,
                    expiresAtMillis = expiresAt,
                )
            }
            val command = "req $password"
            onCommand(command)
            refresh(hasSmsPermission)
        }
    }

    fun deleteConfiguration(id: ConfigurationId, hasSmsPermission: Boolean) {
        viewModelScope.launch {
            Blocking.io { container.configurationRepository.delete(id) }
            refresh(hasSmsPermission)
        }
    }

    fun clearHistory(hasSmsPermission: Boolean) {
        viewModelScope.launch {
            Blocking.io { container.database.historyDao().deleteAll() }
            refresh(hasSmsPermission)
        }
    }

    fun clearDiagnosticEvents() {
        DiagnosticEventBus.clear()
    }

    private fun normalizePhone(raw: String): CanonicalPhone? =
        when (val result = phoneNormalizer.normalize(raw)) {
            is PhoneNormalizeResult.Success -> result.phone
            else -> null
        }

    private fun timedArmErrorMessage(reason: TimedArmRejectReason): String =
        when (reason) {
            TimedArmRejectReason.CONFIGURATION_NOT_FOUND -> "Forwarding rule not found"
            TimedArmRejectReason.CONFIGURATION_DISABLED -> "Forwarding rule is disabled"
            TimedArmRejectReason.ACTIVE_SESSION_EXISTS -> "Another forwarding window is already active"
            TimedArmRejectReason.DURATION_OUT_OF_RANGE -> "Duration must be between 1 and 120 minutes"
            TimedArmRejectReason.APP_PAUSED -> "Turn on Master forwarding first"
            TimedArmRejectReason.APP_NOT_READY -> "Engine is not ready"
        }
}
