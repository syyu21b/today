package com.today.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.Accessories
import com.today.app.data.Accessory
import com.today.app.data.BoxReward
import com.today.app.data.LevelUpEvent
import com.today.app.data.RewardEngine
import com.today.app.data.TitleDef
import com.today.app.data.Titles
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.CelebrationOverlay
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.Sky
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import kotlinx.coroutines.delay

/** Paints the content as a dark shape: "what's coming next" previews. */
fun Modifier.silhouette(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(Color(0xFF4A4466), blendMode = BlendMode.SrcAtop)
    }

/** Small rounded pill used on the character card (shields, title). */
@Composable
fun RewardPill(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 12.sp,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** Banner row: emoji, message, one action. */
@Composable
fun RewardBanner(emoji: String, text: String, action: String, color: Color, onAction: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(action, color = color, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun ShieldDialog(vm: TodayViewModel, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    val r = vm.rewards
    val today = vm.todayDate
    val yesterday = today.minusDays(1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🛡 ${s.shieldName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.shieldCount(r.shields), fontFamily = PixelFont, fontWeight = FontWeight.Bold, color = Sky, fontSize = 18.sp)
                Text(s.shieldDialogBody, color = TextMuted, fontSize = 14.sp)
                // Why the buttons are off: no streak yet, so a shield would be wasted.
                if (r.shields > 0 && vm.streak == 0 && !vm.canShield(yesterday)) {
                    Text(s.msgNoStreakToProtect, color = Gold, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.stardustLine(r.stardust, RewardEngine.SHIELD_PRICE), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::exchangeStardust, enabled = r.stardust >= RewardEngine.SHIELD_PRICE) { Text(s.exchange) }
                }
            }
        },
        confirmButton = {
            Row {
                if (vm.canShield(yesterday)) {
                    TextButton(onClick = { vm.useShield(yesterday); onDismiss() }) { Text(s.useShieldYesterday, color = Sky) }
                }
                TextButton(onClick = { vm.useShield(today); onDismiss() }, enabled = vm.canShield(today)) { Text(s.useShieldToday) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.close) } },
    )
}

@Composable
fun TitlePickerDialog(vm: TodayViewModel, onDismiss: () -> Unit) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.titlePickerTitle) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(Titles.all, key = { it.id }) { t -> TitleRow(vm, t) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(s.ok) } },
    )
}

/** One title: tap to wear it if owned; locked ones show how to earn them. */
@Composable
fun TitleRow(vm: TodayViewModel, t: TitleDef, background: Color = MaterialTheme.colorScheme.surface) {
    val s = LocalStrings.current
    val has = t.id in vm.rewards.titles
    val equipped = t.id == vm.rewards.equippedTitle
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (equipped) Gold.copy(alpha = 0.15f) else background)
            .clickable(enabled = has) { vm.equipTitle(t.id) }
            .alpha(if (has) 1f else 0.4f)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (has) t.emoji else "🔒", fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(s.titleName(t.id), fontWeight = FontWeight.SemiBold)
            Text(s.titleHow(t.id), fontSize = 12.sp, color = TextMuted)
        }
        if (equipped) Text(s.titleEquipped, fontSize = 12.sp, color = Gold)
    }
}

