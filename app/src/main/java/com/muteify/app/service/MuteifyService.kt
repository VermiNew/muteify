package com.muteify.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.muteify.app.data.model.SoundAction
import com.muteify.app.engine.AudioController
import com.muteify.app.engine.RuleEngine
import com.muteify.app.monitor.WifiMonitor

class MuteifyService : Service() {

    companion object {
        const val LEGACY_SERVICE_CHANNEL_ID = "muteify_service"
        const val PROMPT_CHANNEL_ID = "muteify_prompt"
        const val PROMPT_NOTIFICATION_ID = 1
        const val EXTRA_SSID = "extra_ssid"
        const val EXTRA_ACTION_ENTER = "extra_action_enter"
        const val EXTRA_ACTION_LEAVE = "extra_action_leave"
        const val EXTRA_NEVER_AUTO_UNMUTE = "extra_never_auto_unmute"
        const val ACTION_CONFIRM = "action_confirm"
        const val ACTION_DISMISS = "action_dismiss"
    }

    private lateinit var ruleEngine: RuleEngine
    private lateinit var audioController: AudioController
    private lateinit var wifiMonitor: WifiMonitor

    override fun onCreate() {
        super.onCreate()
        audioController = AudioController(this)
        wifiMonitor = WifiMonitor(this)
        ruleEngine = RuleEngine(this, audioController, wifiMonitor)
        ruleEngine.onActionScheduled = { action ->
            showPendingActionNotification(action)
        }
        ruleEngine.onActionPending = { action ->
            if (action.canRunAutomatically(ruleEngine.neverAutoUnmute)) {
                audioController.apply(action)
            }
            clearPendingActionNotification()
        }
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONFIRM -> {
                ruleEngine.confirmAction()
                clearPendingActionNotification()
            }
            ACTION_DISMISS -> {
                ruleEngine.dismissAction()
                clearPendingActionNotification()
            }
            else -> {
                val ssid = intent?.getStringExtra(EXTRA_SSID) ?: return START_STICKY
                val enter = intent.getStringExtra(EXTRA_ACTION_ENTER)
                    ?.let { SoundAction.valueOf(it) } ?: SoundAction.UNSILENCE
                val leave = intent.getStringExtra(EXTRA_ACTION_LEAVE)
                    ?.let { SoundAction.valueOf(it) } ?: SoundAction.SILENCE
                val neverAutoUnmute = intent.getBooleanExtra(EXTRA_NEVER_AUTO_UNMUTE, true)
                ruleEngine.start(ssid, enter, leave, neverAutoUnmute)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ruleEngine.stop()
        clearPendingActionNotification()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val promptChannel = NotificationChannel(
            PROMPT_CHANNEL_ID,
            "Mute-ify monity",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.deleteNotificationChannel(LEGACY_SERVICE_CHANNEL_ID)
        notificationManager.createNotificationChannel(promptChannel)
    }

    private fun showPendingActionNotification(action: SoundAction) {
        if (!canPostNotifications()) return
        getSystemService(NotificationManager::class.java)
            .notify(PROMPT_NOTIFICATION_ID, buildPendingActionNotification(action))
    }

    private fun clearPendingActionNotification() {
        getSystemService(NotificationManager::class.java)
            .cancel(PROMPT_NOTIFICATION_ID)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun buildPendingActionNotification(action: SoundAction): Notification {
        val actionLabel = when (action) {
            SoundAction.SILENCE -> "Wycisz"
            SoundAction.UNSILENCE -> "Odcisz"
            SoundAction.VIBRATE -> "Wibracje"
            SoundAction.DO_NOTHING -> "Bez zmian"
        }
        val promptText = if (action.canRunAutomatically(ruleEngine.neverAutoUnmute)) {
            "Za 30 sekund: $actionLabel"
        } else {
            "Potwierdź: $actionLabel"
        }

        return NotificationCompat.Builder(this, PROMPT_CHANNEL_ID)
            .setContentTitle("Mute-ify")
            .setContentText(promptText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(promptText))
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                android.R.drawable.ic_menu_save,
                "Wykonaj teraz",
                serviceActionIntent(ACTION_CONFIRM)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Anuluj",
                serviceActionIntent(ACTION_DISMISS)
            )
            .build()
    }

    private fun serviceActionIntent(action: String): PendingIntent {
        val intent = Intent(this, MuteifyService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun SoundAction.canRunAutomatically(neverAutoUnmute: Boolean): Boolean {
        return this != SoundAction.DO_NOTHING &&
            !(neverAutoUnmute && this == SoundAction.UNSILENCE)
    }
}
