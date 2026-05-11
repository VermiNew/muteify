package com.muteify.app.ui.screen

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.muteify.app.data.model.SoundAction
import com.muteify.app.service.MuteifyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _ssid = MutableStateFlow("")
    val ssid: StateFlow<String> = _ssid

    private val _actionEnter = MutableStateFlow(SoundAction.SILENCE)
    val actionEnter: StateFlow<SoundAction> = _actionEnter

    private val _actionLeave = MutableStateFlow(SoundAction.UNSILENCE)
    val actionLeave: StateFlow<SoundAction> = _actionLeave

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    fun onSsidChanged(value: String) { _ssid.value = value }
    fun onActionEnterChanged(value: SoundAction) { _actionEnter.value = value }
    fun onActionLeaveChanged(value: SoundAction) { _actionLeave.value = value }

    fun toggleService() {
        val context = getApplication<Application>()
        if (_isRunning.value) {
            context.stopService(Intent(context, MuteifyService::class.java))
            _isRunning.value = false
        } else {
            if (_ssid.value.isBlank()) return
            val intent = Intent(context, MuteifyService::class.java).apply {
                putExtra(MuteifyService.EXTRA_SSID, _ssid.value)
                putExtra(MuteifyService.EXTRA_ACTION_ENTER, _actionEnter.value.name)
                putExtra(MuteifyService.EXTRA_ACTION_LEAVE, _actionLeave.value.name)
            }
            context.startForegroundService(intent)
            _isRunning.value = true
        }
    }
}
