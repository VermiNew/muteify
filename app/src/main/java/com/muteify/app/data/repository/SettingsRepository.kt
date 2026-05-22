package com.muteify.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class ScheduleSettings(
    val morningTime: String = "06:00",
    val nightTime: String = "22:00"
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val scheduleSettings: Flow<ScheduleSettings> = dataStore.data.map { preferences ->
        ScheduleSettings(
            morningTime = preferences[MORNING_TIME_KEY] ?: "06:00",
            nightTime = preferences[NIGHT_TIME_KEY] ?: "22:00"
        )
    }

    suspend fun saveScheduleTimes(morningTime: String, nightTime: String) {
        dataStore.edit { preferences ->
            preferences[MORNING_TIME_KEY] = morningTime
            preferences[NIGHT_TIME_KEY] = nightTime
        }
    }

    private companion object {
        val MORNING_TIME_KEY = stringPreferencesKey("morning_time")
        val NIGHT_TIME_KEY = stringPreferencesKey("night_time")
    }
}
