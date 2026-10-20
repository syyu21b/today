package com.today.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.today.app.data.AppSettings
import com.today.app.data.Streaks
import com.today.app.data.activeOn
import com.today.app.data.completeOn
import com.today.app.data.isRestDayOn
import com.today.app.data.TodayStorage
import com.today.app.i18n.strings
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Daily quest / streak reminders via AlarmManager, without the exact-alarm permission
 * (they can arrive up to ~10 minutes late). Each alarm re-arms itself for the next day when it fires.
 */
object ReminderScheduler {
    const val ACTION_QUEST = "com.today.app.action.QUEST_REMINDER"
    const val ACTION_STREAK = "com.today.app.action.STREAK_REMINDER"

    fun reschedule(context: Context, settings: AppSettings) {
        val on = settings.notificationsEnabled
        schedule(context, ACTION_QUEST, 1, on && settings.questReminder, settings.questReminderTime)
        schedule(context, ACTION_STREAK, 2, on && settings.streakReminder, settings.streakReminderTime)
    }

    private fun schedule(context: Context, action: String, requestCode: Int, enabled: Boolean, minuteOfDay: Int) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        val pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.cancel(pi)
        if (!enabled) return
        // A 10-minute window instead of the default (up to ~1 hour) inexact batching,
        // without asking for the special exact-alarm permission.
        am.setWindow(AlarmManager.RTC_WAKEUP, nextTrigger(minuteOfDay), WINDOW_MS, pi)
    }

    private const val WINDOW_MS = 10 * 60 * 1000L

    /** Next wall-clock time at [minuteOfDay]; tomorrow if that moment already passed today. */
    fun nextTrigger(minuteOfDay: Int, now: ZonedDateTime = ZonedDateTime.now()): Long {
        var t = now.withHour(minuteOfDay / 60).withMinute(minuteOfDay % 60).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }
}

private const val TAG = "TodayReminder"

/** Fires the reminder (if there is still something to do today) and re-arms tomorrow's alarm. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val saved = TodayStorage(context).load()
        val settings = saved.settings
        val s = settings.language.strings()
        val todayDate = LocalDate.now()
        val today = todayDate.toString()
        // Today's list may not exist yet (app not opened today): use what will be carried over.
        val todays = saved.questsByDate[today]
            ?: saved.questsByDate.keys.filter { it < today }.sortedDescending()
                .firstNotNullOfOrNull { k -> saved.questsByDate[k]?.takeIf { it.isNotEmpty() } }
                ?.map { it.copy(done = false) }
                .orEmpty()
        // Rest-day quests aren't due today.
        val remaining = todays.activeOn(todayDate).count { !it.done }
        Log.i(
            TAG,
            "fired ${intent.action} enabled=${settings.notificationsEnabled} quest=${settings.questReminder} " +
                "streak=${settings.streakReminder} remaining=$remaining canPost=${Notifier.canPost(context)}",
        )

        if (settings.notificationsEnabled && remaining > 0) {
            when (intent.action) {
                ReminderScheduler.ACTION_QUEST -> if (settings.questReminder) {
                    Notifier.post(context, s, Notifier.ID_QUEST, Notifier.CHANNEL_REMINDERS, s.questReminderTitle, s.questReminderBody(remaining))
                }
                // A shielded day is already safe: no alert.
                ReminderScheduler.ACTION_STREAK -> if (settings.streakReminder && today !in saved.rewards.shieldedDates) {
                    val shields = saved.rewards.shields
                    // Only offer the shield when there is a streak (ending yesterday) for it to keep.
                    val streak = Streaks.endingAt(
                        todayDate.minusDays(1),
                        { d -> saved.questsByDate[d.toString()].orEmpty().completeOn(d) },
                        { d -> d.toString() in saved.rewards.shieldedDates || saved.questsByDate[d.toString()].orEmpty().isRestDayOn(d) },
                    )
                    Notifier.post(
                        context, s, Notifier.ID_STREAK, Notifier.CHANNEL_REMINDERS, s.streakReminderTitle, s.streakReminderBody(remaining),
                        actions = if (shields > 0 && streak > 0) listOf(Notifier.useShieldAction(context, s, shields)) else emptyList(),
                    )
                }
            }
        }
        ReminderScheduler.reschedule(context, settings)
    }
}

/** Alarms are cleared on reboot / app update: put them back. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIMEZONE_CHANGED ->
                ReminderScheduler.reschedule(context, TodayStorage(context).load().settings)
        }
    }
}
