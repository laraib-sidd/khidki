package dev.laraib.khidki.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.laraib.khidki.KhidkiRuntime
import dev.laraib.khidki.data.adapter.Blocking
import dev.laraib.khidki.domain.filter.CustomSenderRule
import dev.laraib.khidki.domain.filter.ForwardingPolicy
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.CanonicalPhone
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

data class KhidkiUiState(
    val hasSmsPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val appStateLabel: String = "—",
    val activeSession: AuthorizationSession? = null,
    val history: List<HistoryEvent> = emptyList(),
    val errorMessage: String? = null,
    val welcomeCompleted: Boolean = false,
    val advancedUnlocked: Boolean = false,
    val trustedNumberE164: String? = null,
    val forwardingPolicy: ForwardingPolicy = ForwardingPolicy.defaultFirstRun(),
)

class KhidkiViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime: KhidkiRuntime = KhidkiRuntime.get(application)
    private val container = runtime.container
    private val phoneNormalizer = PhoneNormalizer()

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
            migrateLegacySettingsIfNeeded()
            val history = Blocking.io { container.auditStore.listRecent(100) }
            val session = Blocking.io {
                container.sessionRepository.getActiveSession(runtime.clock.nowMillis())
            }
            _uiState.value = _uiState.value.copy(
                hasSmsPermission = hasSmsPermission,
                hasNotificationPermission = hasNotifications,
                appStateLabel = runtime.engine.appState().name,
                activeSession = session,
                history = history,
                welcomeCompleted = runtime.appPreferences.welcomeCompleted,
                advancedUnlocked = runtime.appPreferences.advancedUnlocked,
                trustedNumberE164 = runtime.appPreferences.trustedNumberE164,
                forwardingPolicy = runtime.appPreferences.forwardingPolicy,
            )
        }
    }

    private suspend fun migrateLegacySettingsIfNeeded() {
        if (runtime.appPreferences.policyMigrated) {
            return
        }
        Blocking.io {
            val configs = container.configurationRepository.getAll()
            if (runtime.appPreferences.trustedNumberE164 == null && configs.isNotEmpty()) {
                runtime.appPreferences.trustedNumberE164 = configs.first().requester.e164
            }
            runtime.appPreferences.policyMigrated = true
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun completeWelcome(trustedNumberRaw: String, policy: ForwardingPolicy) {
        val trusted =
            normalizePhone(trustedNumberRaw)
                ?: run {
                    _uiState.value = _uiState.value.copy(errorMessage = "Enter a valid phone number")
                    return
                }
        runtime.appPreferences.trustedNumberE164 = trusted.e164
        runtime.appPreferences.forwardingPolicy = policy
        runtime.appPreferences.welcomeCompleted = true
        runtime.appPreferences.isMasterEnabled = true
        _uiState.value =
            _uiState.value.copy(
                welcomeCompleted = true,
                trustedNumberE164 = trusted.e164,
                forwardingPolicy = policy,
            )
    }

    fun unlockAdvanced() {
        runtime.appPreferences.advancedUnlocked = true
        _uiState.value = _uiState.value.copy(advancedUnlocked = true)
    }

    fun setTrustedNumber(raw: String, hasSmsPermission: Boolean) {
        val trusted =
            normalizePhone(raw)
                ?: run {
                    _uiState.value = _uiState.value.copy(errorMessage = "Invalid phone number")
                    return
                }
        runtime.appPreferences.trustedNumberE164 = trusted.e164
        refresh(hasSmsPermission)
    }

    fun updateForwardingPolicy(policy: ForwardingPolicy, hasSmsPermission: Boolean) {
        runtime.appPreferences.forwardingPolicy = policy
        _uiState.value = _uiState.value.copy(forwardingPolicy = policy)
        refresh(hasSmsPermission)
    }

    fun toggleCategory(
        updater: (ForwardingPolicy) -> ForwardingPolicy,
        hasSmsPermission: Boolean,
    ) {
        val updated = updater(_uiState.value.forwardingPolicy)
        updateForwardingPolicy(updated, hasSmsPermission)
    }

    fun addCustomSender(
        label: String,
        senderContains: String,
        codesOnly: Boolean,
        hasSmsPermission: Boolean,
    ) {
        if (label.isBlank() || senderContains.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Name and sender text are required")
            return
        }
        val policy = _uiState.value.forwardingPolicy
        val updated =
            policy.copy(
                customSenders =
                    policy.customSenders +
                        CustomSenderRule(
                            label = label.trim(),
                            senderContains = senderContains.trim(),
                            codesOnly = codesOnly,
                        ),
            )
        updateForwardingPolicy(updated, hasSmsPermission)
    }

    fun removeCustomSender(index: Int, hasSmsPermission: Boolean) {
        val policy = _uiState.value.forwardingPolicy
        if (index !in policy.customSenders.indices) {
            return
        }
        val updated =
            policy.copy(
                customSenders = policy.customSenders.filterIndexed { i, _ -> i != index },
            )
        updateForwardingPolicy(updated, hasSmsPermission)
    }

    fun startForwarding(durationSeconds: Int, hasSmsPermission: Boolean) {
        val destinationRaw = runtime.appPreferences.trustedNumberE164
        if (destinationRaw == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Set a trusted number in Settings first")
            return
        }
        val destination = CanonicalPhone(destinationRaw)
        val policy = runtime.appPreferences.forwardingPolicy
        viewModelScope.launch {
            when (
                val result =
                    runtime.engine.armTimedWindow(
                        destination = destination,
                        durationSeconds = durationSeconds,
                        policy = policy,
                    )
            ) {
                is TimedArmResult.Success -> {
                    DiagnosticEventBus.record("UI started forwarding (${durationSeconds}s)")
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

    fun stopForwarding(hasSmsPermission: Boolean) {
        val cancelled =
            runtime.engine.cancelTimedWindow() || runtime.engine.cancelActiveWindow()
        if (cancelled) {
            DiagnosticEventBus.record("UI stopped forwarding")
        }
        refresh(hasSmsPermission)
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
            TimedArmRejectReason.ACTIVE_SESSION_EXISTS -> "Forwarding is already running"
            TimedArmRejectReason.DURATION_OUT_OF_RANGE -> "Duration must be between 1 and 120 minutes"
            TimedArmRejectReason.APP_PAUSED -> "Forwarding is paused"
            TimedArmRejectReason.APP_NOT_READY -> "Engine is not ready"
            TimedArmRejectReason.NO_DESTINATION -> "Set a trusted number first"
            TimedArmRejectReason.NO_CATEGORIES_ENABLED -> "Turn on at least one message type in Rules"
        }
}
