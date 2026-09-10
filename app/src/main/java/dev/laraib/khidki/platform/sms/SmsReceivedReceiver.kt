package dev.laraib.khidki.platform.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dev.laraib.khidki.KhidkiApp

class SmsReceivedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) {
            return
        }
        if (!KhidkiApp.runtime.appPreferences.isMasterEnabled) {
            return
        }

        val pending = goAsync()
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val sender = messages.firstOrNull()?.displayOriginatingAddress ?: return
            val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
            if (body.length > SmsInboundProcessor.MAX_BODY) {
                Log.w(TAG, "Reject oversize SMS from=$sender len=${body.length}")
                return
            }
            KhidkiApp.runtime.smsProcessor.process(
                sender = sender,
                body = body,
                receivedAtMillis = System.currentTimeMillis(),
            )
        } catch (error: Exception) {
            Log.e(TAG, "SMS receive failed", error)
        } finally {
            pending.finish()
        }
    }

    companion object {
        private const val TAG = "KhidkiSmsReceiver"
    }
}
