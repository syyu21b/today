package com.today.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.today.app.data.ChatFontSize
import com.today.app.data.ContactsReader
import com.today.app.data.MAX_QUEST_XP
import com.today.app.data.MIN_QUEST_XP
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.PixelFace
import com.today.app.notify.Notifier
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.DmGradient
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val Blue = Color(0xFF5AA9FF)
private val Teal = Color(0xFF3CC8C8)

// ================================================================ Messages

@Composable
fun MessagesPage(vm: TodayViewModel, onBack: () -> Unit, onNavigate: (SettingsPage) -> Unit) {
    val s = LocalStrings.current
    val st = vm.settings
    var confirmClear by remember { mutableStateOf(false) }

    SettingsSubPage(s.tabMessages, onBack) {
        // Live preview of the bubble size
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                s.chatPreviewSample,
                color = Color.White,
                fontSize = st.chatFontSize.sp.sp,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp))
                    .background(DmGradient)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }

        SettingsGroup(s.groupChat) {
            SettingsRow(title = s.chatFontSize, icon = Icons.Filled.TextFields, iconColor = Violet)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                ChatFontSize.entries.forEachIndexed { i, size ->
                    SegmentedButton(
                        selected = st.chatFontSize == size,
                        onClick = { vm.updateSettings { it.copy(chatFontSize = size) } },
                        shape = SegmentedButtonDefaults.itemShape(i, ChatFontSize.entries.size),
                        label = { Text(s.fontSizeLabel(size)) },
                    )
                }
            }
            SettingsDivider()
            SwitchRow(
                s.enterToSend, st.enterToSend, { v -> vm.updateSettings { it.copy(enterToSend = v) } },
                subtitle = s.enterToSendDesc, icon = Icons.AutoMirrored.Filled.KeyboardReturn, iconColor = Blue,
            )
            SettingsDivider()
            SwitchRow(
                s.readReceipts, st.readReceipts, { v -> vm.updateSettings { it.copy(readReceipts = v) } },
                subtitle = s.readReceiptsDesc, icon = Icons.Filled.DoneAll, iconColor = Mint,
            )
            SettingsDivider()
            SwitchRow(
                s.autoReplies, st.autoReplies, { v -> vm.updateSettings { it.copy(autoReplies = v) } },
                subtitle = s.autoRepliesDesc, icon = Icons.AutoMirrored.Filled.Reply, iconColor = Gold,
            )
        }

        SettingsGroup(s.groupHistory) {
            SwitchRow(
                s.saveHistory, st.saveChatHistory, { v -> vm.updateSettings { it.copy(saveChatHistory = v) } },
                subtitle = s.saveHistoryDesc, icon = Icons.Filled.History, iconColor = Teal,
            )
            SettingsDivider()
            SettingsRow(
                title = s.clearChats, icon = Icons.Filled.DeleteSweep, iconColor = Coral, titleColor = Coral,
                onClick = { confirmClear = true },
            )
        }

        SettingsGroup {
            SettingsRow(
                title = s.messageNotif,
                subtitle = if (st.notificationsEnabled && st.messageNotifications) s.on else s.off,
                icon = Icons.Filled.Notifications, iconColor = Coral,
                onClick = { onNavigate(SettingsPage.NOTIFICATIONS) },
            )
            SettingsDivider()
            SettingsRow(
                title = s.manageBlocked,
                subtitle = s.groupBlocked(vm.blockedFriends.size),
                icon = Icons.Filled.Block, iconColor = TextMuted,
                onClick = { onNavigate(SettingsPage.FRIENDS) },
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(s.clearChats) },
            text = { Text(s.clearChatsBody) },
            confirmButton = { TextButton(onClick = { vm.clearAllChats(); confirmClear = false }) { Text(s.delete, color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(s.cancel) } },
        )
    }
}

// ================================================================ Quests

