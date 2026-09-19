package com.thehan.dailyspeak.core.reminder

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderTimeCalculatorTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `schedules later today when target time is still ahead`() {
        val now = localMillis(2026, 9, 19, 19, 30)
        val expected = localMillis(2026, 9, 19, 20, 0)

        assertEquals(expected, ReminderTimeCalculator.nextTriggerMillis(now, "20:00", zoneId))
    }

    @Test
    fun `schedules tomorrow when target time has passed`() {
        val now = localMillis(2026, 9, 19, 21, 0)
        val expected = localMillis(2026, 9, 20, 20, 0)

        assertEquals(expected, ReminderTimeCalculator.nextTriggerMillis(now, "20:00", zoneId))
    }

    @Test
    fun `schedules tomorrow when current time is exactly target time`() {
        val now = localMillis(2026, 9, 19, 20, 0)
        val expected = localMillis(2026, 9, 20, 20, 0)

        assertEquals(expected, ReminderTimeCalculator.nextTriggerMillis(now, "20:00", zoneId))
    }

    @Test
    fun `returns null for an invalid reminder time`() {
        val now = localMillis(2026, 9, 19, 12, 0)

        assertNull(ReminderTimeCalculator.nextTriggerMillis(now, "8:00", zoneId))
        assertNull(ReminderTimeCalculator.nextTriggerMillis(now, "24:00", zoneId))
    }

    private fun localMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}
