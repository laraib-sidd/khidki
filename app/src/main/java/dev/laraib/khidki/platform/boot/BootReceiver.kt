package dev.laraib.khidki.platform.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.laraib.khidki.KhidkiApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        KhidkiApp.runtime.onBootCompleted()
    }
}