/** Level-up dialog: what this level gave and what the next one will. */
@Composable
fun LevelUpDialog(vm: TodayViewModel, event: LevelUpEvent) {
    val s = LocalStrings.current
    AlertDialog(
        onDismissRequest = vm::consumeLevelUp,
        title = { Text("⭐ LEVEL UP! ⭐", fontFamily = PixelFont, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PixelAvatar(vm.character.outfit, Modifier.height(120.dp))
                Spacer(Modifier.height(12.dp))
                Text(s.levelReached(event.level), fontFamily = PixelFont, fontSize = 22.sp, color = Gold)
                Text(s.levelUpBody, color = TextMuted, textAlign = TextAlign.Center)
                if (event.rewarded) {
                    Spacer(Modifier.height(10.dp))
                    Text(s.levelRewards, color = Sky, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Accessories.unlockedAt(event.level).forEach { Text(s.newUnlock(it), color = Mint, textAlign = TextAlign.Center) }
                }
                Accessories.nextLevelUnlock(vm.rewards.rewardedLevel)?.let { next ->
                    Spacer(Modifier.height(12.dp))
                    PixelAvatar(vm.character.outfit.withAccessory(next.slot, next.id), Modifier.height(56.dp).silhouette(), animateColors = false)
                    Text(s.nextUnlock(next), fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::consumeLevelUp) { Text(s.levelUpOk) } },
    )
}

/** Shaking box -> reveal. "Open another" replays the animation for the next box. */
@Composable
fun BoxDialog(vm: TodayViewModel, reward: BoxReward) {
    val s = LocalStrings.current
    var opening by remember { mutableStateOf(true) }
    var reveals by remember { mutableIntStateOf(0) }
    LaunchedEffect(opening) {
        if (opening) {
            delay(1200)
            opening = false
            reveals++
        }
    }
    val shake = rememberInfiniteTransition(label = "shake")
    val angle by shake.animateFloat(-12f, 12f, infiniteRepeatable(tween(90), RepeatMode.Reverse), label = "angle")
    val pop by androidx.compose.animation.core.animateFloatAsState(
        if (opening) 0.3f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pop",
    )

    AlertDialog(
        onDismissRequest = { if (!opening) vm.consumeBoxResult() },
        title = { Text(s.boxTitle) },
        text = {
            Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                if (opening) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎁", fontSize = 72.sp, modifier = Modifier.rotate(angle))
                        Spacer(Modifier.height(12.dp))
                        Text(s.boxOpening, color = TextMuted)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(pop)) {
                        RewardVisual(vm, reward)
                        Spacer(Modifier.height(12.dp))
                        Text(s.boxRewardText(reward), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (vm.settings.celebrationEffects) CelebrationOverlay(trigger = reveals, modifier = Modifier.matchParentSize())
            }
        },
        confirmButton = {
            if (!opening) {
                Row {
                    if (vm.rewards.boxes > 0) {
                        TextButton(onClick = { vm.openBox(); opening = true }) { Text(s.openAnother(vm.rewards.boxes), color = Violet) }
                    }
                    TextButton(onClick = vm::consumeBoxResult) { Text(s.ok) }
                }
            }
        },
    )
}

@Composable
private fun RewardVisual(vm: TodayViewModel, reward: BoxReward) {
    when (reward) {
        is BoxReward.Item -> {
            val acc = Accessories.byId(reward.id)
            if (acc != null && !reward.duplicate) {
                PixelAvatar(vm.character.outfit.withAccessory(acc.slot, acc.id), Modifier.height(110.dp), animateColors = false)
            } else {
                Text(acc?.emoji ?: "✨", fontSize = 64.sp)
            }
        }
        is BoxReward.Title -> Text(Titles.byId(reward.id)?.emoji ?: "🏅", fontSize = 64.sp)
        BoxReward.Shield -> Text("🛡️", fontSize = 64.sp)
        is BoxReward.Stardust -> Text("✨", fontSize = 64.sp)
    }
}

/** Wardrobe card: every accessory, locked ones as silhouettes with how to get them. */
@Composable
fun AccessoriesCard(vm: TodayViewModel) {
    val s = LocalStrings.current
    val outfit = vm.character.outfit
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(s.accessoriesTitle, style = MaterialTheme.typography.titleMedium)
        Text(s.accessoriesHint, color = TextMuted, fontSize = 12.sp)
        Accessories.nextLevelUnlock(vm.rewards.rewardedLevel)?.let { Text(s.nextUnlock(it), color = Sky, fontSize = 12.sp) }
        Accessories.all.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { acc ->
                    AccessoryTile(
                        acc = acc,
                        unlocked = vm.isUnlocked(acc),
                        worn = outfit.accessoryIn(acc.slot) == acc.id,
                        preview = { m -> PixelAvatar(outfit.withAccessory(acc.slot, acc.id), m, animateColors = false) },
                        onClick = { vm.toggleAccessory(acc) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AccessoryTile(
    acc: Accessory,
    unlocked: Boolean,
    worn: Boolean,
    preview: @Composable (Modifier) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (worn) Gold.copy(alpha = 0.12f) else Color.Transparent)
            .border(2.dp, if (worn) Gold else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            preview(if (unlocked) Modifier.fillMaxHeight() else Modifier.fillMaxHeight().silhouette())
            if (!unlocked) Text("🔒", fontSize = 16.sp)
        }
        Text(
            if (unlocked) s.accessoryName(acc.id) else s.accessoryLock(acc),
            fontSize = 11.sp, maxLines = 1,
            color = if (worn) Gold else if (unlocked) Color.White else TextMuted,
            fontWeight = if (worn) FontWeight.Bold else null,
        )
    }
}
