package com.today.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.today.app.data.DayStat
import com.today.app.data.Stats
import com.today.app.data.TodayViewModel
import com.today.app.i18n.LocalStrings
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.NightSurfaceHigh
import com.today.app.ui.theme.PixelFont
import com.today.app.ui.theme.Sky
import com.today.app.ui.theme.TextMuted
import java.time.YearMonth
import kotlin.math.roundToInt

private const val WEEKS = 12

/** One hue (mint), dim -> bright with the share of the day done; 0 = the empty track. */
private fun heatColor(level: Int): Color = when (level) {
    0 -> NightSurfaceHigh
    1 -> Mint.copy(alpha = 0.3f)
    2 -> Mint.copy(alpha = 0.5f)
    3 -> Mint.copy(alpha = 0.75f)
    else -> Mint
}

private fun pct(ratio: Float) = (ratio * 100).roundToInt()

@Composable
fun StatsScreen(vm: TodayViewModel, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val today = vm.todayDate
    val history = vm.questHistory

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(s.tabStats, style = MaterialTheme.typography.headlineMedium)
        Text(s.statsIntro, color = TextMuted, fontSize = 14.sp)

        // ---- Headline numbers ----
        val month = YearMonth.from(today)
        val thisRate = Stats.rate(Stats.monthDays(history, month, today))
        val lastRate = Stats.rate(Stats.monthDays(history, month.minusMonths(1), today))
        val delta = if (thisRate != null && lastRate != null) pct(thisRate) - pct(lastRate) else null
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("🔥", s.days(vm.streak), s.statCurrentStreak, Modifier.weight(1f))
            StatTile("🏆", s.days(vm.bestStreak), s.statBestStreak, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("⭐", "${Stats.totalXp(history)}", s.statTotalXp, Modifier.weight(1f))
            StatTile("✅", thisRate?.let { "${pct(it)}%" } ?: "—", s.statMonthRate, Modifier.weight(1f), sub = s.monthDelta(delta))
        }

        Heatmap(vm)
        WeekBars(vm)
        QuestRates(vm)
        Text(s.statsTapHint, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
    }
}

@Composable
private fun StatTile(emoji: String, value: String, label: String, modifier: Modifier = Modifier, sub: String? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(value, fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(label, color = TextMuted, fontSize = 12.sp)
        if (sub != null) Text(sub, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        },
    )
}

/** Tapped day's numbers, or a placeholder line so the card doesn't jump. */
@Composable
private fun DetailLine(day: DayStat?) {
    val s = LocalStrings.current
    Text(
        day?.let { s.dayDetail(it.date, it.done, it.total) } ?: " ",
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun Heatmap(vm: TodayViewModel) {
    val s = LocalStrings.current
    val today = vm.todayDate
    val mondayFirst = vm.settings.weekStartsMonday
    val start = Stats.weekStart(today, mondayFirst).minusWeeks((WEEKS - 1).toLong())
    var selected by remember { mutableStateOf<DayStat?>(null) }

    StatCard(s.heatmapTitle) {
        DetailLine(selected)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val labelWidth = 22.dp
            val gap = 3.dp
            val cell = (maxWidth - labelWidth - gap * WEEKS) / WEEKS
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(7) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        // Label every other row to keep it airy.
                        Text(
                            if (row % 2 == 0) s.weekdayShort(start.plusDays(row.toLong()).dayOfWeek) else "",
                            fontSize = 10.sp, color = TextMuted, modifier = Modifier.width(labelWidth),
                        )
                        repeat(WEEKS) { week ->
                            val date = start.plusDays((week * 7 + row).toLong())
                            if (date.isAfter(today)) {
                                Spacer(Modifier.size(cell))
                            } else {
                                val day = Stats.day(vm.questHistory, date)
                                val shielded = vm.isShielded(date) && day.done < day.total
                                Box(
                                    Modifier
                                        .size(cell)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (shielded) Sky.copy(alpha = 0.7f) else heatColor(Stats.heatLevel(day)))
                                        .then(if (selected?.date == date) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(4.dp)) else Modifier)
                                        .clickable { selected = day },
                                )
                            }
                        }
                    }
                }
            }
        }
        // Legend: the sequential ramp plus the one special state.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(s.heatLess, fontSize = 11.sp, color = TextMuted)
            (0..4).forEach { Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(heatColor(it))) }
            Text(s.heatMore, fontSize = 11.sp, color = TextMuted)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Sky.copy(alpha = 0.7f)))
            Text(s.legendShielded, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
private fun WeekBars(vm: TodayViewModel) {
    val s = LocalStrings.current
    val days = Stats.lastDays(vm.questHistory, vm.todayDate, 7)
    val scale = maxOf(1, days.maxOf { it.total })
    var selected by remember { mutableStateOf<DayStat?>(null) }

    StatCard(s.weekChartTitle) {
        DetailLine(selected)
        Row(
            Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                val isToday = day.date == vm.todayDate
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selected = day }
                        .then(if (selected?.date == day.date) Modifier.background(Color.White.copy(alpha = 0.06f)) else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text("${day.done}", fontSize = 11.sp, fontFamily = PixelFont, color = if (day.done > 0) Color.White else TextMuted)
                    Spacer(Modifier.height(4.dp))
                    // Track = what was scheduled, fill = what got done (both on one scale).
                    Box(
                        Modifier
                            .width(18.dp)
                            .weight(1f, fill = false)
                            .fillMaxHeight(0.86f * day.total / scale + 0.02f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(NightSurfaceHigh),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (day.done > 0) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(day.done.toFloat() / maxOf(1, day.total))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Mint),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        s.weekdayShort(day.date.dayOfWeek),
                        fontSize = 11.sp,
                        color = if (isToday) Color.White else TextMuted,
                        fontWeight = if (isToday) FontWeight.Bold else null,
                    )
                }
            }
        }
        Stats.bestWeekday(vm.questHistory, vm.todayDate)?.let { (day, avg) ->
            Text(s.bestWeekday(day, pct(avg)), fontSize = 13.sp)
        }
    }
}

@Composable
private fun QuestRates(vm: TodayViewModel) {
    val s = LocalStrings.current
    val rates = Stats.questRates(vm.questHistory, vm.todayDate).take(8)
    StatCard(s.questRatesTitle) {
        if (rates.isEmpty()) {
            Text(s.questRatesEmpty, color = TextMuted, fontSize = 13.sp)
        }
        rates.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(r.title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .width(80.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NightSurfaceHigh),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(r.ratio).clip(RoundedCornerShape(4.dp)).background(Mint))
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(44.dp)) {
                    Text("${pct(r.ratio)}%", fontSize = 13.sp, fontFamily = PixelFont, fontWeight = FontWeight.Bold)
                    Text("${r.done}/${r.total}", fontSize = 10.sp, color = TextMuted)
                }
            }
        }
    }
}
