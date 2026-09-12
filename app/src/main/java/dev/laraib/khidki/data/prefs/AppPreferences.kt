package dev.laraib.khidki.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dev.laraib.khidki.domain.filter.ForwardingPolicy

class AppPreferences(
    context: Context,
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isMasterEnabled: Boolean
        get() = preferences.getBoolean(KEY_MASTER_ENABLED, true)
        set(value) {
            preferences.edit { putBoolean(KEY_MASTER_ENABLED, value) }
        }

    var bootId: String
        get() = preferences.getString(KEY_BOOT_ID, "") ?: ""
        set(value) {
            preferences.edit { putString(KEY_BOOT_ID, value) }
        }

    fun clearBootId() {
        preferences.edit { remove(KEY_BOOT_ID) }
    }

    var welcomeCompleted: Boolean
        get() = preferences.getBoolean(KEY_WELCOME_COMPLETED, false)
        set(value) {
            preferences.edit { putBoolean(KEY_WELCOME_COMPLETED, value) }
        }

    var advancedUnlocked: Boolean
        get() = preferences.getBoolean(KEY_ADVANCED_UNLOCKED, false)
        set(value) {
            preferences.edit { putBoolean(KEY_ADVANCED_UNLOCKED, value) }
        }

    var trustedNumberE164: String?
        get() = preferences.getString(KEY_TRUSTED_NUMBER, null)
        set(value) {
            preferences.edit {
                if (value == null) {
                    remove(KEY_TRUSTED_NUMBER)
                } else {
                    putString(KEY_TRUSTED_NUMBER, value)
                }
            }
        }

    var forwardingPolicy: ForwardingPolicy
        get() {
            val raw = preferences.getString(KEY_FORWARDING_POLICY, null)
            return if (raw == null) {
                ForwardingPolicy.defaultFirstRun()
            } else {
                ForwardingPolicy.fromJson(raw)
            }
        }
        set(value) {
            preferences.edit { putString(KEY_FORWARDING_POLICY, value.toJson().toString()) }
        }

    var policyMigrated: Boolean
        get() = preferences.getBoolean(KEY_POLICY_MIGRATED, false)
        set(value) {
            preferences.edit { putBoolean(KEY_POLICY_MIGRATED, value) }
        }

    private companion object {
        const val PREFS_NAME = "khidki_app_prefs"
        const val KEY_MASTER_ENABLED = "master_enabled"
        const val KEY_BOOT_ID = "boot_id"
        const val KEY_WELCOME_COMPLETED = "welcome_completed"
        const val KEY_ADVANCED_UNLOCKED = "advanced_unlocked"
        const val KEY_TRUSTED_NUMBER = "trusted_number_e164"
        const val KEY_FORWARDING_POLICY = "forwarding_policy_json"
        const val KEY_POLICY_MIGRATED = "policy_migrated"
    }
}
