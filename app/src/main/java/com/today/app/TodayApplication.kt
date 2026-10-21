package com.today.app

import android.app.Application
import com.today.app.data.CrashReporter

class TodayApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
