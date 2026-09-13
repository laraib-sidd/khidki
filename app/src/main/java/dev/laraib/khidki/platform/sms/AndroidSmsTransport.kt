package dev.laraib.khidki.platform.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import dev.laraib.khidki.domain.budget.SmsBudgetLedger
import dev.laraib.khidki.domain.model.CanonicalPhone
import dev.laraib.khidki.domain.model.SendOutcome
import dev.laraib.khidki.domain.model.SendResult
import dev.laraib.khidki.domain.ports.SmsTransport

class AndroidSmsTransport(
    private val context: Context,
) : SmsTransport {
    override fun send(to: CanonicalPhone, body: String): SendResult {
        val destination = to.e164
        if (destination.length < 8 || destination.any { it.isLetter() }) {
            return SendResult(SendOutcome.FAILED, 0)
        }

        val parts = estimateParts(body)
        if (parts > SmsBudgetLedger.MAX_PARTS_PER_MESSAGE) {
            return SendResult(SendOutcome.FAILED, parts)
        }

        val smsManager = SmsManager.getDefault()
        val sentIntent = pendingIntent(SmsSendResultReceiver.ACTION_SENT, destination.hashCode())
        val deliveryIntent = pendingIntent(SmsSendResultReceiver.ACTION_DELIVERED, destination.hashCode() + 1)

        try {
            if (body.length <= 160) {
                smsManager.sendTextMessage(destination, null, body, sentIntent, deliveryIntent)
            } else {
                val divided = smsManager.divideMessage(body)
                val sentIntents = divided.indices.map { index ->
                    pendingIntent(SmsSendResultReceiver.ACTION_SENT, destination.hashCode() + index + 10)
                }
                val deliveryIntents = divided.indices.map { index ->
                    pendingIntent(SmsSendResultReceiver.ACTION_DELIVERED, destination.hashCode() + index + 100)
                }
                smsManager.sendMultipartTextMessage(
                    destination,
                    null,
                    divided,
                    ArrayList(sentIntents),
                    ArrayList(deliveryIntents),
                )
            }
            OutboundSmsGuard.remember(body)
            return SendResult(SendOutcome.SENT, parts)
        } catch (_: Exception) {
            return SendResult(SendOutcome.FAILED, parts)
        }
    }

    private fun estimateParts(body: String): Int {
        if (body.isEmpty()) return 1
        return if (body.length <= 160) 1 else 1 + ((body.length - 160 + 152) / 153)
    }

    private fun pendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, SmsSendResultReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
