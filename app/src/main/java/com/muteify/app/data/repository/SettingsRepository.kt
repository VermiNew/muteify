package com.muteify.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.muteify.app.data.model.SchedulePolicy
import com.muteify.app.data.model.SoundAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private const val DEFAULT_COUNTDOWN_SECONDS = 30

data class ScheduleSettings(
    val morning: ScheduleSlotSettings = ScheduleSlotSettings.morningDefault(),
    val evening: ScheduleSlotSettings = ScheduleSlotSettings.eveningDefault(),
    val neverAutoUnmute: Boolean = true,
    val automationPausedUntilMillis: Long? = null
) {
    val morningTime: String get() = morning.time
    val nightTime: String get() = evening.time
}

data class ScheduleSlotSettings(
    val enabled: Boolean,
    val time: String,
    val action: SoundAction,
    val policy: SchedulePolicy,
    val countdownSeconds: Int
) {
    companion object {
        fun morningDefault(): ScheduleSlotSettings = ScheduleSlotSettings(
            enabled = true,
            time = "06:00",
            action = SoundAction.UNSILENCE,
            policy = SchedulePolicy.REQUIRE_CONFIRMATION,
            countdownSeconds = DEFAULT_COUNTDOWN_SECONDS
        )

        fun eveningDefault(): ScheduleSlotSettings = ScheduleSlotSettings(
            enabled = true,
            time = "22:00",
            action = SoundAction.SILENCE,
            policy = SchedulePolicy.AUTO_AFTER_COUNTDOWN,
            countdownSeconds = DEFAULT_COUNTDOWN_SECONDS
        )
    }
}

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val scheduleSettings: Flow<ScheduleSettings> = dataStore.data.map { preferences ->
        val morningDefault = ScheduleSlotSettings.morningDefault()
        val eveningDefault = ScheduleSlotSettings.eveningDefault()
        ScheduleSettings(
            morning = ScheduleSlotSettings(
                enabled = preferences[MORNING_ENABLED_KEY] ?: morningDefault.enabled,
                time = preferences[MORNING_TIME_KEY] ?: morningDefault.time,
                action = preferences[MORNING_ACTION_KEY].toSoundActionOr(morningDefault.action),
                policy = preferences[MORNING_POLICY_KEY].toSchedulePolicyOr(morningDefault.policy),
                countdownSeconds = preferences[MORNING_COUNTDOWN_SECONDS_KEY]
                    ?: morningDefault.countdownSeconds
            ),
            evening = ScheduleSlotSettings(
                enabled = preferences[EVENING_ENABLED_KEY] ?: eveningDefault.enabled,
                time = preferences[EVENING_TIME_KEY] ?: eveningDefault.time,
                action = preferences[EVENING_ACTION_KEY].toSoundActionOr(eveningDefault.action),
                policy = preferences[EVENING_POLICY_KEY].toSchedulePolicyOr(eveningDefault.policy),
                countdownSeconds = preferences[EVENING_COUNTDOWN_SECONDS_KEY]
                    ?: eveningDefault.countdownSeconds
            ),
            neverAutoUnmute = preferences[NEVER_AUTO_UNMUTE_KEY] ?: true,
            automationPausedUntilMillis = preferences[AUTOMATION_PAUSED_UNTIL_MILLIS_KEY]
        )
    }

    suspend fun getScheduleSettings(): ScheduleSettings {
        return scheduleSettings.first()
    }

    suspend fun saveScheduleTimes(morningTime: String, nightTime: String) {
        dataStore.edit { preferences ->
            preferences[MORNING_TIME_KEY] = morningTime
            preferences[EVENING_TIME_KEY] = nightTime
        }
    }

    suspend fun saveMorningScheduleSettings(settings: ScheduleSlotSettings) {
        dataStore.edit { preferences ->
            preferences[MORNING_ENABLED_KEY] = settings.enabled
            preferences[MORNING_TIME_KEY] = settings.time
            preferences[MORNING_ACTION_KEY] = settings.action.name
            preferences[MORNING_POLICY_KEY] = settings.policy.name
            preferences[MORNING_COUNTDOWN_SECONDS_KEY] = settings.countdownSeconds
        }
    }

    suspend fun saveEveningScheduleSettings(settings: ScheduleSlotSettings) {
        dataStore.edit { preferences ->
            preferences[EVENING_ENABLED_KEY] = settings.enabled
            preferences[EVENING_TIME_KEY] = settings.time
            preferences[EVENING_ACTION_KEY] = settings.action.name
            preferences[EVENING_POLICY_KEY] = settings.policy.name
            preferences[EVENING_COUNTDOWN_SECONDS_KEY] = settings.countdownSeconds
        }
    }

    suspend fun saveNeverAutoUnmute(value: Boolean) {
        dataStore.edit { preferences ->
            preferences[NEVER_AUTO_UNMUTE_KEY] = value
        }
    }

    suspend fun saveAutomationPausedUntilMillis(value: Long?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(AUTOMATION_PAUSED_UNTIL_MILLIS_KEY)
            } else {
                preferences[AUTOMATION_PAUSED_UNTIL_MILLIS_KEY] = value
            }
        }
    }

    private fun String?.toSoundActionOr(default: SoundAction): SoundAction {
        return this?.let { runCatching { SoundAction.valueOf(it) }.getOrNull() } ?: default
    }

    private fun String?.toSchedulePolicyOr(default: SchedulePolicy): SchedulePolicy {
        return this?.let { runCatching { SchedulePolicy.valueOf(it) }.getOrNull() } ?: default
    }

    private companion object {
        val MORNING_ENABLED_KEY = booleanPreferencesKey("morning_enabled")
        val MORNING_TIME_KEY = stringPreferencesKey("morning_time")
        val MORNING_ACTION_KEY = stringPreferencesKey("morning_action")
        val MORNING_POLICY_KEY = stringPreferencesKey("morning_policy")
        val MORNING_COUNTDOWN_SECONDS_KEY = intPreferencesKey("morning_countdown_seconds")

        val EVENING_ENABLED_KEY = booleanPreferencesKey("evening_enabled")
        val EVENING_TIME_KEY = stringPreferencesKey("night_time")
        val EVENING_ACTION_KEY = stringPreferencesKey("evening_action")
        val EVENING_POLICY_KEY = stringPreferencesKey("evening_policy")
        val EVENING_COUNTDOWN_SECONDS_KEY = intPreferencesKey("evening_countdown_seconds")

        val NEVER_AUTO_UNMUTE_KEY = booleanPreferencesKey("never_auto_unmute")
        val AUTOMATION_PAUSED_UNTIL_MILLIS_KEY =
            longPreferencesKey("automation_paused_until_millis")
    }
}