@Composable
fun QuestsPage(vm: TodayViewModel, onBack: () -> Unit, onNavigate: (SettingsPage) -> Unit) {
    val s = LocalStrings.current
    val st = vm.settings
    // Slider moves smoothly; the setting is saved when the finger lifts.
    var xpDraft by remember(st.defaultQuestXp) { mutableFloatStateOf(st.defaultQuestXp.toFloat()) }

    SettingsSubPage(s.sectionQuests, onBack) {
        SettingsGroup {
            SwitchRow(
                s.autoCarryTitle, st.autoCarryOver, { v -> vm.updateSettings { it.copy(autoCarryOver = v) } },
                subtitle = s.autoCarryDesc, icon = Icons.Filled.Repeat, iconColor = Gold,
            )
            SettingsDivider()
            SwitchRow(
                s.celebration, st.celebrationEffects, { v -> vm.updateSettings { it.copy(celebrationEffects = v) } },
                subtitle = s.celebrationDesc, icon = Icons.Filled.Celebration, iconColor = Coral,
            )
        }

        SettingsGroup {
            SettingsRow(title = s.weekStart, icon = Icons.Filled.CalendarMonth, iconColor = Blue)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                listOf(false to s.sunday, true to s.monday).forEachIndexed { i, (monday, label) ->
                    SegmentedButton(
                        selected = st.weekStartsMonday == monday,
                        onClick = { vm.updateSettings { it.copy(weekStartsMonday = monday) } },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        label = { Text(label) },
                    )
                }
            }
        }

        SettingsGroup {
            SettingsRow(
                title = s.defaultXp, subtitle = s.defaultXpDesc, icon = Icons.Filled.Star, iconColor = Gold,
                trailing = { Text("${xpDraft.roundToInt()} XP", fontFamily = PixelFont, fontWeight = FontWeight.Bold, color = Gold) },
            )
            Slider(
                value = xpDraft,
                onValueChange = { xpDraft = it },
                onValueChangeFinished = { vm.updateSettings { it.copy(defaultQuestXp = xpDraft.roundToInt()) } },
                valueRange = MIN_QUEST_XP.toFloat()..MAX_QUEST_XP.toFloat(),
                steps = MAX_QUEST_XP - MIN_QUEST_XP - 1,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }

        SettingsGroup {
            SettingsRow(
                title = s.questReminderLink, icon = Icons.Filled.Notifications, iconColor = Coral,
                onClick = { onNavigate(SettingsPage.NOTIFICATIONS) },
            )
        }
    }
}

// ================================================================ Data

@Composable
fun DataPage(vm: TodayViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    var confirmClear by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportData(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    SettingsSubPage(s.sectionData, onBack) {
        SettingsGroup(s.groupStorage, footer = s.dataDesc) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                DataStat("${vm.recordedDays}", s.statDays)
                DataStat("${vm.totalQuests}", s.statQuestsTotal)
                DataStat("${vm.totalCompleted}", s.statCompleted)
            }
            SettingsDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                DataStat("${vm.friends.size}", s.statFriends)
                DataStat("${vm.totalMessages}", s.statMessages)
                DataStat(s.days(vm.streak), s.statStreak)
            }
        }

        SettingsGroup {
            SettingsRow(
                title = s.exportData, subtitle = s.exportDesc, icon = Icons.Filled.FileDownload, iconColor = Teal,
                onClick = { exportLauncher.launch("today-backup-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.json") },
            )
            SettingsDivider()
            SettingsRow(
                title = s.importData, subtitle = s.importDesc, icon = Icons.Filled.FileUpload, iconColor = Blue,
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            )
        }

        SettingsGroup {
            SettingsRow(
                title = s.clearChats, icon = Icons.Filled.DeleteSweep, iconColor = Coral, titleColor = Coral,
                onClick = { confirmClear = true },
            )
            SettingsDivider()
            SettingsRow(
                title = s.resetButton, icon = Icons.Filled.DeleteForever, iconColor = Coral, titleColor = Coral,
                onClick = { confirmReset = true },
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(s.clearChats) },
            text = { Text(s.clearChatsBody) },
            confirmButton = { TextButton(onClick = { vm.clearAllChats(); confirmClear = false }) { Text(s.delete, color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(s.cancel) } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(s.resetTitle) },
            text = { Text(s.resetBody) },
            confirmButton = { TextButton(onClick = { vm.resetAll(); confirmReset = false }) { Text(s.resetConfirm, color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(s.cancel) } },
        )
    }
    pendingImport?.let { uri -> ImportConfirmDialog(onConfirm = { vm.importData(uri); pendingImport = null }, onDismiss = { pendingImport = null }) }
}

