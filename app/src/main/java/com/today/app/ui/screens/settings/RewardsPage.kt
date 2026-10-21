package com.today.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.today.app.data.Accessories
import com.today.app.data.RewardEngine
import com.today.app.data.Titles
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.components.PixelAvatar
import com.today.app.ui.screens.AccessoriesCard
import com.today.app.ui.screens.BoxDialog
import com.today.app.ui.screens.ShieldDialog
import com.today.app.ui.screens.TitleRow
import com.today.app.ui.screens.silhouette
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.LocalTodayColors
import com.today.app.ui.theme.Hairline
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.Sky
import com.today.app.ui.theme.TextMuted
import java.time.LocalDate

/** Profile card strip: every reward count at a glance; tap for the full page. */
@Composable
fun RewardStrip(vm: TodayViewModel, onOpen: () -> Unit) {
    val s = LocalStrings.current
    val r = vm.rewards
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Hairline.copy(alpha = if (LocalTodayColors.current.isDark) 0.12f else 0.06f))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.rewardsTitle, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Chevron()
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MiniStat("🛡", "${r.shields}", s.statShields)
            MiniStat("🎁", "${r.boxes}", s.statBoxes)
            MiniStat("✨", "${r.stardust}", s.statStardust)
            MiniStat("🏅", "${r.titles.size}/${Titles.all.size}", s.statTitles)
            MiniStat("🧢", "${Accessories.all.count(vm::isUnlocked)}/${Accessories.all.size}", s.statItems)
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 16.sp)
        Text(value, fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, fontSize = 10.sp, color = TextMuted, maxLines = 1)
    }
}

/** Everything reward-related on one page: inventory, next unlock, titles, accessories, shielded days. */
@Composable
fun RewardsPage(vm: TodayViewModel, onBack: () -> Unit) {
    val s = LocalStrings.current
    val r = vm.rewards
    var showShield by remember { mutableStateOf(false) }

    SettingsSubPage(s.rewardsTitle, onBack) {
        SettingsGroup {
            SettingsRow(
                "🛡 ${s.shieldName}", s.shieldCount(r.shields),
                onClick = { showShield = true },
            )
            SettingsDivider()
            SettingsRow(
                s.boxTitle, s.boxesLeft(r.boxes),
                trailing = {
                    TextButton(onClick = vm::openBox, enabled = r.boxes > 0) { Text(s.openBox, color = if (r.boxes > 0) Gold else TextMuted) }
                },
            )
            SettingsDivider()
            SettingsRow(
                "✨ ${s.statStardust}", s.stardustProgress(r.stardust, RewardEngine.SHIELD_PRICE),
                trailing = {
                    TextButton(onClick = vm::exchangeStardust, enabled = r.stardust >= RewardEngine.SHIELD_PRICE) { Text(s.exchange) }
                },
            )
        }

        Accessories.nextLevelUnlock(r.rewardedLevel)?.let { next ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelAvatar(vm.character.outfit.withAccessory(next.slot, next.id), Modifier.height(56.dp).silhouette(), animateColors = false)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("LV ${vm.character.level}", fontFamily = PixelFont, color = Gold, fontWeight = FontWeight.Bold)
                    Text(s.nextUnlock(next), color = Sky, fontSize = 13.sp)
                }
            }
        }

        SettingsGroup(s.titleCollection(r.titles.size, Titles.all.size)) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Titles.all.forEach { TitleRow(vm, it, background = Color.Transparent) }
            }
        }

        AccessoriesCard(vm)

        SettingsGroup(s.shieldedDaysTitle) {
            val days = r.shieldedDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sortedDescending()
            if (days.isEmpty()) {
                Text(s.noShieldedDays, color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
            } else {
                days.forEachIndexed { i, d ->
                    if (i > 0) SettingsDivider()
                    SettingsRow("🛡 ${s.shortDate(d)}", titleColor = Sky)
                }
            }
        }
    }

    if (showShield) ShieldDialog(vm, onDismiss = { showShield = false })
    vm.boxResult?.let { BoxDialog(vm, it) }
}
