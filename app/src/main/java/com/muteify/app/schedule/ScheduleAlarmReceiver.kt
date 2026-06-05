package com.muteify.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muteify.app.data.model.RulePriority
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
                settings.neverAutoUnmute,
                settings.automationPausedUntilMillis,
                settings.rulePriority,
                settings.trustedWifiSsids,
                outcome = "confirmed"
            )
            ACTION_RUN_PENDING -> runAutomaticPendingAction(
                context,
                slot,
                slotSettings,
                scheduler,
                notifier,
                historyRepository,
                settings.neverAutoUnmute,
                settings.automationPausedUntilMillis,
                settings.rulePriority,
                settings.trustedWifiSsids
            )
            ACTION_DISMISS -> dismissPendingAction(
                slot,
                slotSettings,
                scheduler,
                notifier,
                historyRepository,
                settings.neverAutoUnmute
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
                policy = slotSettings.effectivePolicy(settings.neverAutoUnmute),
                homeState = null,
                outcome = "skipped_disabled",
                details = "Schedule slot is disabled"
            )
            return
        }

        if (settings.isAutomationPaused()) {
            dismissPendingAction(slot, scheduler, notifier)
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = slotSettings,
                policy = slotSettings.effectivePolicy(settings.neverAutoUnmute),
                homeState = null,
                outcome = "skipped_paused",
                details = "Automation is paused"
            )
            return
        }

        val policy = slotSettings.effectivePolicy(settings.neverAutoUnmute)
        val homeState = resolveHomeState(context, slotSettings, settings.trustedWifiSsids)
        if (settings.rulePriority.blocksScheduleAction(slotSettings, homeState)) {
            dismissPendingAction(slot, scheduler, notifier)
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = slotSettings,
                policy = policy,
                homeState = homeState,
                outcome = "skipped_rule_priority",
                details = "Schedule unmute was skipped because Wi-Fi has priority"
            )
            return
        }

        when (policy) {
            SchedulePolicy.AUTO_SILENT_AFTER_COUNTDOWN -> {
                notifier.dismiss(slot)
                scheduler.schedulePendingAction(slot, slotSettings.countdownSeconds)
            }
            SchedulePolicy.AUTO_AFTER_COUNTDOWN -> {
                notifier.show(slot, slotSettings, policy, homeState)
                scheduler.schedulePendingAction(slot, slotSettings.countdownSeconds)
            }
            SchedulePolicy.REQUIRE_CONFIRMATION,
            SchedulePolicy.NOTIFY_ONLY -> {
                notifier.show(slot, slotSettings, policy, homeState)
                scheduler.cancelPendingAction(slot)
            }
        }
        recordScheduleEvent(
            historyRepository = historyRepository,
            slot = slot,
            settings = slotSettings,
            policy = policy,
            homeState = homeState,
            outcome = if (policy == SchedulePolicy.AUTO_SILENT_AFTER_COUNTDOWN) {
                "countdown_scheduled"
            } else {
                "prompted"
            },
            details = if (policy == SchedulePolicy.AUTO_SILENT_AFTER_COUNTDOWN) {
                "Schedule trigger handled without prompt"
            } else {
                "Schedule trigger handled"
            }
        )
    }

    private suspend fun runPendingAction(
        context: Context,
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier,
        historyRepository: RuleHistoryRepository,
        neverAutoUnmute: Boolean,
        automationPausedUntilMillis: Long?,
        rulePriority: RulePriority,
        trustedWifiSsids: Set<String>,
        outcome: String
    ) {
        dismissPendingAction(slot, scheduler, notifier)
        if (!settings.enabled) {
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = settings,
                policy = settings.effectivePolicy(neverAutoUnmute),
                homeState = null,
                outcome = "skipped_disabled",
                details = "Pending schedule action was skipped because the slot is disabled"
            )
            return
        }
        if (automationPausedUntilMillis.isAutomationPaused()) {
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = settings,
                policy = settings.effectivePolicy(neverAutoUnmute),
                homeState = null,
                outcome = "skipped_paused",
                details = "Pending schedule action was skipped because automation is paused"
            )
            return
        }
        val homeState = resolveHomeState(context, settings, trustedWifiSsids)
        if (rulePriority.blocksScheduleAction(settings, homeState)) {
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = settings,
                policy = settings.effectivePolicy(neverAutoUnmute),
                homeState = homeState,
                outcome = "skipped_rule_priority",
                details = "Pending schedule unmute was skipped because Wi-Fi has priority"
            )
            return
        }
        val audioController = AudioController(context)
        if (settings.action != SoundAction.DO_NOTHING && !audioController.canChangeRingerMode()) {
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = settings,
                policy = settings.effectivePolicy(neverAutoUnmute),
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
            policy = settings.effectivePolicy(neverAutoUnmute),
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
        historyRepository: RuleHistoryRepository,
        neverAutoUnmute: Boolean,
        automationPausedUntilMillis: Long?,
        rulePriority: RulePriority,
        trustedWifiSsids: Set<String>
    ) {
        if (!settings.effectivePolicy(neverAutoUnmute).runsAfterCountdown()) {
            dismissPendingAction(slot, scheduler, notifier)
            recordScheduleEvent(
                historyRepository = historyRepository,
                slot = slot,
                settings = settings,
                policy = settings.effectivePolicy(neverAutoUnmute),
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
            neverAutoUnmute,
            automationPausedUntilMillis = automationPausedUntilMillis,
            rulePriority = rulePriority,
            trustedWifiSsids = trustedWifiSsids,
            outcome = "auto_executed"
        )
    }

    private suspend fun dismissPendingAction(
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        scheduler: ScheduleAlarmScheduler,
        notifier: SchedulePromptNotifier,
        historyRepository: RuleHistoryRepository,
        neverAutoUnmute: Boolean
    ) {
        dismissPendingAction(slot, scheduler, notifier)
        recordScheduleEvent(
            historyRepository = historyRepository,
            slot = slot,
            settings = settings,
            policy = settings.effectivePolicy(neverAutoUnmute),
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
        settings: ScheduleSlotSettings,
        trustedWifiSsids: Set<String>
    ): TriggerState? {
        if (settings.action != SoundAction.UNSILENCE) return null

        val homeRuleSsid = AppDatabase.getInstance(context).ruleDao().getAllRules().first()
            .firstOrNull { it.isEnabled && it.wifiSsid.isNotBlank() }
            ?.wifiSsid
        val homeSsids = (trustedWifiSsids + homeRuleSsid.orEmpty())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        if (homeSsids.isEmpty()) return TriggerState.UNKNOWN

        val currentSsid = WifiPresenceChecker(context).currentSsid()
            ?: return TriggerState.UNKNOWN

        return if (currentSsid in homeSsids) {
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

    private fun ScheduleSettings.isAutomationPaused(): Boolean {
        return automationPausedUntilMillis.isAutomationPaused()
    }

    private fun Long?.isAutomationPaused(): Boolean {
        return this != null && this > System.currentTimeMillis()
    }

    private fun ScheduleSlotSettings.effectivePolicy(neverAutoUnmute: Boolean): SchedulePolicy {
        return if (
            neverAutoUnmute &&
            action == SoundAction.UNSILENCE &&
            policy.runsAfterCountdown()
        ) {
            SchedulePolicy.REQUIRE_CONFIRMATION
        } else {
            policy
        }
    }

    private fun SchedulePolicy.runsAfterCountdown(): Boolean {
        return this == SchedulePolicy.AUTO_AFTER_COUNTDOWN ||
            this == SchedulePolicy.AUTO_SILENT_AFTER_COUNTDOWN
    }

    private fun RulePriority.blocksScheduleAction(
        settings: ScheduleSlotSettings,
        homeState: TriggerState?
    ): Boolean {
        return this == RulePriority.WIFI_FIRST &&
            settings.action == SoundAction.UNSILENCE &&
            homeState != TriggerState.HOME
    }

    companion object {
        const val ACTION_CONFIRM = "com.muteify.app.schedule.CONFIRM"
        const val ACTION_DISMISS = "com.muteify.app.schedule.DISMISS"
        const val ACTION_RUN_PENDING = "com.muteify.app.schedule.RUN_PENDING"
        const val EXTRA_SLOT = "extra_schedule_slot"
    }
}
