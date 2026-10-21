package com.today.app.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.notify.Notifier
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.Violet

@Composable
fun NotificationsPage(vm: TodayViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val st = vm.settings
    val context = LocalContext.current

    // Re-check when coming back from system settings.
    var systemAllowed by remember { mutableStateOf(Notifier.canPost(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { systemAllowed = Notifier.canPost(context) }

    var showBlockedDialog by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<String?>(null) } // "quest" | "streak"

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        systemAllowed = Notifier.canPost(context)
        if (granted && systemAllowed) vm.setNotificationsEnabled(true) else showBlockedDialog = true
    }

    fun turnOn() {
        when {
            Notifier.canPost(context) -> vm.setNotificationsEnabled(true)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifier.hasPermission(context) ->
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> showBlockedDialog = true // app notifications switched off in system settings
        }
    }

    val active = st.notificationsEnabled && systemAllowed

    SettingsSubPage(s.rowNotifications, onBack) {
        SettingsGroup {
            SwitchRow(
                title = s.notifMaster,
                subtitle = s.notifMasterDesc,
                checked = active,
                onCheckedChange = { if (it) turnOn() else vm.setNotificationsEnabled(false) },
                icon = Icons.Filled.Notifications,
                iconColor = Coral,
            )
        }

        if (st.notificationsEnabled && !systemAllowed) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Coral.copy(alpha = 0.12f))
                    .border(1.dp, Coral.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, null, tint = Coral)
                Spacer(Modifier.width(10.dp))
                Text(s.notifPermissionDenied, fontSize = 13.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { openAppNotificationSettings(context) }) { Text(s.openSystemSettings, color = Coral) }
            }
        }

        SettingsGroup(s.groupReminders) {
            SwitchRow(
                s.questReminder, st.questReminder, { v -> vm.updateSettings { it.copy(questReminder = v) } },
                subtitle = s.questReminderDesc, icon = Icons.Filled.Alarm, iconColor = Gold, enabled = active,
            )
            SettingsDivider()
            SettingsRow(
                title = s.reminderTime,
                enabled = active && st.questReminder,
                onClick = { editingTime = "quest" },
                trailing = { ValueTrailing(s.chatTime(minutesToTime(st.questReminderTime))) },
            )
            SettingsDivider()
            SwitchRow(
                s.streakReminder, st.streakReminder, { v -> vm.updateSettings { it.copy(streakReminder = v) } },
                subtitle = s.streakReminderDesc, icon = Icons.Filled.LocalFireDepartment, iconColor = Coral, enabled = active,
            )
            SettingsDivider()
            SettingsRow(
                title = s.reminderTime,
                enabled = active && st.streakReminder,
                onClick = { editingTime = "streak" },
                trailing = { ValueTrailing(s.chatTime(minutesToTime(st.streakReminderTime))) },
            )
        }

        SettingsGroup(s.groupMessageAlerts) {
            SwitchRow(
                s.messageNotif, st.messageNotifications, { v -> vm.updateSettings { it.copy(messageNotifications = v) } },
                subtitle = s.messageNotifDesc, icon = Icons.AutoMirrored.Filled.Message, iconColor = Violet, enabled = active,
            )
            SettingsDivider()
            SwitchRow(
                s.messagePreview, st.messagePreview, { v -> vm.updateSettings { it.copy(messagePreview = v) } },
                subtitle = s.messagePreviewDesc, icon = Icons.Filled.Visibility, iconColor = Mint,
                enabled = active && st.messageNotifications,
            )
        }

        SettingsGroup {
            SettingsRow(
                title = s.testNotification,
                icon = Icons.Filled.NotificationsActive,
                iconColor = Gold,
                enabled = active,
                onClick = vm::sendTestNotification,
            )
        }
    }

    editingTime?.let { which ->
        val initial = if (which == "quest") st.questReminderTime else st.streakReminderTime
        ReminderTimeDialog(
            initialMinutes = initial,
            onDismiss = { editingTime = null },
            onConfirm = { minutes ->
                vm.updateSettings { if (which == "quest") it.copy(questReminderTime = minutes) else it.copy(streakReminderTime = minutes) }
                editingTime = null
            },
        )
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedDialog = false },
            title = { Text(s.rowNotifications) },
            text = { Text(s.notifPermissionDenied) },
            confirmButton = {
                TextButton(onClick = { showBlockedDialog = false; openAppNotificationSettings(context) }) { Text(s.openSystemSettings) }
            },
            dismissButton = { TextButton(onClick = { showBlockedDialog = false }) { Text(s.cancel) } },
        )
    }
}

/** 24-hour dial: digits only, so it reads the same in Korean and English. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeDialog(initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val s = LocalStrings.current
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.pickTime) },
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { TimePicker(state = state) } },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text(s.ok) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}
