package com.today.app.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.AppLanguage
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.screens.PixelProgressBar
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import com.today.app.widget.TodayWidget
import java.time.LocalTime

private val Blue = Color(0xFF5AA9FF)
private val Teal = Color(0xFF3CC8C8)
private val Slate = Color(0xFFA8A3C2)

/**
 * Settings tab: a dashboard of categories, each opening its own page.
 * [onOpenChat] jumps to a conversation (from the friend list).
 */
@Composable
fun SettingsScreen(
    vm: TodayViewModel,
    onOpenOutfit: () -> Unit,
    onOpenChat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
    BackHandler(enabled = page != SettingsPage.HOME) { page = SettingsPage.HOME }
    val back = { page = SettingsPage.HOME }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState != SettingsPage.HOME) slideInHorizontally { it / 3 } + fadeIn() togetherWith fadeOut()
            else fadeIn() togetherWith slideOutHorizontally { it / 3 } + fadeOut()
        },
        modifier = modifier.fillMaxSize(),
        label = "settings",
    ) { current ->
        when (current) {
            SettingsPage.HOME -> SettingsHome(vm, onOpenOutfit, onNavigate = { page = it })
            SettingsPage.REWARDS -> RewardsPage(vm, back)
            SettingsPage.LANGUAGE -> LanguagePage(vm, back)
            SettingsPage.NOTIFICATIONS -> NotificationsPage(vm, back)
            SettingsPage.FRIENDS -> FriendsPage(vm, back, onOpenChat)
            SettingsPage.MESSAGES -> MessagesPage(vm, back, onNavigate = { page = it })
            SettingsPage.QUESTS -> QuestsPage(vm, back, onNavigate = { page = it })
            SettingsPage.DATA -> DataPage(vm, back)
            SettingsPage.ABOUT -> AboutPage(vm, back)
        }
    }
}

@Composable
private fun SettingsHome(vm: TodayViewModel, onOpenOutfit: () -> Unit, onNavigate: (SettingsPage) -> Unit) {
    val s = LocalStrings.current
    val st = vm.settings
    val context = LocalContext.current
    val version = remember { appVersionName(context) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(s.settingsTitle, style = MaterialTheme.typography.headlineMedium)
        ProfileCard(vm, onOpenOutfit, onOpenRewards = { onNavigate(SettingsPage.REWARDS) })

        SettingsGroup(s.groupGeneral) {
            SettingsRow(
                s.sectionLanguage, st.language.label, Icons.Filled.Language, Blue,
                onClick = { onNavigate(SettingsPage.LANGUAGE) },
            )
            SettingsDivider()
            SettingsRow(
                s.rowNotifications,
                s.notifSummary(st.notificationsEnabled, if (st.questReminder) s.chatTime(minutesToTime(st.questReminderTime)) else null),
                Icons.Filled.Notifications, Coral,
                onClick = { onNavigate(SettingsPage.NOTIFICATIONS) },
            )
        }
        SettingsGroup(s.groupSocial) {
            SettingsRow(
                s.rowFriends, s.friendsSummary(vm.visibleFriends.size, vm.blockedFriends.size), Icons.Filled.Group, Mint,
                onClick = { onNavigate(SettingsPage.FRIENDS) },
            )
            SettingsDivider()
            SettingsRow(
                s.tabMessages, s.messagesSummary(s.fontSizeLabel(st.chatFontSize), st.autoReplies), Icons.AutoMirrored.Filled.Chat, Violet,
                onClick = { onNavigate(SettingsPage.MESSAGES) },
            )
        }
        SettingsGroup(s.groupActivity) {
            SettingsRow(
                s.sectionQuests, s.questsSummary(st.autoCarryOver, st.weekStartsMonday), Icons.Filled.TaskAlt, Gold,
                onClick = { onNavigate(SettingsPage.QUESTS) },
            )
            SettingsDivider()
            SettingsRow(
                s.rowWidget, s.widgetRowDesc, Icons.Filled.Widgets, Mint,
                onClick = { if (!TodayWidget.requestPin(context)) vm.showMessage(s.msgWidgetUnsupported) },
            )
        }
        SettingsGroup(s.groupMore) {
            SettingsRow(
                s.sectionData, s.dataSummary(vm.recordedDays, vm.totalQuests), Icons.Filled.Storage, Teal,
                onClick = { onNavigate(SettingsPage.DATA) },
            )
            SettingsDivider()
            SettingsRow(
                s.sectionAbout, s.versionSummary(version), Icons.Filled.Info, Slate,
                onClick = { onNavigate(SettingsPage.ABOUT) },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ProfileCard(vm: TodayViewModel, onOpenOutfit: () -> Unit, onOpenRewards: () -> Unit) {
    val s = LocalStrings.current
    val c = vm.character
    var editingName by remember { mutableStateOf(false) }
    val xpProgress by animateFloatAsState(c.progress, tween(600), label = "xp")

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF3A2F6B), MaterialTheme.colorScheme.surface)))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                PixelAvatar(c.outfit, Modifier.height(72.dp), mood = vm.mood)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { editingName = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(vm.settings.nickname, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Edit, contentDescription = s.editNickname, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
                Text(s.titleLabel(vm.rewards.equippedTitle), color = Gold, fontSize = 12.sp)
                Text(s.outfitName(c.outfit.name), color = Violet, fontFamily = PixelFont, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("LV ${c.level}", fontFamily = PixelFont, fontWeight = FontWeight.Bold, color = Gold)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) { PixelProgressBar(xpProgress) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Stat("🔥", s.days(vm.streak), s.statStreak)
            Stat("✅", "${vm.totalCompleted}", s.statCompleted)
            Stat("🗓️", s.days(vm.recordedDays), s.statDays)
        }
        Spacer(Modifier.height(12.dp))
        RewardStrip(vm, onOpenRewards)
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onOpenOutfit, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Checkroom, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.customizeOutfit)
        }
    }

    if (editingName) {
        var name by remember { mutableStateOf(vm.settings.nickname) }
        AlertDialog(
            onDismissRequest = { editingName = false },
            title = { Text(s.editNickname) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(TodayViewModel.MAX_NICKNAME) },
                    singleLine = true,
                    label = { Text(s.nicknameLabel(TodayViewModel.MAX_NICKNAME)) },
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.updateNickname(name); editingName = false }, enabled = name.isNotBlank()) { Text(s.save) }
            },
            dismissButton = { TextButton(onClick = { editingName = false }) { Text(s.cancel) } },
        )
    }
}

@Composable
private fun Stat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(value, fontFamily = PixelFont, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun LanguagePage(vm: TodayViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    SettingsSubPage(s.sectionLanguage, onBack) {
        SettingsGroup(footer = s.languageHint) {
            AppLanguage.entries.forEachIndexed { i, lang ->
                if (i > 0) SettingsDivider()
                SettingsRow(
                    title = lang.label,
                    subtitle = s.languageName(lang),
                    icon = Icons.Filled.Language,
                    iconColor = Blue,
                    onClick = { vm.updateLanguage(lang) },
                    trailing = { RadioButton(selected = vm.settings.language == lang, onClick = { vm.updateLanguage(lang) }) },
                )
            }
        }
    }
}

fun minutesToTime(minutes: Int): LocalTime = LocalTime.of(minutes / 60, minutes % 60)
