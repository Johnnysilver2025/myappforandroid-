package com.example.ui.lock

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppLockRepository
import com.example.security.AppLockManager
import com.example.security.HashUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockScreenUiState(
    val appLabel: String = "",
    val appIcon: Drawable? = null,
    val passwordType: String = "PIN", // PIN or PATTERN
    val isLockedOut: Boolean = false,
    val lockoutRemainingSeconds: Long = 0L,
    val failedAttempts: Int = 0,
    val biometricAvailable: Boolean = false,
    val isRecoveryMode: Boolean = false,
    val securityQuestion: String = "",
    val recoverySuccess: Boolean = false,
    val recoveryError: Boolean = false,
    val unlockSuccess: Boolean = false
)

class LockScreenViewModel(
    private val context: Context,
    private val repository: AppLockRepository,
    private val appLockManager: AppLockManager,
    private val targetPackageName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockScreenUiState())
    val uiState: StateFlow<LockScreenUiState> = _uiState.asStateFlow()

    val darkMode: StateFlow<String> = repository.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    private var timerJob: Job? = null

    init {
        loadAppDetails()
        loadLockSettings()
        checkBiometrics()
        startLockoutTimerIfNeeded()
    }

    private fun loadAppDetails() {
        viewModelScope.launch {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(targetPackageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                _uiState.update { it.copy(appLabel = label, appIcon = icon) }
            } catch (e: Exception) {
                _uiState.update { it.copy(appLabel = targetPackageName) }
            }
        }
    }

    private fun loadLockSettings() {
        viewModelScope.launch {
            val pType = repository.passwordType.first()
            val questionIdx = repository.securityQuestionIndex.first()
            val questions = listOf(
                "What was the name of your first pet?",
                "In what city were you born?",
                "What is your mother's maiden name?",
                "What was your childhood nickname?",
                "What was the name of your primary school?"
            )
            val question = questions.getOrElse(questionIdx) { questions[0] }
            
            _uiState.update { 
                it.copy(
                    passwordType = pType,
                    securityQuestion = question,
                    failedAttempts = appLockManager.getFailedAttempts()
                )
            }
        }
    }

    private fun checkBiometrics() {
        viewModelScope.launch {
            val biometricEnabled = repository.biometricEnabled.first()
            if (!biometricEnabled) {
                _uiState.update { it.copy(biometricAvailable = false) }
                return@launch
            }

            val biometricManager = BiometricManager.from(context)
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            val canAuthenticate = biometricManager.canAuthenticate(authenticators)
            
            _uiState.update { 
                it.copy(biometricAvailable = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) 
            }
        }
    }

    private fun startLockoutTimerIfNeeded() {
        timerJob?.cancel()
        if (appLockManager.isLockedOut()) {
            _uiState.update { 
                it.copy(
                    isLockedOut = true,
                    lockoutRemainingSeconds = appLockManager.getLockoutRemainingSeconds()
                ) 
            }
            timerJob = viewModelScope.launch {
                while (appLockManager.isLockedOut()) {
                    _uiState.update { 
                        it.copy(lockoutRemainingSeconds = appLockManager.getLockoutRemainingSeconds()) 
                    }
                    delay(1000)
                }
                _uiState.update { 
                    it.copy(
                        isLockedOut = false,
                        lockoutRemainingSeconds = 0L,
                        failedAttempts = appLockManager.getFailedAttempts()
                    ) 
                }
            }
        } else {
            _uiState.update { 
                it.copy(
                    isLockedOut = false,
                    lockoutRemainingSeconds = 0L,
                    failedAttempts = appLockManager.getFailedAttempts()
                ) 
            }
        }
    }

    fun verifyPin(pin: String): Boolean {
        if (appLockManager.isLockedOut()) return false
        
        var success = false
        viewModelScope.launch {
            val storedHash = repository.passwordHash.first() ?: ""
            val storedSalt = repository.passwordSalt.first() ?: ""
            val inputHash = HashUtils.hashPassword(pin, storedSalt)
            
            if (inputHash == storedHash) {
                success = true
                handleUnlockSuccess()
            } else {
                handleUnlockFailure()
            }
        }
        return success
    }

    fun verifyPattern(pattern: List<Int>): Boolean {
        if (appLockManager.isLockedOut()) return false
        
        val patternString = pattern.joinToString(",")
        var success = false
        viewModelScope.launch {
            val storedHash = repository.passwordHash.first() ?: ""
            val storedSalt = repository.passwordSalt.first() ?: ""
            val inputHash = HashUtils.hashPassword(patternString, storedSalt)
            
            if (inputHash == storedHash) {
                success = true
                handleUnlockSuccess()
            } else {
                handleUnlockFailure()
            }
        }
        return success
    }

    fun verifyRecoveryAnswer(answer: String) {
        viewModelScope.launch {
            val storedHash = repository.securityQuestionAnswerHash.first() ?: ""
            val storedSalt = repository.passwordSalt.first() ?: "" // reuse salt
            val inputHash = HashUtils.hashPassword(answer.trim().lowercase(), storedSalt)
            
            if (inputHash == storedHash) {
                // Recovered! Clear credentials and allow user to re-onboard
                repository.resetAll()
                repository.logEvent("SECURITY_RECOVERED", "User successfully bypassed AppLock via recovery question.")
                _uiState.update { it.copy(recoverySuccess = true, recoveryError = false) }
            } else {
                _uiState.update { it.copy(recoveryError = true) }
                repository.logEvent("RECOVERY_FAILED", "Failed security answer attempt.")
            }
        }
    }

    private fun handleUnlockSuccess() {
        appLockManager.resetFailedAttempts()
        appLockManager.whitelistApp(targetPackageName)
        _uiState.update { it.copy(unlockSuccess = true, failedAttempts = 0) }
    }

    private fun handleUnlockFailure() {
        appLockManager.handleFailedAttempt()
        _uiState.update { it.copy(failedAttempts = appLockManager.getFailedAttempts()) }
        startLockoutTimerIfNeeded()
    }

    fun toggleRecoveryMode(enabled: Boolean) {
        _uiState.update { it.copy(isRecoveryMode = enabled, recoveryError = false) }
    }

    fun triggerBiometricPrompt(activity: FragmentActivity, onFinish: () -> Unit) {
        if (!_uiState.value.biometricAvailable || appLockManager.isLockedOut()) return

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    handleUnlockSuccess()
                    onFinish()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    handleUnlockFailure()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock ${uiState.value.appLabel}")
            .setSubtitle("Use your biometric credential to access the application")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Use Password")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    class Factory(
        private val context: Context,
        private val repository: AppLockRepository,
        private val appLockManager: AppLockManager,
        private val targetPackageName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LockScreenViewModel(context, repository, appLockManager, targetPackageName) as T
        }
    }
}
