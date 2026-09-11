package dev.laraib.khidki.platform.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuditEventType

class StatusNotifier(
    private val context: Context,
) {
    fun onAuditEvent(event: AuditEvent) {
        when (event.type) {
            AuditEventType.SESSION_ARMED -> notify(
                title = "Khidki window armed",
                body = "Forwarding window is active.",
            )
            AuditEventType.CANDIDATE_FORWARDED -> notify(
                title = "Khidki forwarded SMS",
                body = "A matching message was forwarded to the requester.",
            )
            AuditEventType.TIMED_ARMED -> notify(
                title = "Khidki timed window armed",
                body = "Forwarding window is active until expiry.",
            )
            AuditEventType.TIMED_CANCELLED -> notify(
                title = "Khidki timed window cancelled",
                body = "Timed forwarding was stopped.",
            )
            AuditEventType.TIMED_EXPIRED -> notify(
                title = "Khidki timed window expired",
                body = "Timed forwarding has ended.",
            )
            else -> Unit
        }
    }

    private fun notify(title: String, body: String) {
        if (!canPostNotifications()) {
            return
        }
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(nextNotificationId(), notification)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Khidki status",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Armed window and forwarding status"
        }
        manager.createNotificationChannel(channel)
    }

    private fun nextNotificationId(): Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

    companion object {
        const val CHANNEL_ID = "khidki_forwarding"
    }
}
