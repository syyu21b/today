package com.today.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.Accessories
import com.today.app.data.MAX_STATUS_MESSAGE
import com.today.app.data.MissionType
import com.today.app.data.Mood
import com.today.app.data.Outfit
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.unit.Dp
import com.today.app.data.RoutineTemplate
import com.today.app.data.SeasonEvents
import com.today.app.data.Templates
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Hairline
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.NightSurfaceHigh
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet

private fun MissionType.emoji() = when (this) {
    MissionType.COMPLETE_THREE -> "✅"
    MissionType.COMPLETE_ALL -> "🏁"
    MissionType.MORNING -> "🌤️"
    MissionType.PET -> "🐾"
    MissionType.CHAT -> "💬"
    MissionType.ADD_QUEST -> "➕"
    MissionType.STATUS -> "😊"
}

/** Home card: today's three missions with progress and a claim button each. */
@Composable
fun MissionsCard(vm: TodayViewModel) {
    val s = LocalStrings.current
    val missions = vm.todayMissions
    val claimed = missions.count { vm.isMissionClaimed(it) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯 " + s.missionsTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("$claimed / ${missions.size}", fontFamily = PixelFont, color = if (claimed == missions.size) Mint else TextMuted)
        }
        missions.forEach { m -> MissionRow(vm, m) }
        Text(s.missionsBonusHint(vm.activeEvent != null), color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun MissionRow(vm: TodayViewModel, m: MissionType) {
    val s = LocalStrings.current
    val goal = vm.missionGoal(m)
    val progress = vm.missionProgress(m)
    val done = vm.isMissionDone(m)
    val claimed = vm.isMissionClaimed(m)
    val fill by animateFloatAsState(progress / goal.toFloat(), label = "mission")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (claimed) Mint.copy(alpha = 0.10f) else Hairline.copy(alpha = 0.04f))
            .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(m.emoji(), fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(s.missionText(m, goal), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (claimed) TextMuted else Color.Unspecified)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(NightSurfaceHigh)) {
                Box(Modifier.fillMaxWidth(fill.coerceIn(0f, 1f)).fillMaxHeight().background(if (done) Mint else Violet))
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            claimed -> Icon(Icons.Filled.Check, contentDescription = s.missionClaimed, tint = Mint, modifier = Modifier.padding(horizontal = 14.dp).size(20.dp))
            done -> FilledTonalButton(
                onClick = { vm.claimMission(m) },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Gold.copy(alpha = 0.25f)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            ) { Text("${s.missionClaim} +${m.xp}", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.Bold) }
            else -> Text("$progress/$goal", fontFamily = PixelFont, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 12.dp))
        }
    }
}

/** Season event banner with progress and the limited item it gives. */
@Composable
fun EventBanner(vm: TodayViewModel) {
    val s = LocalStrings.current
    val event = vm.activeEvent ?: return
    val days = vm.eventDays
    val item = Accessories.byId(event.itemId) ?: return
    val owned = vm.isUnlocked(item)
    val end = event.lastDay(vm.todayDate)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Coral.copy(alpha = 0.12f))
            .border(1.dp, Coral.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Coral.copy(alpha = 0.15f)), contentAlignment = Alignment.TopCenter) {
            HeadCrop(vm.character.outfit.withAccessory(item.slot, item.id), 56.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(s.eventBanner(event, days, SeasonEvents.GOAL_DAYS), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${s.accessoryLabel(item.id)} · ${s.eventEnds(event, end.monthValue, end.dayOfMonth)}" + if (owned) " · ✅" else "",
                fontSize = 11.sp, color = TextMuted,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(SeasonEvents.GOAL_DAYS) { i ->
                    Box(Modifier.width(22.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (i < days) Coral else NightSurfaceHigh))
                }
            }
        }
    }
}

/** Mood grid (with "auto") + a short message. */
@Composable
fun StatusDialog(vm: TodayViewModel, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    var mood by remember { mutableStateOf(vm.settings.statusMood) }
    var message by remember { mutableStateOf(vm.settings.statusMessage) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.editStatus) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.statusTitle, fontSize = 13.sp, color = TextMuted)
                val options: List<Mood?> = listOf(null) + listOf(
                    Mood.HAPPY, Mood.EXCITED, Mood.CALM, Mood.LOVE, Mood.NORMAL, Mood.SLEEPY, Mood.SAD, Mood.ANGRY, Mood.SICK,
                )
                options.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { m ->
                            val on = m == mood
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (on) Violet.copy(alpha = 0.22f) else Hairline.copy(alpha = 0.04f))
                                    .border(2.dp, if (on) Violet else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { mood = m }
                                    .padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (m == null) Text("✨", fontSize = 20.sp) else PixelAvatarFace(vm, m)
                                Text(if (m == null) s.statusAuto else s.moodName(m), fontSize = 10.sp, maxLines = 1)
                            }
                        }
                        repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(MAX_STATUS_MESSAGE) },
                    label = { Text(s.statusMessageLabel) },
                    placeholder = { Text(s.statusEmpty) },
                    supportingText = { Text("${message.length} / $MAX_STATUS_MESSAGE") },
                    maxLines = 2,
                )
            }
        },
        confirmButton = { TextButton(onClick = { vm.updateStatus(mood, message); onDismiss() }) { Text(s.save) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
    )
}

/** The character's head with [mood], as a small mood swatch. */
@Composable
private fun PixelAvatarFace(vm: TodayViewModel, mood: Mood) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.TopCenter) {
        HeadCrop(vm.character.outfit.withoutAccessories(), 34.dp, mood)
    }
}

/** The character drawn big enough that only the head fills a [box]-sized square (top-aligned). */
@Composable
fun HeadCrop(outfit: Outfit, box: Dp, mood: Mood = Mood.NORMAL) {
    val h = box * 2f
    PixelAvatar(
        outfit,
        Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true).requiredSize(h * (2f / 3f), h),
        animateColors = false,
        mood = mood,
    )
}

/** Speech bubble with the status message (tap to edit). */
@Composable
fun StatusBubble(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 12.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp, 12.dp, 12.dp, 3.dp))
            .background(Violet.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Routine packs: tap one to add its quests to [date]. */
@Composable
fun TemplatesDialog(vm: TodayViewModel, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.templatesTitle) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(s.templatesHint, fontSize = 12.sp, color = TextMuted)
                Templates.all.forEach { t -> TemplateRow(t, selected = false) { vm.applyTemplate(vm.selectedDate, t); onDismiss() } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.close) } },
    )
}

/** One routine pack: emoji, name, description and its quests. */
@Composable
fun TemplateRow(t: RoutineTemplate, selected: Boolean, onClick: () -> Unit) {
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Violet.copy(alpha = 0.18f) else Hairline.copy(alpha = 0.04f))
            .border(2.dp, if (selected) Violet else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(t.emoji, fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(s.templateName(t.id), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(s.templateDesc(t.id), fontSize = 12.sp, color = TextMuted)
            Text(t.quests.joinToString(" ") { it.emoji }, fontSize = 13.sp)
        }
        if (selected) Icon(Icons.Filled.Check, null, tint = Violet)
    }
}
