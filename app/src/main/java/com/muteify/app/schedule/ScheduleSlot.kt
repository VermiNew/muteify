package com.muteify.app.schedule

enum class ScheduleSlot(
    val requestCode: Int
) {
    MORNING(requestCode = 1001),
    EVENING(requestCode = 1002);

    companion object {
        fun fromName(value: String?): ScheduleSlot? {
            return values().firstOrNull { it.name == value }
        }
    }
}
