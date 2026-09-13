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
import dev.laraib.khidki.domain.model.DestinationProfile
import dev.laraib.khidki.domain.model.DestinationProfiles
import dev.laraib.khidki.domain.model.HistoryEvent
import dev.laraib.khidki.domain.model.PhoneNormalizeResult
import dev.laraib.khidki.domain.model.TimedArmRejectReason
import dev.laraib.khidki.domain.model.TimedArmResult
import dev.laraib.khidki.domain.phone.PhoneNormalizer
import dev.laraib.khidki.domain.session.ForwardingEngine
import dev.laraib.khidki.platform.diagnostics.DiagnosticEventBus
import dev.laraib.khidki.platform.notification.ForwardingNotificationManager
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
    val destinations: List<DestinationProfile> = emptyList(),
)

class KhidkiViewModel(application: Application) : AndroidViewModel(application) {
    private val runtime: KhidkiRuntime = KhidkiRuntime.get(application)
    private val container = runtime.container
    private val phoneNormalizer = PhoneNormalizer()
    private val forwardingNotificationManager = ForwardingNotificationManager(application)

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
            session?.takeIf { it.isActive }?.let { active ->
                forwardingNotificationManager.showOngoing(active, runtime.clock.nowMillis())
            }
            _uiState.value = _uiState.value.copy(
                hasSmsPermission = hasSmsPermission,
                hasNotificationPermission = hasNotifications,
                appStateLabel = runtime.engine.appState().name,
                activeSession = session,
                history = history,
                welcomeCompleted = runtime.appPreferences.welcomeCompleted,
                advancedUnlocked = runtime.appPreferences.advancedUnlocked,
                destinations = runtime.appPreferences.destinations,
            )
        }
    }

    private suspend fun migrateLegacySettingsIfNeeded() {
        if (runtime.appPreferences.destinationsMigrated) {
            return
        }
        Blocking.io {
            val existing = runtime.appPreferences.destinations
            if (existing.isEmpty()) {
                val legacyNumber = runtime.appPreferences.trustedNumberE164
                val legacyPolicy = runtime.appPreferences.forwardingPolicy
                if (legacyNumber != null) {
                    runtime.appPreferences.destinations =
                        listOf(
                            DestinationProfile(
                                name = "Contact",
                                phoneE164 = legacyNumber,
                                policy = legacyPolicy,
                                enabled = true,
                            ),
                        )
                } else {
                    val configs = container.configurationRepository.getAll()
                    if (configs.isNotEmpty()) {
                        runtime.appPreferences.destinations =
                            listOf(
                                DestinationProfile(
                                    name = configs.first().label,
                                    phoneE164 = configs.first().requester.e164,
                                    policy = ForwardingPolicy.defaultFirstRun(),
                                    enabled = true,
                                ),
                            )
                    }
                }
            }
            runtime.appPreferences.destinationsMigrated = true
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun completeWelcome(name: String, trustedNumberRaw: String, policy: ForwardingPolicy): Boolean {
        val trusted =
            normalizePhone(trustedNumberRaw)
                ?: run {
                    _uiState.value = _uiState.value.copy(errorMessage = "Enter a valid phone number")
                    return false
                }
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter a name for this person")
            return false
        }
        val profile =
            DestinationProfile(
                name = name.trim(),
                phoneE164 = trusted.e164,
                policy = policy,
                enabled = true,
            )
        runtime.appPreferences.destinations = listOf(profile)
        runtime.appPreferences.welcomeCompleted = true
        runtime.appPreferences.isMasterEnabled = true
        _uiState.value =
            _uiState.value.copy(
                welcomeCompleted = true,
                destinations = listOf(profile),
            )
        return true
    }

    fun skipWelcome() {
        runtime.appPreferences.welcomeCompleted = true
        runtime.appPreferences.isMasterEnabled = true
        _uiState.value =
            _uiState.value.copy(
                welcomeCompleted = true,
                errorMessage = "Skipped setup — add a person on Rules when you're ready",
            )
    }

    fun unlockAdvanced() {
        runtime.appPreferences.advancedUnlocked = true
        _uiState.value = _uiState.value.copy(advancedUnlocked = true)
    }

    fun addDestination(
        name: String,
        phoneRaw: String,
        policy: ForwardingPolicy,
        hasSmsPermission: Boolean,
    ) {
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Name is required")
            return
        }
        val phone =
            normalizePhone(phoneRaw)
                ?: run {
                    _uiState.value = _uiState.value.copy(errorMessage = "Invalid phone number")
                    return
                }
        val current = runtime.appPreferences.destinations
        if (current.size >= DestinationProfile.MAX_DESTINATIONS) {
            _uiState.value =
                _uiState.value.copy(
                    errorMessage = "Maximum ${DestinationProfile.MAX_DESTINATIONS} people allowed",
                )
            return
        }
        val updated =
            current +
                DestinationProfile(
                    name = name.trim(),
                    phoneE164 = phone.e164,
                    policy = policy,
                    enabled = true,
                )
        saveDestinations(updated, hasSmsPermission)
    }

    fun updateDestination(
        id: String,
        name: String,
        phoneRaw: String,
        policy: ForwardingPolicy,
        enabled: Boolean,
        hasSmsPermission: Boolean,
    ) {
        val phone =
            normalizePhone(phoneRaw)
                ?: run {
                    _uiState.value = _uiState.value.copy(errorMessage = "Invalid phone number")
                    return
                }
        val updated =
            runtime.appPreferences.destinations.map { profile ->
                if (profile.id == id) {
                    profile.copy(
                        name = name.trim(),
                        phoneE164 = phone.e164,
                        policy = policy,
                        enabled = enabled,
                    )
                } else {
                    profile
                }
            }
        saveDestinations(updated, hasSmsPermission)
    }

    fun removeDestination(id: String, hasSmsPermission: Boolean) {
        val updated = runtime.appPreferences.destinations.filterNot { it.id == id }
        saveDestinations(updated, hasSmsPermission)
    }

    fun toggleDestinationEnabled(id: String, enabled: Boolean, hasSmsPermission: Boolean) {
        val updated =
            runtime.appPreferences.destinations.map { profile ->
                if (profile.id == id) profile.copy(enabled = enabled) else profile
            }
        saveDestinations(updated, hasSmsPermission)
    }

    fun updateDestinationPolicy(
        id: String,
        updater: (ForwardingPolicy) -> ForwardingPolicy,
        hasSmsPermission: Boolean,
    ) {
        val updated =
            runtime.appPreferences.destinations.map { profile ->
                if (profile.id == id) profile.copy(policy = updater(profile.policy)) else profile
            }
        saveDestinations(updated, hasSmsPermission)
    }

    fun addCustomSenderToDestination(
        destinationId: String,
        label: String,
        senderContains: String,
        codesOnly: Boolean,
        hasSmsPermission: Boolean,
    ) {
        if (label.isBlank() || senderContains.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Name and sender text are required")
            return
        }
        updateDestinationPolicy(destinationId, { policy ->
            policy.copy(
                customSenders =
                    policy.customSenders +
                        CustomSenderRule(
                            label = label.trim(),
                            senderContains = senderContains.trim(),
                            codesOnly = codesOnly,
                        ),
            )
        }, hasSmsPermission)
    }

    fun removeCustomSenderFromDestination(
        destinationId: String,
        index: Int,
        hasSmsPermission: Boolean,
    ) {
        val profile = runtime.appPreferences.destinations.find { it.id == destinationId } ?: return
        if (index !in profile.policy.customSenders.indices) {
            return
        }
        updateDestinationPolicy(destinationId, { policy ->
            policy.copy(customSenders = policy.customSenders.filterIndexed { i, _ -> i != index })
        }, hasSmsPermission)
    }

    private fun saveDestinations(destinations: List<DestinationProfile>, hasSmsPermission: Boolean) {
        runtime.appPreferences.destinations = destinations
        _uiState.value = _uiState.value.copy(destinations = destinations)
        refresh(hasSmsPermission)
    }

    fun startForwarding(durationSeconds: Int, hasSmsPermission: Boolean) {
        val enabledProfiles = DestinationProfiles.enabledWithPolicy(runtime.appPreferences.destinations)
        if (enabledProfiles.isEmpty()) {
            _uiState.value =
                _uiState.value.copy(
                    errorMessage =
                        if (runtime.appPreferences.destinations.isEmpty()) {
                            "Add at least one person in Rules first"
                        } else {
                            "Turn on at least one person with message types enabled"
                        },
                )
            return
        }
        val snapshots = enabledProfiles.map { it.toSnapshot() }
        viewModelScope.launch {
            runtime.appPreferences.isMasterEnabled = true
            runtime.refreshAppState(hasSmsPermission)
            when (
                val result =
                    runtime.engine.armTimedWindow(
                        destinations = snapshots,
                        durationSeconds = durationSeconds,
                    )
            ) {
                is TimedArmResult.Success -> {
                    val label =
                        if (durationSeconds == ForwardingEngine.DURATION_UNTIL_STOP) {
                            "until stop"
                        } else {
                            "${durationSeconds}s"
                        }
                    DiagnosticEventBus.record(
                        "UI started forwarding ($label → ${snapshots.size} people)",
                    )
                    forwardingNotificationManager.showOngoing(result.session, runtime.clock.nowMillis())
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
            forwardingNotificationManager.dismissOngoing()
        }
        refresh(hasSmsPermission)
    }

    fun pauseApp(hasSmsPermission: Boolean) {
        runtime.engine.setAppState(dev.laraib.khidki.domain.model.AppState.PAUSED)
        runtime.appPreferences.isMasterEnabled = false
        forwardingNotificationManager.dismissOngoing()
        DiagnosticEventBus.record("UI paused Khidki")
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
            TimedArmRejectReason.DURATION_OUT_OF_RANGE -> "Pick a duration between 1 and 120 minutes, or Until I stop"
            TimedArmRejectReason.APP_PAUSED -> "Forwarding is paused — tap Start on Home when you're ready"
            TimedArmRejectReason.APP_NOT_READY -> "Engine is not ready"
            TimedArmRejectReason.NO_DESTINATION -> "Add at least one person in Rules"
            TimedArmRejectReason.NO_CATEGORIES_ENABLED -> "Turn on message types for at least one person"
        }
}
