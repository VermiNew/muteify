package com.muteify.app.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.muteify.app.data.repository.ScheduleSettings
import com.muteify.app.data.repository.ScheduleSlotSettings
import java.time.ZonedDateTime

class ScheduleAlarmScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    fun scheduleDaily(settings: ScheduleSettings) {
        scheduleSlot(ScheduleSlot.MORNING, settings.morning)
        scheduleSlot(ScheduleSlot.EVENING, settings.evening)
    }

    private fun scheduleSlot(slot: ScheduleSlot, settings: ScheduleSlotSettings) {
        if (settings.enabled) {
            schedule(slot, settings.time)
        } else {
            cancel(slot)
        }
    }

    private fun schedule(slot: ScheduleSlot, timeValue: String) {
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
        ScheduleSlot.values().forEach(::cancelPendingAction)
    }

    fun cancel(slot: ScheduleSlot) {
        alarmManager.cancel(pendingIntent(slot))
    }

    fun schedulePendingAction(slot: ScheduleSlot, delaySeconds: Int) {
        val triggerAtMillis = System.currentTimeMillis() + delaySeconds.coerceAtLeast(0) * 1_000L
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingActionIntent(slot)
        )
    }

    fun cancelPendingAction(slot: ScheduleSlot) {
        alarmManager.cancel(pendingActionIntent(slot))
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

    private fun pendingActionIntent(slot: ScheduleSlot): PendingIntent {
        val intent = Intent(appContext, ScheduleAlarmReceiver::class.java).apply {
            action = ScheduleAlarmReceiver.ACTION_RUN_PENDING
            putExtra(ScheduleAlarmReceiver.EXTRA_SLOT, slot.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            slot.pendingActionRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
