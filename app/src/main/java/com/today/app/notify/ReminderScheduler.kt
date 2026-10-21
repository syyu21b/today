package com.today.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.today.app.data.AppSettings
import com.today.app.data.Quest
import com.today.app.data.SavedState
import com.today.app.i18n.Strings
import com.today.app.data.Streaks
import com.today.app.data.activeOn
import com.today.app.data.questsOn
import com.today.app.widget.TodayWidget
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
    const val ACTION_QUEST_ITEM = "com.today.app.action.QUEST_ITEM_REMINDER"
    private const val REQUEST_QUEST_ITEM = 3

    fun reschedule(context: Context, settings: AppSettings) {
        val on = settings.notificationsEnabled
        schedule(context, ACTION_QUEST, 1, on && settings.questReminder, settings.questReminderTime)
        schedule(context, ACTION_STREAK, 2, on && settings.streakReminder, settings.streakReminderTime)
        rescheduleQuestAlarm(context)
    }

    /**
     * One alarm for the next quest that has its own reminder time (today's still-open ones, else
     * tomorrow's first). When it fires, every quest that became due since the last firing is announced.
     */
    fun rescheduleQuestAlarm(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_QUEST_ITEM)
        val pi = PendingIntent.getBroadcast(context, REQUEST_QUEST_ITEM, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        am.cancel(pi)
        val saved = TodayStorage(context).load()
        if (!saved.settings.notificationsEnabled) return
        val now = ZonedDateTime.now()
        val minute = now.hour * 60 + now.minute
        val today = now.toLocalDate()
        val nextToday = saved.questsOn(today).activeOn(today).filter { !it.done }.mapNotNull { it.remindAt }.filter { it > minute }.minOrNull()
        val trigger = if (nextToday != null) {
            nextTrigger(nextToday, now)
        } else {
            val tomorrow = today.plusDays(1)
            // Tomorrow's list is usually today's carried over.
            val first = saved.questsOn(tomorrow).ifEmpty { saved.questsOn(today) }.activeOn(tomorrow).mapNotNull { it.remindAt }.minOrNull() ?: return
            nextTrigger(first, tomorrow.atStartOfDay(now.zone).minusSeconds(1))
        }
        am.setWindow(AlarmManager.RTC_WAKEUP, trigger, QUEST_WINDOW_MS, pi)
    }

    private const val QUEST_WINDOW_MS = 5 * 60 * 1000L

    /** Quests due between the last announced minute (exclusive) and [minute] (inclusive). */
    fun dueQuests(quests: List<Quest>, afterMinute: Int, minute: Int): List<Quest> =
        quests.filter { q -> !q.done && q.remindAt != null && q.remindAt > afterMinute && q.remindAt <= minute }

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
        val todays = saved.questsOn(todayDate)
        // Rest-day quests aren't due today.
        val remaining = todays.activeOn(todayDate).count { !it.done }
        Log.i(
            TAG,
            "fired ${intent.action} enabled=${settings.notificationsEnabled} quest=${settings.questReminder} " +
                "streak=${settings.streakReminder} remaining=$remaining canPost=${Notifier.canPost(context)}",
        )

        if (intent.action == ReminderScheduler.ACTION_QUEST_ITEM) {
            announceDueQuests(context, saved, s, todayDate)
            ReminderScheduler.rescheduleQuestAlarm(context)
            return
        }

        if (settings.notificationsEnabled && remaining > 0) {
            when (intent.action) {
                ReminderScheduler.ACTION_QUEST -> if (settings.questReminder) {
                    Notifier.post(context, s, Notifier.ID_QUEST, Notifier.CHANNEL_REMINDERS, s.questReminderTitle, s.questReminderBody(remaining))
                }
                // A shielded day is already safe: no alert.
                ReminderScheduler.ACTION_STREAK -> if (settings.streakReminder && today !in saved.rewards.shieldedDates) {
                    val shields = saved.rewards.shields
                    // Only offer the shield when there is a streak (ending yesterday) for it to keep.
                    val streak = Streaks.endingAtFor(saved.questsByDate, saved.rewards.shieldedDates, todayDate.minusDays(1))
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

/** Posts one alert per quest whose own reminder time has come, each only once per day. */
private fun announceDueQuests(context: Context, saved: SavedState, s: Strings, today: LocalDate) {
    if (!saved.settings.notificationsEnabled) return
    val prefs = context.getSharedPreferences("reminder_state", Context.MODE_PRIVATE)
    val now = java.time.LocalTime.now()
    val minute = now.hour * 60 + now.minute
    val last = if (prefs.getString("questItemDate", null) == today.toString()) prefs.getInt("questItemMinute", -1) else -1
    val due = ReminderScheduler.dueQuests(saved.questsOn(today).activeOn(today), last, minute)
    due.forEach { q ->
        Notifier.post(context, s, Notifier.questItemId(q.title), Notifier.CHANNEL_REMINDERS, s.questItemReminderTitle(q.emoji, q.title), s.questItemReminderBody)
    }
    prefs.edit {
        putString("questItemDate", today.toString())
        putInt("questItemMinute", minute)
    }
}

/** Alarms are cleared on reboot / app update: put them back. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_TIMEZONE_CHANGED -> {
                ReminderScheduler.reschedule(context, TodayStorage(context).load().settings)
                // The widget's "today" (and its midnight alarm) depends on the clock too.
                TodayWidget.updateAll(context)
            }
        }
    }
}
