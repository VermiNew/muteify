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
    private companion object {
        const val PENDING_ACTION_DELAY_MS = 30_000L
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null
    private var pendingActionJob: Job? = null
    private var pendingActionVersion: Long = 0

    // Called when the delayed action is ready to run.
    var onActionPending: ((SoundAction) -> Unit)? = null

    // Called when the user can still cancel or confirm the delayed action.
    var onActionScheduled: ((SoundAction) -> Unit)? = null

    private var pendingAction: SoundAction? = null
    private var targetSsid: String = ""
    private var actionEnter: SoundAction = SoundAction.UNSILENCE
    private var actionLeave: SoundAction = SoundAction.SILENCE
    var neverAutoUnmute: Boolean = true
        private set

    fun start(ssid: String, enter: SoundAction, leave: SoundAction, neverAutoUnmute: Boolean) {
        targetSsid = ssid
        actionEnter = enter
        actionLeave = leave
        this.neverAutoUnmute = neverAutoUnmute
        wifiMonitor.start(ssid)
        monitorJob = scope.launch {
            var lastState: TriggerState? = null
            wifiMonitor.state
                .collect { state ->
                    val previousState = lastState
                    lastState = state
                    if (previousState == null || previousState == state) return@collect

                    when (state) {
                        TriggerState.HOME -> if (previousState == TriggerState.AWAY) {
                            schedulePendingAction(actionEnter)
                        }
                        TriggerState.AWAY -> if (previousState == TriggerState.HOME) {
                            schedulePendingAction(actionLeave)
                        }
                        TriggerState.UNKNOWN -> Unit
                    }
                }
        }
    }

    fun stop() {
        monitorJob?.cancel()
        cancelPendingAction()
        wifiMonitor.stop()
    }

    private fun schedulePendingAction(action: SoundAction) {
        pendingActionJob?.cancel()
        pendingAction = action
        onActionScheduled?.invoke(action)
        val actionVersion = ++pendingActionVersion
        pendingActionJob = scope.launch {
            delay(PENDING_ACTION_DELAY_MS)
            if (pendingActionVersion != actionVersion) return@launch
            val actionToRun = pendingAction ?: return@launch
            pendingAction = null
            pendingActionJob = null
            onActionPending?.invoke(actionToRun)
        }
    }

    fun confirmAction() {
        pendingActionVersion++
        pendingActionJob?.cancel()
        pendingActionJob = null
        pendingAction?.let { audioController.apply(it) }
        pendingAction = null
    }

    fun dismissAction() {
        cancelPendingAction()
    }

    private fun cancelPendingAction() {
        pendingActionVersion++
        pendingActionJob?.cancel()
        pendingActionJob = null
        pendingAction = null
    }
}
