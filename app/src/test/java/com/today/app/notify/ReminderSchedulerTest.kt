package com.today.app.notify

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderSchedulerTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 10, 19, h, m, 0, 0, zone)
    private fun millis(t: ZonedDateTime) = t.toInstant().toEpochMilli()

    @Test
    fun laterToday() {
        assertEquals(millis(at(9, 0)), ReminderScheduler.nextTrigger(9 * 60, at(7, 30)))
    }

    @Test
    fun alreadyPassedMeansTomorrow() {
        assertEquals(millis(at(9, 0).plusDays(1)), ReminderScheduler.nextTrigger(9 * 60, at(9, 0)))
        assertEquals(millis(at(21, 0).plusDays(1)), ReminderScheduler.nextTrigger(21 * 60, at(23, 59)))
    }

    @Test
    fun midnightAndLastMinute() {
        assertEquals(millis(at(0, 0).plusDays(1)), ReminderScheduler.nextTrigger(0, at(12, 0)))
        assertEquals(millis(at(23, 59)), ReminderScheduler.nextTrigger(23 * 60 + 59, at(12, 0)))
    }
}
