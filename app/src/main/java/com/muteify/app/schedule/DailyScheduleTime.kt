package com.muteify.app.schedule

import java.time.ZonedDateTime

data class DailyScheduleTime(
    val hour: Int,
    val minute: Int
) {
    fun nextOccurrenceAfter(now: ZonedDateTime): ZonedDateTime {
        val candidate = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)
        return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
    }

    companion object {
        fun parse(value: String): DailyScheduleTime? {
            val parts = value.split(":")
            if (parts.size != 2) return null

            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null

            return DailyScheduleTime(
                hour = hour,
                minute = minute
            )
        }
    }
}
