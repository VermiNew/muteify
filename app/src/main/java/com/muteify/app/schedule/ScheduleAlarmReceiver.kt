package com.muteify.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muteify.app.data.model.SchedulePolicy
import com.muteify.app.data.model.SoundAction
import com.muteify.app.data.model.TriggerState
import com.muteify.app.data.repository.AppDatabase
import com.muteify.app.data.repository.RuleHistoryRepository
import com.muteify.app.data.repository.ScheduleSettings
import com.muteify.app.data.repository.ScheduleSlotSettings
import com.muteify.app.data.repository.SettingsRepository
import com.muteify.app.engine.AudioController
import com.muteify.app.monitor.WifiPresenceChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
        val historyRepository = RuleHistoryRepository(context)

        when (intent.action) {
            ACTION_CONFIRM -> runPendingAction(
                context,
                slot,
                slotSettings,
                scheduler,
                notifier,
                historyRepository,
                outcome = "confirmed"
            )
            ACTION_RUN_PENDING -> runAutomaticPendingAction(
                context,
                slot,
                slotSettings,
                scheduler,
                notifier,
                historyRepository
            )
            ACTION_DISMISS -> dismissPendingAction(
                slot,
                slotSettings,
                scheduler,
                notifier,
                historyRepository
            )
            else -> handleScheduleTrigger(
                context,
                slot,
                settings,
                slotSettings,
                scheduler,
                notifier,
                historyRepository
            )
        }
    }

    private suspend fun handleScheduleTrigger(
        context: Context,
        slot: ScheduleSlot,
        settings: ScheduleSettings,
        slotSettings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier,
        historyRepository: RuleHistoryRepository
    ) {
        scheduler.scheduleDaily(settings)

        if (!slotSettings.enabled) {
            dismissPendingAction(slot, scheduler, notifier)
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = slotSettings,
                policy = slotSettings.effectivePolicy(),
                homeState = null,
                outcome = "skipped_disabled",
                details = "Schedule slot is disabled"
            )
            return
        }

        val policy = slotSettings.effectivePolicy()
        val homeState = resolveHomeState(context, slotSettings)
        notifier.show(slot, slotSettings, policy, homeState)
        if (policy == SchedulePolicy.AUTO_AFTER_COUNTDOWN) {
            scheduler.schedulePendingAction(slot, slotSettings.countdownSeconds)
        } else {
            scheduler.cancelPendingAction(slot)
        }
        recordScheduleEvent(
            historyRepository = historyRepository,
            slot = slot,
            settings = slotSettings,
            policy = policy,
            homeState = homeState,
            outcome = "prompted",
            details = "Schedule trigger handled"
        )
    }

    private suspend fun runPendingAction(
        context: Context,
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier,
        historyRepository: RuleHistoryRepository,
        outcome: String
    ) {
        dismissPendingAction(slot, scheduler, notifier)
        if (!settings.enabled) {
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = settings,
                policy = settings.effectivePolicy(),
                homeState = null,
                outcome = "skipped_disabled",
                details = "Pending schedule action was skipped because the slot is disabled"
            )
            return
        }
        val audioController = AudioController(context)
        if (settings.action != SoundAction.DO_NOTHING && !audioController.canChangeRingerMode()) {
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = settings,
                policy = settings.effectivePolicy(),
                homeState = null,
                outcome = "skipped_missing_notification_policy_access",
                details = "Schedule action was skipped because notification policy access is missing"
            )
            return
        }
        audioController.apply(settings.action)
        recordScheduleEvent(
            historyRepository = historyRepository,
            slot = slot,
            settings = settings,
            policy = settings.effectivePolicy(),
            homeState = null,
            outcome = outcome,
            details = "Schedule action applied"
        )
    }

    private suspend fun runAutomaticPendingAction(
        context: Context,
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier,
        historyRepository: RuleHistoryRepository
    ) {
        if (settings.effectivePolicy() != SchedulePolicy.AUTO_AFTER_COUNTDOWN) {
            dismissPendingAction(slot, scheduler, notifier)
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = settings,
                policy = settings.effectivePolicy(),
                homeState = null,
                outcome = "skipped_policy_changed",
                details = "Pending schedule action was skipped because policy changed"
            )
            return
        }
        runPendingAction(
            context,
            slot,
            settings,
            scheduler,
            notifier,
            historyRepository,
            outcome = "auto_executed"
        )
    }

    private suspend fun dismissPendingAction(
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier,
        historyRepository: RuleHistoryRepository
    ) {
        dismissPendingAction(slot, scheduler, notifier)
        recordScheduleEvent(
            historyRepository = historyRepository,
            slot = slot,
            settings = settings,
            policy = settings.effectivePolicy(),
            homeState = null,
            outcome = "dismissed",
            details = "Pending schedule action was cancelled"
        )
    }

    private fun dismissPendingAction(
        slot: ScheduleSlot,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier
    ) {
        scheduler.cancelPendingAction(slot)
        notifier.dismiss(slot)
    }

    private suspend fun resolveHomeState(
        context: Context,
        settings: ScheduleSlotSettings
    ): TriggerState? {
        if (settings.action != SoundAction.UNSILENCE) return null

        val homeRule = AppDatabase.getInstance(context).ruleDao().getAllRules().first()
            .firstOrNull { it.isEnabled && it.wifiSsid.isNotBlank() }
            ?: return TriggerState.UNKNOWN
        val currentSsid = WifiPresenceChecker(context).currentSsid()
            ?: return TriggerState.UNKNOWN

        return if (currentSsid == homeRule.wifiSsid) {
            TriggerState.HOME
        } else {
            TriggerState.AWAY
        }
    }

    private suspend fun recordScheduleEvent(
        historyRepository: RuleHistoryRepository,
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        policy: SchedulePolicy,
        homeState: TriggerState?,
        outcome: String,
        details: String
    ) {
        historyRepository.recordEvent(
            source = "schedule:${slot.name.lowercase()}",
            triggerState = homeState?.name ?: "NOT_APPLICABLE",
            action = settings.action.name,
            policy = policy.name,
            outcome = outcome,
            details = details
        )
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
