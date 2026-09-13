package dev.laraib.khidki

import android.app.Application

class KhidkiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runtime = KhidkiRuntime.get(this)
        if (runtime.appPreferences.bootId.isBlank()) {
            runtime.appPreferences.bootId = "boot-${System.currentTimeMillis()}"
        }
    }

    companion object {
        lateinit var runtime: KhidkiRuntime
            private set
    }
}
