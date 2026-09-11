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

    fun createAppDetailsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun sideloadSetupSteps(): List<String> = listOf(
        "Tap Open App Info, then ⋮ → Allow restricted settings (Android 15+).",
        "Return to Khidki and tap Grant SMS Permissions.",
        "Realme GT 6T: set Battery to Unrestricted and enable Auto-start.",
    )
}
