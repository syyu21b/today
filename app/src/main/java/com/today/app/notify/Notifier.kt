package com.today.app.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.today.app.MainActivity
import com.today.app.R
import com.today.app.i18n.Strings

/** Channels + posting. Every post re-checks permission, so callers never crash on a revoked grant. */
object Notifier {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_MESSAGES = "messages"
    const val EXTRA_OPEN_CHAT = "com.today.app.OPEN_CHAT"
    const val EXTRA_USE_SHIELD = "com.today.app.USE_SHIELD"
    private const val REQUEST_USE_SHIELD = 5001

    const val ID_QUEST = 1001
    const val ID_STREAK = 1002
    const val ID_TEST = 1003
    private const val ID_MESSAGE_BASE = 2000
    private const val ID_QUEST_ITEM_BASE = 100_000

    /** Android 13+ needs the runtime permission; older versions only the user-level toggle. */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun canPost(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Channel names follow the app language (re-registering just renames them). */
    fun ensureChannels(context: Context, s: Strings) {
        val nm = NotificationManagerCompat.from(context)
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_REMINDERS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(s.channelReminders).build()
        )
        nm.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_MESSAGES, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(s.channelMessages).build()
        )
    }

    fun messageId(friendId: String) = ID_MESSAGE_BASE + (friendId.hashCode() and 0xFFFF)

    /** One slot per quest title, so a quest re-alerting replaces its own notification. */
    fun questItemId(title: String) = ID_QUEST_ITEM_BASE + (title.hashCode() and 0xFFFF)

    fun post(
        context: Context,
        s: Strings,
        id: Int,
        channel: String,
        title: String,
        text: String,
        openChatFriendId: String? = null,
        actions: List<NotificationCompat.Action> = emptyList(),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        ensureChannels(context, s)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            openChatFriendId?.let { putExtra(EXTRA_OPEN_CHAT, it) }
        }
        val content = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF6C4BF4.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(content)
            .setAutoCancel(true)
            .setCategory(if (channel == CHANNEL_MESSAGES) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_REMINDER)
            .apply { actions.forEach { addAction(it) } }
            .build()
        nm.notify(id, notification)
    }

    /** Streak alert button: opens the app, which spends a shield on today (the app owns the saved state). */
    fun useShieldAction(context: Context, s: Strings, shields: Int): NotificationCompat.Action {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_USE_SHIELD, true)
        }
        val pi = PendingIntent.getActivity(context, REQUEST_USE_SHIELD, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(R.drawable.ic_notification, s.useShieldAction(shields), pi)
    }

    fun cancel(context: Context, id: Int) = NotificationManagerCompat.from(context).cancel(id)
}
