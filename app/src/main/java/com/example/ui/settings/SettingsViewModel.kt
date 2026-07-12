package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityLog
import com.example.data.AppLockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AppLockRepository
) : ViewModel() {

    val darkMode: StateFlow<String> = repository.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val lockBehavior: StateFlow<String> = repository.lockBehavior
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "IMMEDIATELY")

    val lockAppLockItself: StateFlow<Boolean> = repository.lockAppLockItself
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoRestartProtection: StateFlow<Boolean> = repository.autoRestartProtection
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val newAppDetection: StateFlow<Boolean> = repository.newAppDetection
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrationEnabled: StateFlow<Boolean> = repository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val biometricEnabled: StateFlow<Boolean> = repository.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val activityLogs: StateFlow<List<ActivityLog>> = repository.activityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backupString = MutableStateFlow<String?>(null)
    val backupString: StateFlow<String?> = _backupString.asStateFlow()

    private val _backupImportStatus = MutableStateFlow<Boolean?>(null)
    val backupImportStatus: StateFlow<Boolean?> = _backupImportStatus.asStateFlow()

    fun setDarkMode(mode: String) {
        viewModelScope.launch {
            repository.setDarkMode(mode)
        }
    }

    fun setLockBehavior(behavior: String) {
        viewModelScope.launch {
            repository.setLockBehavior(behavior)
            val delayMs = when (behavior) {
                "CUSTOM_DELAY" -> 60_000L // default 1 minute delay
                else -> 0L
            }
            repository.setLockDelayMs(delayMs)
        }
    }

    fun setLockAppLockItself(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLockAppLockItself(enabled)
        }
    }

    fun setAutoRestartProtection(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoRestartProtection(enabled)
        }
    }

    fun setNewAppDetection(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNewAppDetection(enabled)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVibrationEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun triggerBackupExport() {
        viewModelScope.launch {
            try {
                val encryptedStr = repository.exportBackup()
                _backupString.value = encryptedStr
            } catch (e: Exception) {
                _backupString.value = null
            }
        }
    }

    fun clearBackupString() {
        _backupString.value = null
    }

    fun triggerBackupImport(base64Data: String) {
        viewModelScope.launch {
            val success = repository.importBackup(base64Data)
            _backupImportStatus.value = success
        }
    }

    fun clearImportStatus() {
        _backupImportStatus.value = null
    }

    fun factoryReset() {
        viewModelScope.launch {
            repository.resetAll()
        }
    }

    class Factory(
        private val repository: AppLockRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
