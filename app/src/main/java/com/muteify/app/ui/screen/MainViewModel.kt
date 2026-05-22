package com.muteify.app.ui.screen

import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.muteify.app.data.model.RuleEntity
import com.muteify.app.data.model.SoundAction
import com.muteify.app.data.repository.AppDatabase
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

    private val _ssid = MutableStateFlow("")
    val ssid: StateFlow<String> = _ssid

    private val _actionEnter = MutableStateFlow(SoundAction.UNSILENCE)
    val actionEnter: StateFlow<SoundAction> = _actionEnter

    private val _actionLeave = MutableStateFlow(SoundAction.SILENCE)
    val actionLeave: StateFlow<SoundAction> = _actionLeave

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _hasNotificationPolicyAccess = MutableStateFlow(false)
    val hasNotificationPolicyAccess: StateFlow<Boolean> = _hasNotificationPolicyAccess

    init {
        refreshPermissionStatus()
        observeSavedHomeRule()
    }

    fun onSsidChanged(value: String) { _ssid.value = value }
    fun onActionEnterChanged(value: SoundAction) { _actionEnter.value = value }
    fun onActionLeaveChanged(value: SoundAction) { _actionLeave.value = value }

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

    private fun String.toSoundActionOr(default: SoundAction): SoundAction {
        return runCatching { SoundAction.valueOf(this) }.getOrDefault(default)
    }
}
