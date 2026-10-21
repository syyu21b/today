package com.today.app

import android.app.Application
import com.today.app.data.CrashReporter
import com.today.app.ui.components.CharacterArt

class TodayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        // Parse the character art off the main thread so the first screen draws without a hitch.
        Thread { runCatching { CharacterArt.preload(this) } }.start()
    }
}
