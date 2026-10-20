package com.today.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class StatsTest {

    // 2026-10-20 is a Tuesday.
    private val today = LocalDate.of(2026, 10, 20)
    private var n = 0
    private fun q(title: String, done: Boolean, xp: Int = 20, days: Set<DayOfWeek> = emptySet()) =
        Quest("${n++}", title, "⭐", xp, done, days)

    @Test
    fun restDayQuestsDoNotCount() {
        val gym = q("Gym", done = false, days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val read = q("Read", done = true)
        val tuesday = listOf(gym, read)
        assertFalse(gym.activeOn(today))
        assertTrue(gym.activeOn(today.plusDays(1)))
        assertEquals(listOf(read), tuesday.activeOn(today))
        // Gym rests on Tuesday, so finishing Read completes the day.
        assertTrue(tuesday.completeOn(today))
        assertFalse(tuesday.completeOn(today.plusDays(1)))
        assertTrue(listOf(gym).isRestDayOn(today))
        assertFalse(emptyList<Quest>().isRestDayOn(today))
        assertFalse(emptyList<Quest>().completeOn(today))
    }

    @Test
    fun dayStatsAndHeatLevels() {
        val byDate = mapOf(
            today.minusDays(2).toString() to listOf(q("A", true), q("B", true)),
            today.minusDays(1).toString() to listOf(q("A", true), q("B", false), q("C", false)),
            today.toString() to listOf(q("A", false), q("Gym", true, days = setOf(DayOfWeek.MONDAY))),
        )
        val week = Stats.lastDays(byDate, today, 3)
        assertEquals(listOf(2, 1, 0), week.map { it.done })
        // The Monday-only quest rests on Tuesday: not counted even though it's checked.
        assertEquals(listOf(2, 3, 1), week.map { it.total })
        assertEquals(listOf(4, 1, 0), week.map(Stats::heatLevel))
        assertEquals(0, Stats.heatLevel(DayStat(today, 0, 0)))
        assertEquals(3, Stats.heatLevel(DayStat(today, 2, 3)))
        assertEquals(3f / 6f, Stats.rate(week)!!, 0.0001f)
        assertNull(Stats.rate(listOf(DayStat(today, 0, 0))))
    }

    @Test
    fun weekStartFollowsSetting() {
        assertEquals(LocalDate.of(2026, 10, 18), Stats.weekStart(today, mondayFirst = false)) // Sunday
        assertEquals(LocalDate.of(2026, 10, 19), Stats.weekStart(today, mondayFirst = true)) // Monday
        val sunday = LocalDate.of(2026, 10, 18)
        assertEquals(sunday, Stats.weekStart(sunday, mondayFirst = false))
        assertEquals(LocalDate.of(2026, 10, 12), Stats.weekStart(sunday, mondayFirst = true))
    }

    @Test
    fun monthDaysStopAtToday() {
        val byDate = mapOf(today.toString() to listOf(q("A", true)))
        assertEquals(20, Stats.monthDays(byDate, YearMonth.of(2026, 10), today).size)
        assertEquals(30, Stats.monthDays(byDate, YearMonth.of(2026, 9), today).size)
        assertTrue(Stats.monthDays(byDate, YearMonth.of(2026, 11), today).isEmpty())
    }

    @Test
    fun questRatesUsePastDaysOnly() {
        val byDate = mapOf(
            today.minusDays(3).toString() to listOf(q("Read", true), q("Run", false)),
            today.minusDays(2).toString() to listOf(q("Read", true), q("Run", true)),
            today.minusDays(1).toString() to listOf(q("Read", false), q("Run", false)),
            today.toString() to listOf(q("Read", true), q("Run", true)),
        )
        val rates = Stats.questRates(byDate, today)
        assertEquals(listOf("Read", "Run"), rates.map { it.title })
        assertEquals(2, rates[0].done)
        assertEquals(3, rates[0].total)
        assertEquals(1, rates[1].done)
        assertTrue(Stats.questRates(emptyMap(), today).isEmpty())
    }

    @Test
    fun bestWeekdayNeedsEnoughData() {
        val two = mapOf(
            today.minusDays(1).toString() to listOf(q("A", true)),
            today.minusDays(2).toString() to listOf(q("A", false)),
        )
        assertNull(Stats.bestWeekday(two, today))
        val more = two + (today.minusDays(3).toString() to listOf(q("A", false)))
        val (day, avg) = Stats.bestWeekday(more, today)!!
        assertEquals(today.minusDays(1).dayOfWeek, day)
        assertEquals(1f, avg, 0.0001f)
    }

    @Test
    fun totalXpCountsCompletedQuests() {
        val byDate = mapOf(
            "2026-10-18" to listOf(q("A", true, xp = 30), q("B", false, xp = 25)),
            "2026-10-19" to listOf(q("C", true, xp = 5)),
        )
        assertEquals(35, Stats.totalXp(byDate))
    }

    @Test
    fun restDaysBridgeStreaks() {
        // Done Sun 18, Mon 19 was a planned rest day, done today: rest days bridge without counting.
        val complete = setOf(today.minusDays(2), today)
        val rest = setOf(today.minusDays(1))
        assertEquals(2, Streaks.current(today, { it in complete }, { it in rest }))
    }

    @Test
    fun moodFollowsTodayAndYesterday() {
        val y = today.minusDays(1).toString()
        val t = today.toString()
        val missed = mapOf(y to listOf(q("A", false)), t to listOf(q("A", false), q("B", false)))
        assertEquals(Mood.SAD, Moods.of(missed, emptySet(), today))
        // A shielded yesterday isn't a miss: just sleepy before starting.
        assertEquals(Mood.SLEEPY, Moods.of(missed, setOf(y), today))
        val started = missed + (t to listOf(q("A", true), q("B", false)))
        assertEquals(Mood.NORMAL, Moods.of(started, emptySet(), today))
        val done = missed + (t to listOf(q("A", true), q("B", true)))
        assertEquals(Mood.HAPPY, Moods.of(done, emptySet(), today))
        assertEquals(Mood.NORMAL, Moods.of(emptyMap(), emptySet(), today))
    }

    @Test
    fun widgetPreviewCarriesOverUnchecked() {
        val saved = SavedState(
            character = CharacterState(),
            questsByDate = mapOf(
                today.minusDays(3).toString() to listOf(q("Old", true)),
                today.minusDays(1).toString() to listOf(q("Read", true), q("Run", false)),
            ),
            settings = AppSettings(),
            friends = emptyList(),
            chats = null,
        )
        assertEquals(listOf("Read", "Run"), saved.questsOn(today).map { it.title })
        assertTrue(saved.questsOn(today).none { it.done })
        // Saved lists win; carry-over off means nothing for a new day.
        assertEquals(listOf(true, false), saved.questsOn(today.minusDays(1)).map { it.done })
        assertTrue(saved.copy(settings = AppSettings(autoCarryOver = false)).questsOn(today).isEmpty())
    }

    @Test
    fun streakFromSavedData() {
        val byDate = mapOf(
            today.minusDays(3).toString() to listOf(q("A", true)),
            today.minusDays(2).toString() to listOf(q("A", false)),
            today.minusDays(1).toString() to listOf(q("A", true)),
        )
        assertEquals(1, Streaks.currentFor(byDate, emptySet(), today))
        assertEquals(2, Streaks.currentFor(byDate, setOf(today.minusDays(2).toString()), today))
        // Ending exactly on a missed day is 0 (nothing for a shield to keep).
        assertEquals(0, Streaks.endingAtFor(byDate, emptySet(), today.minusDays(2)))
        assertEquals(1, Streaks.endingAtFor(byDate, emptySet(), today.minusDays(1)))
    }
}
