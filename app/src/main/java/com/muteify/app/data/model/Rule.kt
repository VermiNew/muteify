package com.muteify.app.data.model

data class Rule(
    val id: Long = 0,
    val name: String,
    val wifiSsid: String,
    val actionEnter: SoundAction,
    val actionLeave: SoundAction,
    val isEnabled: Boolean = true
)

enum class SoundAction {
    SILENCE, UNSILENCE, VIBRATE, DO_NOTHING
}
