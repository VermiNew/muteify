package com.muteify.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muteify.app.data.model.SchedulePolicy
import com.muteify.app.data.model.SoundAction
import com.muteify.app.data.repository.ScheduleSettings
import com.muteify.app.data.repository.ScheduleSlotSettings
import com.muteify.app.data.repository.SettingsRepository
import com.muteify.app.engine.AudioController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slot = ScheduleSlot.fromName(intent.getStringExtra(EXTRA_SLOT)) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReceive(context, intent, slot)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReceive(context: Context, intent: Intent, slot: ScheduleSlot) {
        val settings = SettingsRepository(context).getScheduleSettings()
        val slotSettings = settings.settingsFor(slot)
        val scheduler = ScheduleAlarmScheduler(context)
        val notifier = SchedulePromptNotifier(context)

        when (intent.action) {
            ACTION_CONFIRM -> runPendingAction(context, slot, slotSettings, scheduler, notifier)
            ACTION_RUN_PENDING -> runAutomaticPendingAction(
                context,
                slot,
                slotSettings,
                scheduler,
                notifier
            )
            ACTION_DISMISS -> dismissPendingAction(slot, scheduler, notifier)
            else -> handleScheduleTrigger(slot, settings, slotSettings, scheduler, notifier)
        }
    }

    private fun handleScheduleTrigger(
        slot: ScheduleSlot,
        settings: ScheduleSettings,
        slotSettings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier
    ) {
        scheduler.scheduleDaily(settings)

        if (!slotSettings.enabled) {
            dismissPendingAction(slot, scheduler, notifier)
            return
        }

        val policy = slotSettings.effectivePolicy()
        notifier.show(slot, slotSettings, policy)
        if (policy == SchedulePolicy.AUTO_AFTER_COUNTDOWN) {
            scheduler.schedulePendingAction(slot, slotSettings.countdownSeconds)
        } else {
            scheduler.cancelPendingAction(slot)
        }
    }

    private fun runPendingAction(
        context: Context,
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier
    ) {
        dismissPendingAction(slot, scheduler, notifier)
        if (!settings.enabled) return
        AudioController(context).apply(settings.action)
    }

    private fun runAutomaticPendingAction(
        context: Context,
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier
    ) {
        if (settings.effectivePolicy() != SchedulePolicy.AUTO_AFTER_COUNTDOWN) {
            dismissPendingAction(slot, scheduler, notifier)
            return
        }
        runPendingAction(context, slot, settings, scheduler, notifier)
    }

    private fun dismissPendingAction(
        slot: ScheduleSlot,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier
    ) {
        scheduler.cancelPendingAction(slot)
        notifier.dismiss(slot)
    }

    private fun ScheduleSettings.settingsFor(slot: ScheduleSlot): ScheduleSlotSettings {
        return when (slot) {
            ScheduleSlot.MORNING -> morning
            ScheduleSlot.EVENING -> evening
        }
    }

    private fun ScheduleSlotSettings.effectivePolicy(): SchedulePolicy {
        return if (action == SoundAction.UNSILENCE && policy == SchedulePolicy.AUTO_AFTER_COUNTDOWN) {
            SchedulePolicy.REQUIRE_CONFIRMATION
        } else {
            policy
        }
    }

    companion object {
        const val ACTION_CONFIRM = "com.muteify.app.schedule.CONFIRM"
        const val ACTION_DISMISS = "com.muteify.app.schedule.DISMISS"
        const val ACTION_RUN_PENDING = "com.muteify.app.schedule.RUN_PENDING"
        const val EXTRA_SLOT = "extra_schedule_slot"
    }
}
