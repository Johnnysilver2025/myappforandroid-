package com.example.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppLockRepository
import com.example.security.HashUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 0, // 0: Intro, 1: Lock Type, 2: Set Password, 3: Set Security Question, 4: Permissions Guide, 5: Done
    val passwordType: String = "PIN", // PIN or PATTERN
    val tempFirstPasswordInput: String = "",
    val tempConfirmPasswordInput: String = "",
    val passwordErrorMsg: String? = null,
    val securityQuestionIndex: Int = 0,
    val securityAnswer: String = "",
    val securityErrorMsg: String? = null,
    val onboardingComplete: Boolean = false
)

class OnboardingViewModel(
    private val repository: AppLockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val securityQuestions = listOf(
        "What was the name of your first pet?",
        "In what city were you born?",
        "What is your mother's maiden name?",
        "What was your childhood nickname?",
        "What was the name of your primary school?"
    )

    fun nextStep() {
        _uiState.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun selectPasswordType(type: String) {
        _uiState.update { it.copy(passwordType = type, tempFirstPasswordInput = "", tempConfirmPasswordInput = "", passwordErrorMsg = null) }
        nextStep()
    }

    fun setFirstPasswordInput(input: String) {
        _uiState.update { it.copy(tempFirstPasswordInput = input, passwordErrorMsg = null) }
    }

    fun confirmPasswordAndNext(confirmInput: String): Boolean {
        val state = _uiState.value
        if (confirmInput != state.tempFirstPasswordInput) {
            _uiState.update { it.copy(passwordErrorMsg = "Passwords do not match. Please try again.") }
            return false
        }

        // Credentials match!
        viewModelScope.launch {
            val salt = HashUtils.generateSalt()
            val hash = HashUtils.hashPassword(confirmInput, salt)
            repository.setPasswordDetails(state.passwordType, hash, salt)
            _uiState.update { it.copy(tempConfirmPasswordInput = confirmInput, passwordErrorMsg = null) }
            nextStep()
        }
        return true
    }

    fun setSecurityQuestionDetails(index: Int, answer: String) {
        if (answer.trim().isBlank()) {
            _uiState.update { it.copy(securityErrorMsg = "Answer cannot be blank.") }
            return
        }

        viewModelScope.launch {
            val salt = HashUtils.generateSalt() // we can generate or reuse
            val answerHash = HashUtils.hashPassword(answer.trim().lowercase(), salt)
            
            // Set details
            repository.setSecurityQuestionDetails(index, answerHash)
            _uiState.update { it.copy(securityQuestionIndex = index, securityAnswer = answer, securityErrorMsg = null) }
            nextStep()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboarded(true)
            repository.logEvent("APP_INITIALIZED", "Aegis AppLock onboarding completed successfully.")
            _uiState.update { it.copy(onboardingComplete = true) }
        }
    }

    class Factory(
        private val repository: AppLockRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(repository) as T
        }
    }
}
