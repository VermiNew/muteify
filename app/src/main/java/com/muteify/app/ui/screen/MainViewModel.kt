package com.muteify.app.ui.screen

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.muteify.app.data.model.RuleEntity
import com.muteify.app.data.model.SchedulePolicy
import com.muteify.app.data.model.SoundAction
import com.muteify.app.data.repository.AppDatabase
import com.muteify.app.data.repository.ScheduleSlotSettings
import com.muteify.app.data.repository.SettingsRepository
import com.muteify.app.service.MuteifyService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val HOME_RULE_ID = 1L
        const val HOME_RULE_NAME = "Dom"
    }

    private val ruleDao = AppDatabase.getInstance(application).ruleDao()
    private val settingsRepository = SettingsRepository(application)

    private val _ssid = MutableStateFlow("")
    val ssid: StateFlow<String> = _ssid

    private val _actionEnter = MutableStateFlow(SoundAction.UNSILENCE)
    val actionEnter: StateFlow<SoundAction> = _actionEnter

    private val _actionLeave = MutableStateFlow(SoundAction.SILENCE)
    val actionLeave: StateFlow<SoundAction> = _actionLeave

    private val _morningTime = MutableStateFlow("06:00")
    val morningTime: StateFlow<String> = _morningTime

    private val _nightTime = MutableStateFlow("22:00")
    val nightTime: StateFlow<String> = _nightTime

    private val _morningScheduleEnabled = MutableStateFlow(true)
    val morningScheduleEnabled: StateFlow<Boolean> = _morningScheduleEnabled

    private val _morningScheduleAction = MutableStateFlow(SoundAction.UNSILENCE)
    val morningScheduleAction: StateFlow<SoundAction> = _morningScheduleAction

    private val _morningSchedulePolicy = MutableStateFlow(SchedulePolicy.REQUIRE_CONFIRMATION)
    val morningSchedulePolicy: StateFlow<SchedulePolicy> = _morningSchedulePolicy

    private val _morningCountdownSeconds = MutableStateFlow(30)
    val morningCountdownSeconds: StateFlow<Int> = _morningCountdownSeconds

    private val _eveningScheduleEnabled = MutableStateFlow(true)
    val eveningScheduleEnabled: StateFlow<Boolean> = _eveningScheduleEnabled

    private val _eveningScheduleAction = MutableStateFlow(SoundAction.SILENCE)
    val eveningScheduleAction: StateFlow<SoundAction> = _eveningScheduleAction

    private val _eveningSchedulePolicy = MutableStateFlow(SchedulePolicy.AUTO_AFTER_COUNTDOWN)
    val eveningSchedulePolicy: StateFlow<SchedulePolicy> = _eveningSchedulePolicy

    private val _eveningCountdownSeconds = MutableStateFlow(30)
    val eveningCountdownSeconds: StateFlow<Int> = _eveningCountdownSeconds

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _hasNotificationPolicyAccess = MutableStateFlow(false)
    val hasNotificationPolicyAccess: StateFlow<Boolean> = _hasNotificationPolicyAccess

    init {
        refreshPermissionStatus()
        observeSavedHomeRule()
        observeScheduleSettings()
    }

    fun onSsidChanged(value: String) { _ssid.value = value }
    fun onActionEnterChanged(value: SoundAction) { _actionEnter.value = value }
    fun onActionLeaveChanged(value: SoundAction) { _actionLeave.value = value }
    fun onMorningTimeChanged(value: String) {
        _morningTime.value = value.toTimeInput()
        saveScheduleTimes()
    }
    fun onNightTimeChanged(value: String) {
        _nightTime.value = value.toTimeInput()
        saveScheduleTimes()
    }
    fun onMorningScheduleEnabledChanged(value: Boolean) {
        _morningScheduleEnabled.value = value
        saveMorningScheduleSettings()
    }
    fun onMorningScheduleActionChanged(value: SoundAction) {
        _morningScheduleAction.value = value
        saveMorningScheduleSettings()
    }
    fun onMorningSchedulePolicyChanged(value: SchedulePolicy) {
        _morningSchedulePolicy.value = value
        saveMorningScheduleSettings()
    }
    fun onMorningCountdownSecondsChanged(value: String) {
        _morningCountdownSeconds.value = value.toCountdownSeconds()
        saveMorningScheduleSettings()
    }
    fun onEveningScheduleEnabledChanged(value: Boolean) {
        _eveningScheduleEnabled.value = value
        saveEveningScheduleSettings()
    }
    fun onEveningScheduleActionChanged(value: SoundAction) {
        _eveningScheduleAction.value = value
        saveEveningScheduleSettings()
    }
    fun onEveningSchedulePolicyChanged(value: SchedulePolicy) {
        _eveningSchedulePolicy.value = value
        saveEveningScheduleSettings()
    }
    fun onEveningCountdownSecondsChanged(value: String) {
        _eveningCountdownSeconds.value = value.toCountdownSeconds()
        saveEveningScheduleSettings()
    }

    fun refreshPermissionStatus() {
        val notificationManager =
            getApplication<Application>().getSystemService(NotificationManager::class.java)
        _hasNotificationPolicyAccess.value = notificationManager.isNotificationPolicyAccessGranted
    }

    fun openNotificationPolicySettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun toggleService() {
        val context = getApplication<Application>()
        if (_isRunning.value) {
            context.stopService(Intent(context, MuteifyService::class.java))
            _isRunning.value = false
        } else {
            if (_ssid.value.isBlank()) return
            saveHomeRule()
            val intent = Intent(context, MuteifyService::class.java).apply {
                putExtra(MuteifyService.EXTRA_SSID, _ssid.value)
                putExtra(MuteifyService.EXTRA_ACTION_ENTER, _actionEnter.value.name)
                putExtra(MuteifyService.EXTRA_ACTION_LEAVE, _actionLeave.value.name)
            }
            context.startForegroundService(intent)
            _isRunning.value = true
        }
    }

    private fun observeSavedHomeRule() {
        viewModelScope.launch {
            ruleDao.getAllRules().collectLatest { rules ->
                if (_isRunning.value) return@collectLatest
                val homeRule = rules.firstOrNull { it.id == HOME_RULE_ID } ?: return@collectLatest
                _ssid.value = homeRule.wifiSsid
                _actionEnter.value = homeRule.actionEnter.toSoundActionOr(SoundAction.UNSILENCE)
                _actionLeave.value = homeRule.actionLeave.toSoundActionOr(SoundAction.SILENCE)
            }
        }
    }

    private fun saveHomeRule() {
        val homeRule = RuleEntity(
            id = HOME_RULE_ID,
            name = HOME_RULE_NAME,
            wifiSsid = _ssid.value,
            actionEnter = _actionEnter.value.name,
            actionLeave = _actionLeave.value.name,
            isEnabled = true
        )
        viewModelScope.launch {
            ruleDao.insertRule(homeRule)
        }
    }

    private fun observeScheduleSettings() {
        viewModelScope.launch {
            settingsRepository.scheduleSettings.collectLatest { settings ->
                _morningTime.value = settings.morningTime.toTimeInput()
                _nightTime.value = settings.nightTime.toTimeInput()
                _morningScheduleEnabled.value = settings.morning.enabled
                _morningScheduleAction.value = settings.morning.action
                _morningSchedulePolicy.value = settings.morning.policy
                _morningCountdownSeconds.value = settings.morning.countdownSeconds
                _eveningScheduleEnabled.value = settings.evening.enabled
                _eveningScheduleAction.value = settings.evening.action
                _eveningSchedulePolicy.value = settings.evening.policy
                _eveningCountdownSeconds.value = settings.evening.countdownSeconds
            }
        }
    }

    private fun saveScheduleTimes() {
        val morningTime = _morningTime.value
        val nightTime = _nightTime.value
        viewModelScope.launch {
            settingsRepository.saveScheduleTimes(morningTime, nightTime)
        }
    }

    private fun saveMorningScheduleSettings() {
        val settings = ScheduleSlotSettings(
            enabled = _morningScheduleEnabled.value,
            time = _morningTime.value,
            action = _morningScheduleAction.value,
            policy = _morningSchedulePolicy.value,
            countdownSeconds = _morningCountdownSeconds.value
        )
        viewModelScope.launch {
            settingsRepository.saveMorningScheduleSettings(settings)
        }
    }

    private fun saveEveningScheduleSettings() {
        val settings = ScheduleSlotSettings(
            enabled = _eveningScheduleEnabled.value,
            time = _nightTime.value,
            action = _eveningScheduleAction.value,
            policy = _eveningSchedulePolicy.value,
            countdownSeconds = _eveningCountdownSeconds.value
        )
        viewModelScope.launch {
            settingsRepository.saveEveningScheduleSettings(settings)
        }
    }

    private fun String.toSoundActionOr(default: SoundAction): SoundAction {
        return runCatching { SoundAction.valueOf(this) }.getOrDefault(default)
    }

    private fun String.toTimeInput(): String {
        return filter { it.isDigit() || it == ':' }.take(5)
    }

    private fun String.toCountdownSeconds(): Int {
        return filter { it.isDigit() }
            .take(3)
            .toIntOrNull()
            ?.coerceIn(0, 300)
            ?: 0
    }
}
