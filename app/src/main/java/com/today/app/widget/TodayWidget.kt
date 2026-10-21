package com.today.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.today.app.MainActivity
import com.today.app.R
import com.today.app.data.Moods
import com.today.app.data.SavedState
import com.today.app.data.Streaks
import com.today.app.data.TodayStorage
import com.today.app.data.activeOn
import com.today.app.data.questsOn
import com.today.app.i18n.strings
import com.today.app.ui.components.HeadBox
import com.today.app.ui.components.renderAvatarBitmap
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.roundToInt

/** Builds and pushes the one-row home-screen widget: character, level & streak, today's %, and an open button. */
object TodayWidget {
    const val ACTION_REFRESH = "com.today.app.widget.REFRESH"

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val MINT = 0xFF3DDC97.toInt()

    /** Asks the launcher to place the widget (Settings shortcut). False when the launcher can't. */
    fun requestPin(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        return manager.requestPinAppWidget(ComponentName(context, TodayWidgetProvider::class.java), null, null)
    }

    /** Redraws every placed widget from what's saved (cheap no-op when none is placed). */
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, build(context, TodayStorage(context).load()))
        scheduleMidnightRefresh(context)
    }

    private fun build(context: Context, saved: SavedState): RemoteViews {
        val today = LocalDate.now()
        val s = saved.settings.language.strings()
        val todays = saved.questsOn(today)
        // Today may only exist as a carried-over preview; mood and streak should see it.
        val byDate = saved.questsByDate + (today.toString() to todays)
        val active = todays.activeOn(today) // resting quests aren't due today
        val done = active.count { it.done }
        val percent = if (active.isEmpty()) 0 else (done * 100f / active.size).roundToInt()
        val streak = Streaks.currentFor(byDate, saved.rewards.shieldedDates, today)
        val mood = saved.settings.statusMood ?: Moods.of(byDate, saved.rewards.shieldedDates, today)

        val views = RemoteViews(context.packageName, R.layout.widget_today)
        views.setImageViewBitmap(R.id.widget_avatar, renderAvatarBitmap(context, saved.character.outfit, mood, width = 192, box = HeadBox.FACE))
        views.setTextViewText(R.id.widget_level, "LV ${saved.character.level} · 🔥 ${s.days(streak)}")
        views.setProgressBar(R.id.widget_bar, 100, percent, false)
        views.setTextViewText(R.id.widget_percent, if (active.isEmpty()) "–" else "$percent%")
        views.setTextColor(R.id.widget_percent, if (percent == 100) MINT else WHITE)
        views.setTextViewText(R.id.widget_today_label, if (active.isEmpty()) s.widgetNoQuests else s.widgetTodayLabel)
        views.setTextViewText(R.id.widget_open, s.widgetOpen)

        // The button is the obvious target; tapping anywhere else on the bar opens the app too.
        val open = openAppIntent(context)
        views.setOnClickPendingIntent(R.id.widget_open, open)
        views.setOnClickPendingIntent(R.id.widget_root, open)
        return views
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** Switch to the new day's numbers right after midnight, even if the app is never opened. */
    private fun scheduleMidnightRefresh(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_REFRESH)
        val pi = PendingIntent.getBroadcast(context, 99, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val now = ZonedDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
        am.set(AlarmManager.RTC, nextMidnight.toInstant().toEpochMilli() + 5_000, pi)
    }
}

/** Launcher entry point: placed, resized (by the system) or periodic refresh. */
class TodayWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        TodayWidget.updateAll(context)
    }
}

/** Midnight refresh. Not exported: only our own alarm reaches it. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TodayWidget.ACTION_REFRESH) TodayWidget.updateAll(context)
    }
}
