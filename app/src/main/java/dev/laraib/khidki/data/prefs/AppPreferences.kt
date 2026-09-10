package dev.laraib.khidki.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AppPreferences(
    context: Context,
) {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isMasterEnabled: Boolean
        get() = preferences.getBoolean(KEY_MASTER_ENABLED, false)
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

    private companion object {
        const val PREFS_NAME = "khidki_app_prefs"
        const val KEY_MASTER_ENABLED = "master_enabled"
        const val KEY_BOOT_ID = "boot_id"
    }
}