@Composable
private fun ImportConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.importConfirmTitle) },
        text = { Text(s.importConfirmBody) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(s.importConfirm, color = Coral) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

// ================================================================ Privacy

@Composable
fun PrivacyPage(vm: TodayViewModel, onBack: () -> Unit, onNavigate: (SettingsPage) -> Unit) {
    val s = LocalStrings.current
    var confirmDelete by remember { mutableStateOf(false) }
    SettingsSubPage(s.rowPrivacy, onBack) {
        SettingsGroup(s.privacyPolicyTitle) {
            Text(s.privacyPolicyBody, fontSize = 13.sp, lineHeight = 20.sp, color = TextMuted, modifier = Modifier.padding(16.dp))
        }
        SettingsGroup {
            SwitchRow(
                s.crashReportsTitle, vm.settings.crashReports, { v -> vm.updateSettings { it.copy(crashReports = v) } },
                subtitle = s.crashReportsDesc, icon = Icons.Filled.BugReport, iconColor = Gold,
            )
        }
        SettingsGroup {
            SettingsRow(
                title = s.sectionData, subtitle = s.importDesc, icon = Icons.Filled.Storage, iconColor = Teal,
                onClick = { onNavigate(SettingsPage.DATA) },
            )
            SettingsDivider()
            SettingsRow(
                title = s.deleteAllTitle, subtitle = s.deleteAllDesc, icon = Icons.Filled.DeleteForever, iconColor = Coral, titleColor = Coral,
                onClick = { confirmDelete = true },
            )
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(s.deleteAllTitle) },
            text = { Text(s.resetBody) },
            confirmButton = { TextButton(onClick = { vm.resetAll(); confirmDelete = false }) { Text(s.resetConfirm, color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun DataStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = TextMuted, fontSize = 12.sp)
    }
}

// ================================================================ About

private val LIBRARIES = listOf(
    "Jetpack Compose · Material 3" to "Apache License 2.0",
    "AndroidX (Core, Activity, Lifecycle, ExifInterface, SplashScreen)" to "Apache License 2.0",
    "Lottie for Android" to "Apache License 2.0",
    "Google ML Kit (Pose Detection, Selfie Segmentation)" to "ML Kit Terms of Service",
    "Kotlin · kotlinx.coroutines" to "Apache License 2.0",
    "Galmuri11 font © Lee Minseo (quiple)" to "SIL Open Font License 1.1",
)

@Composable
fun AboutPage(vm: TodayViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val version = remember { appVersionName(context) }
    var notifAllowed by remember { mutableStateOf(Notifier.canPost(context)) }
    var contactsAllowed by remember { mutableStateOf(ContactsReader.hasPermission(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifAllowed = Notifier.canPost(context)
        contactsAllowed = ContactsReader.hasPermission(context)
    }
    var showLicenses by remember { mutableStateOf(false) }

    SettingsSubPage(s.sectionAbout, onBack) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Gold), contentAlignment = Alignment.Center) {
                PixelFace(vm.character.outfit, Modifier.width(67.dp))
            }
            Spacer(Modifier.padding(top = 10.dp))
            Text("Today", fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(s.aboutDesc, color = TextMuted, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        SettingsGroup {
            SettingsRow(title = s.version, icon = Icons.Filled.Info, iconColor = TextMuted, trailing = { Text(version, color = TextMuted, fontFamily = PixelFont) })
            SettingsDivider()
            SettingsRow(title = s.builtWith, subtitle = "Kotlin · Jetpack Compose · ML Kit · Lottie", icon = Icons.Filled.Code, iconColor = Violet)
            SettingsDivider()
            SettingsRow(title = s.licenses, icon = Icons.Filled.Policy, iconColor = Blue, onClick = { showLicenses = true })
        }

        SettingsGroup(s.permissionsTitle) {
            SettingsRow(
                title = s.permNotifications, subtitle = if (notifAllowed) s.permGranted else s.permDenied,
                icon = Icons.Filled.Notifications, iconColor = Coral, onClick = { openAppNotificationSettings(context) },
            )
            SettingsDivider()
            SettingsRow(
                title = s.permContacts, subtitle = if (contactsAllowed) s.permGranted else s.permDenied,
                icon = Icons.Filled.Contacts, iconColor = Mint, onClick = { openAppDetailsSettings(context) },
            )
            SettingsDivider()
            SettingsRow(title = s.permCamera, subtitle = s.permCameraDesc, icon = Icons.Filled.CameraAlt, iconColor = Gold)
        }

        SettingsGroup(s.privacyTitle) {
            Text(s.privacyBody, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(s.licenses) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LIBRARIES.forEach { (name, license) ->
                        Column {
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(license, color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text(s.close) } },
        )
    }
}

/** How each feature works, one card per feature (Settings > User guide). */
@Composable
fun GuidePage(onBack: () -> Unit) {
    val s = LocalStrings.current
    SettingsSubPage(s.guideTitle, onBack) {
        Text(s.guideIntro, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
        s.guideSections.forEach { (emoji, title, body) ->
            SettingsGroup {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(emoji, fontSize = 24.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(body, color = TextMuted, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}
