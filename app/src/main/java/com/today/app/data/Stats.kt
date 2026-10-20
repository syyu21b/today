package com.today.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** One day's scheduled quests: [done] of [total] (rest-day quests don't count). */
data class DayStat(val date: LocalDate, val done: Int, val total: Int) {
    val ratio: Float get() = if (total == 0) 0f else done.toFloat() / total
}

/** How often a quest (grouped by title) was done on the days it was scheduled. */
data class QuestRate(val title: String, val emoji: String, val done: Int, val total: Int) {
    val ratio: Float get() = if (total == 0) 0f else done.toFloat() / total
}

/** Pure statistics over ISO date -> quests (no Android), so they're unit-tested directly. */
object Stats {
    fun day(byDate: Map<String, List<Quest>>, date: LocalDate): DayStat {
        val active = byDate[date.toString()].orEmpty().activeOn(date)
        return DayStat(date, active.count { it.done }, active.size)
    }

    /** [count] days ending at [end], oldest first. */
    fun lastDays(byDate: Map<String, List<Quest>>, end: LocalDate, count: Int): List<DayStat> =
        (count - 1 downTo 0).map { day(byDate, end.minusDays(it.toLong())) }

    /** Heatmap shade 0..4 (0 = nothing done or nothing scheduled). */
    fun heatLevel(d: DayStat): Int = when {
        d.total == 0 || d.done == 0 -> 0
        // Integer math so thirds land exactly: up to 1/3 -> 1, under 2/3 -> 2, 2/3 or more -> 3.
        d.done * 3 <= d.total -> 1
        d.done * 3 < d.total * 2 -> 2
        d.done < d.total -> 3
        else -> 4
    }

    /** First day of [date]'s week. */
    fun weekStart(date: LocalDate, mondayFirst: Boolean): LocalDate {
        val first = if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
        return date.minusDays(((date.dayOfWeek.value - first.value + 7) % 7).toLong())
    }

    /** Completion rate (0..1) over [days], or null when nothing was scheduled. */
    fun rate(days: List<DayStat>): Float? {
        val total = days.sumOf { it.total }
        return if (total == 0) null else days.sumOf { it.done }.toFloat() / total
    }

    /** Days of [month] up to [today] (a future month is empty). */
    fun monthDays(byDate: Map<String, List<Quest>>, month: YearMonth, today: LocalDate): List<DayStat> {
        val last = minOf(month.atEndOfMonth(), today)
        val first = month.atDay(1)
        if (last.isBefore(first)) return emptyList()
        return generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }.map { day(byDate, it) }.toList()
    }

    /** Past days with something scheduled (today is still in progress, so it's left out). */
    private fun pastDays(byDate: Map<String, List<Quest>>, today: LocalDate): List<DayStat> =
        byDate.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .filter { it.isBefore(today) }
            .map { day(byDate, it) }
            .filter { it.total > 0 }

    /** Weekday with the best average completion, once there are 3+ past days over 2+ weekdays. */
    fun bestWeekday(byDate: Map<String, List<Quest>>, today: LocalDate): Pair<DayOfWeek, Float>? {
        val past = pastDays(byDate, today)
        val byWeekday = past.groupBy { it.date.dayOfWeek }
        if (past.size < 3 || byWeekday.size < 2) return null
        return byWeekday.mapValues { (_, days) -> days.map { it.ratio }.average().toFloat() }
            .maxByOrNull { it.value }
            ?.let { it.key to it.value }
    }

    /** Success rate per quest title over past days, best first. */
    fun questRates(byDate: Map<String, List<Quest>>, today: LocalDate): List<QuestRate> {
        val counts = linkedMapOf<String, QuestRate>()
        byDate.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .filter { it.isBefore(today) }
            .sorted()
            .forEach { date ->
                byDate[date.toString()].orEmpty().activeOn(date).forEach { q ->
                    val old = counts[q.title] ?: QuestRate(q.title, q.emoji, 0, 0)
                    // Latest emoji wins, so an edited quest shows its current look.
                    counts[q.title] = QuestRate(q.title, q.emoji, old.done + if (q.done) 1 else 0, old.total + 1)
                }
            }
        return counts.values.sortedWith(compareByDescending<QuestRate> { it.ratio }.thenByDescending { it.total })
    }

    /** XP from every completed quest ever (independent of level-downs). */
    fun totalXp(byDate: Map<String, List<Quest>>): Int = byDate.values.sumOf { list -> list.filter { it.done }.sumOf { it.xp } }
}
