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
import dev.laraib.khidki.domain.phone.PhoneNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class KhidkiUiState(
    val masterEnabled: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val appStateLabel: String = "—",
    val activeSession: AuthorizationSession? = null,
    val configurations: List<Configuration> = emptyList(),
    val history: List<HistoryEvent> = emptyList(),
    val revealedCommand: String? = null,
    val errorMessage: String? = null,
)

class KhidkiViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime: KhidkiRuntime = KhidkiRuntime.get(application)
    private val container = runtime.container
    private val phoneNormalizer = PhoneNormalizer()
    private val credentialGenerator = CredentialGenerator()

    private val _uiState = MutableStateFlow(KhidkiUiState())
    val uiState: StateFlow<KhidkiUiState> = _uiState.asStateFlow()

    fun refresh(hasSmsPermission: Boolean) {
        runtime.refreshAppState(hasSmsPermission)
        viewModelScope.launch {
            val configs = Blocking.io { container.configurationRepository.getAll() }
            val history = Blocking.io { container.auditStore.listRecent(100) }
            val session = Blocking.io {
                container.sessionRepository.getActiveSession(runtime.clock.nowMillis())
            }
            _uiState.value = _uiState.value.copy(
                masterEnabled = runtime.appPreferences.isMasterEnabled,
                hasSmsPermission = hasSmsPermission,
                appStateLabel = runtime.engine.appState().name,
                activeSession = session,
                configurations = configs,
                history = history,
                errorMessage = null,
            )
        }
    }

    fun setMasterEnabled(enabled: Boolean, hasSmsPermission: Boolean) {
        runtime.appPreferences.isMasterEnabled = enabled
        runtime.refreshAppState(hasSmsPermission)
        refresh(hasSmsPermission)
    }

    fun cancelActiveSession(hasSmsPermission: Boolean) {
        runtime.engine.setAppState(dev.laraib.khidki.domain.model.AppState.PAUSED)
        runtime.engine.setAppState(dev.laraib.khidki.domain.model.AppState.READY)
        refresh(hasSmsPermission)
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
            val requester = normalizePhone(requesterRaw)
                ?: run {
                    _uiState.value = _uiState.value.copy(errorMessage = "Invalid requester number")
                    return@launch
                }
            if (senderPattern.isBlank() || contentPattern.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Sender and content filters are required")
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

    private fun normalizePhone(raw: String): CanonicalPhone? =
        when (val result = phoneNormalizer.normalize(raw)) {
            is PhoneNormalizeResult.Success -> result.phone
            else -> null
        }
}
