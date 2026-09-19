package com.thehan.dailyspeak.core.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderTimeCalculator {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val timePattern = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

    fun nextTriggerMillis(
        nowMillis: Long,
        reminderTime: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (!timePattern.matches(reminderTime)) return null

        val targetTime = runCatching {
            LocalTime.parse(reminderTime, timeFormatter)
        }.getOrNull() ?: return null

        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val nextTrigger = now.toLocalDate()
            .atTime(targetTime)
            .atZone(zoneId)
            .let { candidate ->
                if (candidate.toInstant().toEpochMilli() > nowMillis) {
                    candidate
                } else {
                    candidate.plusDays(1)
                }
            }

        return nextTrigger.toInstant().toEpochMilli()
    }
}
