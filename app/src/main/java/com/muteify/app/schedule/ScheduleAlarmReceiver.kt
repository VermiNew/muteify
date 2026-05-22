package com.muteify.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ScheduleSlot.fromName(intent.getStringExtra(EXTRA_SLOT)) == null) return
    }

    companion object {
        const val EXTRA_SLOT = "extra_schedule_slot"
    }
}
