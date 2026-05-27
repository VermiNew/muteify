package com.muteify.app.schedule

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.muteify.app.data.model.SchedulePolicy
import com.muteify.app.data.model.SoundAction
import com.muteify.app.data.model.TriggerState
import com.muteify.app.data.repository.ScheduleSlotSettings

class SchedulePromptNotifier(context: Context) {

    private val appContext = context.applicationContext
    private val notificationManager = appContext.getSystemService(NotificationManager::class.java)

    fun show(
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        policy: SchedulePolicy,
        homeState: TriggerState?
    ) {
        if (!canPostNotifications()) return
        createChannel()

        val text = promptText(slot, settings, policy, homeState)
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Mute-ify")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(policy != SchedulePolicy.NOTIFY_ONLY)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (policy != SchedulePolicy.NOTIFY_ONLY) {
            builder
                .addAction(
                    android.R.drawable.ic_menu_save,
                    "Wykonaj teraz",
                    receiverIntent(ScheduleAlarmReceiver.ACTION_CONFIRM, slot, slot.requestCode + 2_000)
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Anuluj",
                    receiverIntent(ScheduleAlarmReceiver.ACTION_DISMISS, slot, slot.requestCode + 3_000)
                )
        }

        notificationManager.notify(notificationId(slot), builder.build())
    }

    fun dismiss(slot: ScheduleSlot) {
        notificationManager.cancel(notificationId(slot))
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mute-ify monity",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun promptText(
        slot: ScheduleSlot,
        settings: ScheduleSlotSettings,
        policy: SchedulePolicy,
        homeState: TriggerState?
    ): String {
        val slotLabel = when (slot) {
            ScheduleSlot.MORNING -> "Rano"
            ScheduleSlot.EVENING -> "Wieczorem"
        }
        val actionLabel = when (settings.action) {
            SoundAction.SILENCE -> "wycisz"
            SoundAction.UNSILENCE -> "odcisz"
            SoundAction.VIBRATE -> "włącz wibracje"
            SoundAction.DO_NOTHING -> "bez zmian"
        }
        if (settings.action == SoundAction.UNSILENCE) {
            return when (homeState) {
                TriggerState.HOME -> "$slotLabel: w domu, potwierdź odciszenie"
                TriggerState.AWAY -> "$slotLabel: poza domem, odcisz ręcznie"
                TriggerState.UNKNOWN -> "$slotLabel: miejsce nieznane, odcisz ręcznie"
                null -> "$slotLabel: potwierdź: $actionLabel"
            }
        }
        return when (policy) {
            SchedulePolicy.AUTO_AFTER_COUNTDOWN ->
                "$slotLabel: za ${settings.countdownSeconds} s: $actionLabel"
            SchedulePolicy.AUTO_SILENT_AFTER_COUNTDOWN ->
                "$slotLabel: za ${settings.countdownSeconds} s: $actionLabel"
            SchedulePolicy.REQUIRE_CONFIRMATION ->
                "$slotLabel: potwierdź: $actionLabel"
            SchedulePolicy.NOTIFY_ONLY ->
                "$slotLabel: $actionLabel"
        }
    }

    private fun receiverIntent(action: String, slot: ScheduleSlot, requestCode: Int): PendingIntent {
        val intent = Intent(appContext, ScheduleAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(ScheduleAlarmReceiver.EXTRA_SLOT, slot.name)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notificationId(slot: ScheduleSlot): Int {
        return 4_000 + slot.requestCode
    }

    private companion object {
        const val CHANNEL_ID = "muteify_prompt"
    }
}
