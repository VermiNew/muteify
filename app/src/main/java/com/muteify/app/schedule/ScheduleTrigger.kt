package com.muteify.app.schedule

enum class ScheduleTrigger(
    val requestCode: Int
) {
    MORNING_UNMUTE_CHECK(requestCode = 600),
    NIGHT_MUTE_CHECK(requestCode = 2200);

    companion object {
        fun fromName(value: String?): ScheduleTrigger? {
            return values().firstOrNull { it.name == value }
        }
    }
}
