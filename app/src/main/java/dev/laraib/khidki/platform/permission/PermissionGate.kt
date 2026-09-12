package dev.laraib.khidki.platform.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionGate {
    fun hasSmsPermissions(context: Context): Boolean {
        val receive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS)
        val send = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
        return receive == PackageManager.PERMISSION_GRANTED && send == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun createAppDetailsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun setupSteps(): List<String> = listOf(
        "Open App Info, then ⋮ → Allow restricted settings (Android 15+).",
        "Return here and tap Grant permissions.",
        "On Realme/ColorOS: Battery → Unrestricted, and enable Auto-start.",
    )
}
