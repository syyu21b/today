package com.today.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.today.app.data.DayStatus
import com.today.app.ui.theme.Coral
import com.today.app.ui.theme.Gold
import com.today.app.ui.theme.Mint
import com.today.app.ui.theme.TextMuted
import com.today.app.ui.theme.Violet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private fun statusColor(s: DayStatus): Color? = when (s) {
    DayStatus.EMPTY -> null
    DayStatus.PLANNED -> Violet
    DayStatus.PARTIAL -> Gold
    DayStatus.COMPLETE -> Mint
    DayStatus.MISSED -> Coral
}

/** Sunday-first week containing [date]. */
private fun weekOf(date: LocalDate): List<LocalDate> {
    val sunday = date.minusDays((date.dayOfWeek.value % 7).toLong())
    return List(7) { sunday.plusDays(it.toLong()) }
}

private fun dayLabel(d: DayOfWeek) = d.getDisplayName(TextStyle.SHORT, Locale.KOREAN)

/** Week strip with arrows; tapping the month title opens the full month view. */
@Composable
fun WeekCalendar(
    selected: LocalDate,
    today: LocalDate,
    statusOf: (LocalDate) -> DayStatus,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMonth by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onSelect(selected.minusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 주")
            }
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showMonth = true }
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CalendarMonth, null, tint = Violet, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("${selected.year}년 ${selected.monthValue}월", fontWeight = FontWeight.Bold)
            }
            if (selected != today) {
                TextButton(onClick = { onSelect(today) }) { Text("오늘") }
            }
            IconButton(onClick = { onSelect(selected.plusWeeks(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 주")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weekOf(selected).forEach { day ->
                DayCell(
                    day = day,
                    weekday = dayLabel(day.dayOfWeek),
                    isSelected = day == selected,
                    isToday = day == today,
                    status = statusOf(day),
                    onClick = { onSelect(day) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showMonth) {
        MonthCalendarDialog(
            initialMonth = YearMonth.from(selected),
            selected = selected,
            today = today,
            statusOf = statusOf,
            onSelect = { onSelect(it); showMonth = false },
            onDismiss = { showMonth = false },
        )
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    weekday: String?,
    isSelected: Boolean,
    isToday: Boolean,
    status: DayStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val weekendColor = when (day.dayOfWeek) {
        DayOfWeek.SUNDAY -> Coral
        DayOfWeek.SATURDAY -> Color(0xFF6FA8FF)
        else -> Color.White
    }
    Column(
        modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Violet else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, Violet, RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (weekday != null) {
            Text(weekday, fontSize = 11.sp, color = if (isSelected) Color.White else weekendColor.copy(alpha = 0.8f))
        }
        Text(
            "${day.dayOfMonth}",
            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isSelected -> Color.White
                dimmed -> TextMuted.copy(alpha = 0.5f)
                else -> weekendColor
            },
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor(status)?.let { if (isSelected) Color.White else it } ?: Color.Transparent),
        )
    }
}

@Composable
private fun MonthCalendarDialog(
    initialMonth: YearMonth,
    selected: LocalDate,
    today: LocalDate,
    statusOf: (LocalDate) -> DayStatus,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var month by remember { mutableStateOf(initialMonth) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "이전 달")
                    }
                    Text(
                        "${month.year}년 ${month.monthValue}월",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "다음 달")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
                        .forEach {
                            Text(dayLabel(it), fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        }
                }
                Spacer(Modifier.height(4.dp))
                val first = month.atDay(1)
                val leading = first.dayOfWeek.value % 7
                val cells = leading + month.lengthOfMonth()
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val index = r * 7 + c - leading
                            if (index < 0 || index >= month.lengthOfMonth()) {
                                Spacer(Modifier.weight(1f).aspectRatio(0.8f))
                            } else {
                                val day = month.atDay(index + 1)
                                DayCell(
                                    day = day,
                                    weekday = null,
                                    isSelected = day == selected,
                                    isToday = day == today,
                                    status = statusOf(day),
                                    onClick = { onSelect(day) },
                                    modifier = Modifier.weight(1f).aspectRatio(0.8f),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Legend()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onSelect(today) }) { Text("오늘로 이동") }
                    TextButton(onClick = onDismiss) { Text("닫기") }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(DayStatus.COMPLETE to "완료", DayStatus.PARTIAL to "진행 중", DayStatus.PLANNED to "예정", DayStatus.MISSED to "미완료").forEach { (s, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(statusColor(s)!!))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}
