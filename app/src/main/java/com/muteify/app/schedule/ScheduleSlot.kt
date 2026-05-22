package com.muteify.app.schedule

enum class ScheduleSlot(
    val requestCode: Int,
    val pendingActionRequestCode: Int
) {
    MORNING(
        requestCode = 1001,
        pendingActionRequestCode = 1101
    ),
    EVENING(
        requestCode = 1002,
        pendingActionRequestCode = 1102
    );

    companion object {
        fun fromName(value: String?): ScheduleSlot? {
            return values().firstOrNull { it.name == value }
        }
    }
}
