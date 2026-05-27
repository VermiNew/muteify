package com.muteify.app.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.muteify.app.data.model.SoundAction
import com.muteify.app.data.repository.RuleHistoryRepository
import com.muteify.app.data.repository.SettingsRepository
import com.muteify.app.engine.AudioController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuietHoursAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReceive(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReceive(context: Context) {
        val settingsRepository = SettingsRepository(context)
        val settings = settingsRepository.getScheduleSettings()
        val quietUntilMillis = settings.quietHoursUntilMillis ?: return
        val historyRepository = RuleHistoryRepository(context)

        if (quietUntilMillis > System.currentTimeMillis()) {
            QuietHoursScheduler(context).schedule(quietUntilMillis)
            return
        }

        val audioController = AudioController(context)
        if (!audioController.canChangeRingerMode()) {
            settingsRepository.saveQuietHoursUntilMillis(null)
            historyRepository.recordEvent(
                source = QUIET_HOURS_SOURCE,
                triggerState = "NOT_APPLICABLE",
                action = SoundAction.UNSILENCE.name,
                policy = QUIET_HOURS_POLICY,
                outcome = "skipped_missing_notification_policy_access",
                details = "One-off quiet hours could not end because notification policy access is missing"
            )
            return
        }

        audioController.apply(SoundAction.UNSILENCE)
        settingsRepository.saveQuietHoursUntilMillis(null)
        historyRepository.recordEvent(
            source = QUIET_HOURS_SOURCE,
            triggerState = "NOT_APPLICABLE",
            action = SoundAction.UNSILENCE.name,
            policy = QUIET_HOURS_POLICY,
            outcome = "auto_executed",
            details = "One-off quiet hours ended"
        )
    }

    companion object {
        const val QUIET_HOURS_SOURCE = "quiet:one_off"
        const val QUIET_HOURS_POLICY = "ONE_OFF"
    }
}
