package com.example.ui.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppLockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystem: Boolean,
    val isLocked: Boolean
)

class AppSelectionViewModel(
    private val context: Context,
    private val repository: AppLockRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isSystemFilterActive = MutableStateFlow(false)
    val isSystemFilterActive: StateFlow<Boolean> = _isSystemFilterActive

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val installedAppsFlow = MutableStateFlow<List<AppItem>>(emptyList())

    val appsListState: StateFlow<List<AppItem>> = combine(
        installedAppsFlow,
        repository.protectedApps,
        _searchQuery,
        _isSystemFilterActive
    ) { installedApps, protectedApps, query, filterSystem ->
        val protectedPackageNames = protectedApps.map { it.packageName }.toSet()
        
        installedApps.map { app ->
            app.copy(isLocked = protectedPackageNames.contains(app.packageName))
        }.filter { app ->
            val matchesQuery = app.label.contains(query, ignoreCase = true) || 
                               app.packageName.contains(query, ignoreCase = true)
            val matchesFilter = if (filterSystem) app.isSystem else !app.isSystem
            matchesQuery && matchesFilter
        }.sortedBy { it.label.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApplications()
    }

    private fun loadInstalledApplications() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val appItems = mutableListOf<AppItem>()
                
                val ourPackageName = context.packageName

                for (info in packages) {
                    // Skip our own app from this selection (it has self-protection setting instead)
                    if (info.packageName == ourPackageName) continue

                    // Filter out system packages that are critical or empty to ensure smooth performance
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (isSystem) {
                        // Skip core low-level system services that shouldn't be locked
                        if (info.packageName == "android" || 
                            info.packageName == "com.android.systemui" ||
                            info.packageName == "com.android.settings" ||
                            info.packageName == "com.android.providers.settings"
                        ) continue
                    }

                    try {
                        val label = pm.getApplicationLabel(info).toString()
                        // Ensure the package has a valid launcher intent or is a typical app
                        val launchIntent = pm.getLaunchIntentForPackage(info.packageName)
                        if (launchIntent != null || !isSystem) {
                            val icon = pm.getApplicationIcon(info)
                            appItems.add(
                                AppItem(
                                    packageName = info.packageName,
                                    label = label,
                                    icon = icon,
                                    isSystem = isSystem,
                                    isLocked = false
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Ignore packages that fail to load
                    }
                }
                appItems
            }
            installedAppsFlow.value = apps
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSystemFilter(showSystem: Boolean) {
        _isSystemFilterActive.value = showSystem
    }

    fun toggleAppLock(app: AppItem) {
        viewModelScope.launch {
            if (app.isLocked) {
                repository.unprotectApp(app.packageName)
            } else {
                repository.protectApp(app.packageName)
            }
        }
    }

    class Factory(
        private val context: Context,
        private val repository: AppLockRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppSelectionViewModel(context, repository) as T
        }
    }
}
