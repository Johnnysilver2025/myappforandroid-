package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.apps.AppSelectionViewModel
import com.example.ui.onboarding.OnboardingViewModel
import com.example.ui.screens.AppSelectionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AppLockApplication
        val repository = app.container.repository

        setContent {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.Factory(repository)
            )

            val darkModeChoice by mainViewModel.darkMode.collectAsStateWithLifecycle()
            val isDark = when (darkModeChoice) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                val isOnboarded by mainViewModel.isOnboarded.collectAsStateWithLifecycle()
                val lockedAppsCount by mainViewModel.lockedAppsCount.collectAsStateWithLifecycle()
                val isAppLockEnabled by mainViewModel.isAppLockEnabled.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        isOnboarded = isOnboarded,
                        lockedAppsCount = lockedAppsCount,
                        isAppLockEnabled = isAppLockEnabled,
                        mainViewModel = mainViewModel,
                        app = app
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    isOnboarded: Boolean,
    lockedAppsCount: Int,
    isAppLockEnabled: Boolean,
    mainViewModel: MainViewModel,
    app: AppLockApplication
) {
    val navController = rememberNavController()

    // Determine startup destination
    val startDestination = if (isOnboarded) "dashboard" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ONBOARDING ROUTE
        composable("onboarding") {
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.Factory(app.container.repository)
            )
            val uiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()

            OnboardingScreen(
                viewModel = onboardingViewModel,
                uiState = uiState,
                onOnboardingFinished = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // DASHBOARD ROUTE
        composable("dashboard") {
            DashboardScreen(
                lockedAppsCount = lockedAppsCount,
                isAppLockEnabled = isAppLockEnabled,
                onToggleGlobalLock = { enabled ->
                    mainViewModel.toggleAppLockEnabled(enabled)
                },
                onNavigateToAppSelection = {
                    navController.navigate("app_selection")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        // APP SELECTION ROUTE
        composable("app_selection") {
            val appSelectionViewModel: AppSelectionViewModel = viewModel(
                factory = AppSelectionViewModel.Factory(app, app.container.repository)
            )
            val appsList by appSelectionViewModel.appsListState.collectAsStateWithLifecycle()
            val searchQuery by appSelectionViewModel.searchQuery.collectAsStateWithLifecycle()
            val isSystemFilterActive by appSelectionViewModel.isSystemFilterActive.collectAsStateWithLifecycle()
            val isLoading by appSelectionViewModel.isLoading.collectAsStateWithLifecycle()

            AppSelectionScreen(
                viewModel = appSelectionViewModel,
                appsList = appsList,
                searchQuery = searchQuery,
                isSystemFilterActive = isSystemFilterActive,
                isLoading = isLoading,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // SETTINGS ROUTE
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.container.repository)
            )

            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetCredentials = {
                    mainViewModel.forceResetToOnboarding()
                    navController.navigate("onboarding") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}
