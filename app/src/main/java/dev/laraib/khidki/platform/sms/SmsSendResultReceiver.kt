package dev.laraib.khidki.platform.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SmsSendResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val resultCode = resultCode
        Log.i(TAG, "SMS callback action=$action result=$resultCode")
    }

    companion object {
        private const val TAG = "KhidkiSmsCallback"
        const val ACTION_SENT = "dev.laraib.khidki.SMS_SENT"
        const val ACTION_DELIVERED = "dev.laraib.khidki.SMS_DELIVERED"
    }
}
