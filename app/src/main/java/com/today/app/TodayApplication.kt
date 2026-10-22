package com.today.app

import android.app.Application
import com.today.app.data.CrashReporter
import com.today.app.ui.components.CharacterArt
import com.today.app.ui.components.PetArt

class TodayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        // Parse the character and pet art off the main thread so the first screen draws without a hitch.
        Thread { runCatching { CharacterArt.preload(this); PetArt.get(this) } }.start()
    }
}
