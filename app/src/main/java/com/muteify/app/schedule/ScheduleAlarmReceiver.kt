package com.muteify.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muteify.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ScheduleSlot.fromName(intent.getStringExtra(EXTRA_SLOT)) == null) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).getScheduleSettings()
                ScheduleAlarmScheduler(context).scheduleDaily(settings)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_RUN_PENDING = "com.muteify.app.schedule.RUN_PENDING"
        const val EXTRA_SLOT = "extra_schedule_slot"
    }
}
