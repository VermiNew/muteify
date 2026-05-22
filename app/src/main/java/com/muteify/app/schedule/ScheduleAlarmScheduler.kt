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
        schedule(ScheduleTrigger.MORNING_UNMUTE_CHECK, morningTime)
        schedule(ScheduleTrigger.NIGHT_MUTE_CHECK, nightTime)
    }

    fun schedule(trigger: ScheduleTrigger, timeValue: String) {
        val scheduleTime = DailyScheduleTime.parse(timeValue) ?: return
        val triggerAtMillis = scheduleTime
            .nextOccurrenceAfter(ZonedDateTime.now())
            .toInstant()
            .toEpochMilli()

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(trigger)
        )
    }

    fun cancelAll() {
        ScheduleTrigger.values().forEach(::cancel)
    }

    fun cancel(trigger: ScheduleTrigger) {
        alarmManager.cancel(pendingIntent(trigger))
    }

    private fun pendingIntent(trigger: ScheduleTrigger): PendingIntent {
        val intent = Intent(appContext, ScheduleAlarmReceiver::class.java).apply {
            putExtra(ScheduleAlarmReceiver.EXTRA_TRIGGER, trigger.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            trigger.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
