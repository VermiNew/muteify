package com.muteify.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ScheduleTrigger.fromName(intent.getStringExtra(EXTRA_TRIGGER)) == null) return
    }

    companion object {
        const val EXTRA_TRIGGER = "extra_schedule_trigger"
    }
}
