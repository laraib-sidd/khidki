package dev.laraib.khidki.platform.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.laraib.khidki.KhidkiRuntime
import dev.laraib.khidki.platform.diagnostics.DiagnosticEventBus

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val runtime = KhidkiRuntime.get(context)
        val notificationManager = ForwardingNotificationManager(context)
        when (action) {
            ACTION_STOP_FORWARDING -> {
                val stopped =
                    runtime.engine.cancelTimedWindow() || runtime.engine.cancelActiveWindow()
                if (stopped) {
                    DiagnosticEventBus.record("Notification stopped forwarding")
                }
                notificationManager.dismissOngoing()
            }
        }
    }

    companion object {
        const val ACTION_STOP_FORWARDING = "dev.laraib.khidki.action.STOP_FORWARDING"
    }
}
