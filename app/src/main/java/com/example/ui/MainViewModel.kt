package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppLockRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AppLockRepository
) : ViewModel() {

    val isOnboarded: StateFlow<Boolean> = repository.isOnboarded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAppLockEnabled: StateFlow<Boolean> = repository.isAppLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lockedAppsCount: StateFlow<Int> = repository.protectedApps
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val darkMode: StateFlow<String> = repository.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    fun toggleAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAppLockEnabled(enabled)
        }
    }

    fun forceResetToOnboarding() {
        viewModelScope.launch {
            repository.resetAll()
        }
    }

    class Factory(
        private val repository: AppLockRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
