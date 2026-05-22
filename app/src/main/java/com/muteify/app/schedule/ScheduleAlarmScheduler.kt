package com.muteify.app.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

class ScheduleAlarmScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun scheduleDaily(morningTime: String, nightTime: String) {
        schedule(ScheduleSlot.MORNING, morningTime)
        schedule(ScheduleSlot.EVENING, nightTime)
    }

    fun schedule(slot: ScheduleSlot, timeValue: String) {
        val scheduleTime = DailyScheduleTime.parse(timeValue) ?: return
        val triggerAtMillis = scheduleTime
            .nextOccurrenceAfter(ZonedDateTime.now())
            .toInstant()
            .toEpochMilli()

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(slot)
        )
    }

    fun cancelAll() {
        ScheduleSlot.values().forEach(::cancel)
    }

    fun cancel(slot: ScheduleSlot) {
        alarmManager.cancel(pendingIntent(slot))
    }

    private fun pendingIntent(slot: ScheduleSlot): PendingIntent {
        val intent = Intent(appContext, ScheduleAlarmReceiver::class.java).apply {
            putExtra(ScheduleAlarmReceiver.EXTRA_SLOT, slot.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            slot.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
