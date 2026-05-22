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
        const val CHANNEL_ID = "muteify_service"
        const val PROMPT_CHANNEL_ID = "muteify_prompt"
        const val NOTIFICATION_ID = 1
        const val EXTRA_SSID = "extra_ssid"
        const val EXTRA_ACTION_ENTER = "extra_action_enter"
        const val EXTRA_ACTION_LEAVE = "extra_action_leave"
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
            audioController.apply(action)
            showMonitoringNotification()
        }
        createNotificationChannels()
        startForeground(NOTIFICATION_ID, buildMonitoringNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONFIRM -> {
                ruleEngine.confirmAction()
                showMonitoringNotification()
            }
            ACTION_DISMISS -> {
                ruleEngine.dismissAction()
                showMonitoringNotification()
            }
            else -> {
                val ssid = intent?.getStringExtra(EXTRA_SSID) ?: return START_STICKY
                val enter = intent.getStringExtra(EXTRA_ACTION_ENTER)
                    ?.let { SoundAction.valueOf(it) } ?: SoundAction.SILENCE
                val leave = intent.getStringExtra(EXTRA_ACTION_LEAVE)
                    ?.let { SoundAction.valueOf(it) } ?: SoundAction.UNSILENCE
                ruleEngine.start(ssid, enter, leave)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ruleEngine.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Mute-ify działa w tle",
            NotificationManager.IMPORTANCE_LOW
        )
        val promptChannel = NotificationChannel(
            PROMPT_CHANNEL_ID,
            "Mute-ify monity",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(serviceChannel, promptChannel))
    }

    private fun showMonitoringNotification() {
        if (!canPostNotifications()) return
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildMonitoringNotification())
    }

    private fun showPendingActionNotification(action: SoundAction) {
        if (!canPostNotifications()) return
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildPendingActionNotification(action))
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun buildMonitoringNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mute-ify")
            .setContentText("Monitorowanie aktywne")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildPendingActionNotification(action: SoundAction): Notification {
        val actionLabel = when (action) {
            SoundAction.SILENCE -> "Wycisz"
            SoundAction.UNSILENCE -> "Odcisz"
            SoundAction.VIBRATE -> "Wibracje"
            SoundAction.DO_NOTHING -> "Bez zmian"
        }
        val promptText = "Za 30 sekund: $actionLabel"

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
}
