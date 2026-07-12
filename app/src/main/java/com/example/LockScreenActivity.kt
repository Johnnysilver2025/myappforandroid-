package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.lock.LockScreen
import com.example.ui.lock.LockScreenViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class LockScreenActivity : FragmentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (targetPackageName.isNullOrEmpty()) {
            finish()
            return
        }

        // Intercept back presses to send the user home, preventing bypass
        onBackPressedDispatcher.addCallback(this) {
            sendUserHome()
        }

        val app = application as AppLockApplication

        setContent {
            val viewModel: LockScreenViewModel = viewModel(
                factory = LockScreenViewModel.Factory(
                    context = this@LockScreenActivity,
                    repository = app.container.repository,
                    appLockManager = app.container.appLockManager,
                    targetPackageName = targetPackageName
                )
            )

            val darkModeChoice by viewModel.darkMode.collectAsStateWithLifecycle()
            val isDark = when (darkModeChoice) {
                "DARK" -> true
                "LIGHT" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // Lockout check or successful unlock checks
                LaunchedEffect(uiState.unlockSuccess) {
                    if (uiState.unlockSuccess) {
                        finish()
                    }
                }

                LaunchedEffect(uiState.recoverySuccess) {
                    if (uiState.recoverySuccess) {
                        delay(1200)
                        val restartIntent = Intent(this@LockScreenActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(restartIntent)
                        finish()
                    }
                }

                // Automatically trigger system biometric verification if enabled & available
                LaunchedEffect(uiState.biometricAvailable) {
                    if (uiState.biometricAvailable && !uiState.isLockedOut && !uiState.isRecoveryMode) {
                        viewModel.triggerBiometricPrompt(this@LockScreenActivity) {}
                    }
                }

                LockScreen(
                    uiState = uiState,
                    onPinEntered = { pin -> viewModel.verifyPin(pin) },
                    onPatternEntered = { pattern -> viewModel.verifyPattern(pattern) },
                    onRecoveryAnswered = { answer -> viewModel.verifyRecoveryAnswer(answer) },
                    onToggleRecovery = { enabled -> viewModel.toggleRecoveryMode(enabled) },
                    onBiometricTriggered = {
                        viewModel.triggerBiometricPrompt(this@LockScreenActivity) {}
                    },
                    onExitClicked = {
                        sendUserHome()
                    }
                )
            }
        }
    }

    private fun sendUserHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }
}
