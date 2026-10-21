package com.today.app.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime

/**
 * Local crash log: an uncaught exception is written to a file before the app dies, and the next
 * launch offers to share it (Settings > Privacy can turn the offer off). Nothing is sent anywhere
 * unless the user picks an app in the share sheet.
 */
object CrashReporter {
    private const val FILE = "last_crash.txt"
    private const val MAX_CHARS = 12_000

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { File(app.filesDir, FILE).writeText(report(app, thread, error).take(MAX_CHARS)) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The saved report, or null when the last session ended normally. */
    fun pending(context: Context): String? = runCatching {
        File(context.filesDir, FILE).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
    }

    private fun report(context: Context, thread: Thread, error: Throwable): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
        return buildString {
            appendLine("Today $version crash report")
            appendLine("Time: ${LocalDateTime.now()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(trace)
        }
    }
}
