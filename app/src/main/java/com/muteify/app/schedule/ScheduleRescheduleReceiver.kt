package com.muteify.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muteify.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

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
}
