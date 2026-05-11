package com.muteify.app.engine

import android.content.Context
import com.muteify.app.data.model.SoundAction
import com.muteify.app.data.model.TriggerState
import com.muteify.app.monitor.WifiMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RuleEngine(
    private val context: Context,
    private val audioController: AudioController,
    private val wifiMonitor: WifiMonitor
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null

    // Callback wywoływany gdy trzeba pokazać overlay użytkownikowi
    var onActionPending: ((SoundAction) -> Unit)? = null

    private var pendingAction: SoundAction? = null
    private var targetSsid: String = ""
    private var actionEnter: SoundAction = SoundAction.SILENCE
    private var actionLeave: SoundAction = SoundAction.UNSILENCE

    fun start(ssid: String, enter: SoundAction, leave: SoundAction) {
        targetSsid = ssid
        actionEnter = enter
        actionLeave = leave
        wifiMonitor.start(ssid)
        monitorJob = scope.launch {
            wifiMonitor.state
                .collect { state ->
                    when (state) {
                        TriggerState.HOME -> schedulePendingAction(actionEnter)
                        TriggerState.AWAY -> schedulePendingAction(actionLeave)
                        TriggerState.UNKNOWN -> Unit
                    }
                }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        wifiMonitor.stop()
    }

    // Odczekaj 60 sekund przed wykonaniem akcji (debounce)
    private fun schedulePendingAction(action: SoundAction) {
        pendingAction = action
        scope.launch {
            delay(60_000)
            pendingAction?.let { onActionPending?.invoke(it) }
        }
    }

    // Wywołane gdy użytkownik zatwierdził akcję z overlay
    fun confirmAction() {
        pendingAction?.let { audioController.apply(it) }
        pendingAction = null
    }

    // Wywołane gdy użytkownik odrzucił akcję z overlay
    fun dismissAction() {
        pendingAction = null
    }
}
