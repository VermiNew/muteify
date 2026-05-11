package com.muteify.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.muteify.app.data.model.SoundAction
import com.muteify.app.engine.AudioController
import com.muteify.app.engine.RuleEngine
import com.muteify.app.monitor.WifiMonitor

class MuteifyService : Service() {

    companion object {
        const val CHANNEL_ID = "muteify_service"
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
        ruleEngine.onActionPending = { action ->
            // Na razie wykonaj akcję automatycznie – overlay dodamy później
            audioController.apply(action)
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONFIRM -> ruleEngine.confirmAction()
            ACTION_DISMISS -> ruleEngine.dismissAction()
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mute-ify działa w tle",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mute-ify")
            .setContentText("Monitorowanie aktywne")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .build()
    }
}
