package com.muteify.app.engine

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.muteify.app.data.model.SoundAction

class AudioController(context: Context) {

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun apply(action: SoundAction) {
        if (action != SoundAction.DO_NOTHING && !canChangeRingerMode()) return

        when (action) {
            SoundAction.SILENCE -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            SoundAction.UNSILENCE -> audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            SoundAction.VIBRATE -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            SoundAction.DO_NOTHING -> Unit
        }
    }

    fun canChangeRingerMode(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun getCurrentAction(): SoundAction {
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> SoundAction.SILENCE
            AudioManager.RINGER_MODE_VIBRATE -> SoundAction.VIBRATE
            else -> SoundAction.UNSILENCE
        }
    }
}
