package dev.laraib.khidki.platform.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.laraib.khidki.R
import dev.laraib.khidki.domain.model.AuthorizationSession
import dev.laraib.khidki.domain.model.AuditEvent
import dev.laraib.khidki.domain.model.AuditEventType
import dev.laraib.khidki.ui.MainActivity
import kotlin.math.max

class ForwardingNotificationManager(
    private val context: Context,
    private val sessionProvider: (() -> AuthorizationSession?)? = null,
) {
    fun onAuditEvent(event: AuditEvent) {
        when (event.type) {
            AuditEventType.TIMED_ARMED -> showOngoingFromDetail(event.detail)
            AuditEventType.TIMED_CANCELLED,
            AuditEventType.TIMED_EXPIRED,
            AuditEventType.SESSION_CANCELLED,
            AuditEventType.SESSION_EXPIRED,
            -> dismissOngoing()
            AuditEventType.CANDIDATE_FORWARDED -> refreshOngoingFromSession()
            else -> Unit
        }
    }

    fun showOngoing(session: AuthorizationSession, nowMillis: Long = System.currentTimeMillis()) {
        if (!canPostNotifications()) {
            return
        }
        ensureChannel()
        val remainingMillis =
            if (session.untilStop) {
                null
            } else {
                max(0L, session.expiresAtMillis - nowMillis)
            }
        val body =
            buildString {
                append(
                    context.getString(
                        R.string.notification_forwarding_body,
                        session.forwardCount,
                        session.activeDestinations().size,
                    ),
                )
                if (remainingMillis != null) {
                    append(" · ")
                    append(formatRemaining(remainingMillis))
                } else {
                    append(" · ")
                    append(context.getString(R.string.notification_until_stop))
                }
            }
        postOngoing(
            title = context.getString(R.string.notification_forwarding_title),
            body = body,
        )
    }

    fun dismissOngoing() {
        NotificationManagerCompat.from(context).cancel(ONGOING_NOTIFICATION_ID)
    }

    private fun showOngoingFromDetail(detail: String?) {
        val untilStop = detail?.startsWith("until-stop") == true
        val body =
            if (untilStop) {
                context.getString(R.string.notification_forwarding_until_stop_body)
            } else {
                context.getString(R.string.notification_forwarding_started_body)
            }
        postOngoing(
            title = context.getString(R.string.notification_forwarding_title),
            body = body,
        )
    }

    private fun refreshOngoingFromSession() {
        val session = sessionProvider?.invoke() ?: return
        showOngoing(session)
    }

    private fun postOngoing(title: String, body: String) {
        if (!canPostNotifications()) {
            return
        }
        ensureChannel()
        val openHomeIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentPendingIntent =
            PendingIntent.getActivity(
                context,
                REQUEST_OPEN_HOME,
                openHomeIntent,
                pendingIntentFlags(),
            )
        val stopIntent =
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_STOP_FORWARDING
            }
        val stopPendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_STOP,
                stopIntent,
                pendingIntentFlags(),
            )
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentPendingIntent)
                .addAction(
                    R.drawable.ic_notification,
                    context.getString(R.string.notification_action_stop),
                    stopPendingIntent,
                )
                .build()
        NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, notification)
    }

    private fun pendingIntentFlags(): Int {
        val base = PendingIntent.FLAG_UPDATE_CURRENT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            base or PendingIntent.FLAG_IMMUTABLE
        } else {
            base
        }
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
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
        manager.createNotificationChannel(channel)
    }

    private fun formatRemaining(remainingMillis: Long): String {
        val totalSeconds = remainingMillis / 1_000L
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    companion object {
        const val CHANNEL_ID = "khidki_forwarding_ongoing"
        const val ONGOING_NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN_HOME = 2001
        private const val REQUEST_STOP = 2002
    }
}
