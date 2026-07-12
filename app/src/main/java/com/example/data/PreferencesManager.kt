package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aegis_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val IS_ONBOARDED = booleanPreferencesKey("is_onboarded")
        private val PASSWORD_TYPE = stringPreferencesKey("password_type") // PIN or PATTERN
        private val PASSWORD_HASH = stringPreferencesKey("password_hash")
        private val PASSWORD_SALT = stringPreferencesKey("password_salt")
        private val SECURITY_QUESTION_INDEX = intPreferencesKey("security_question_index")
        private val SECURITY_QUESTION_ANSWER_HASH = stringPreferencesKey("security_question_answer_hash")
        
        private val LOCK_BEHAVIOR = stringPreferencesKey("lock_behavior") // IMMEDIATELY, SCREEN_OFF, CUSTOM_DELAY
        private val LOCK_DELAY_MS = longPreferencesKey("lock_delay_ms")
        private val IS_APPLOCK_ENABLED = booleanPreferencesKey("is_applock_enabled")
        private val LOCK_APPLOCK_ITSELF = booleanPreferencesKey("lock_applock_itself")
        private val AUTO_RESTART_PROTECTION = booleanPreferencesKey("auto_restart_protection")
        private val NEW_APP_DETECTION = booleanPreferencesKey("new_app_detection")
        private val DARK_MODE = stringPreferencesKey("dark_mode") // SYSTEM, LIGHT, DARK
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    val isOnboardedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_ONBOARDED] ?: false
    }

    suspend fun setOnboarded(onboarded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_ONBOARDED] = onboarded
        }
    }

    val passwordTypeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PASSWORD_TYPE] ?: "PIN"
    }

    suspend fun setPasswordType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[PASSWORD_TYPE] = type
        }
    }

    val passwordHashFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PASSWORD_HASH]
    }

    val passwordSaltFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PASSWORD_SALT]
    }

    suspend fun setPasswordDetails(hash: String, salt: String) {
        context.dataStore.edit { prefs ->
            prefs[PASSWORD_HASH] = hash
            prefs[PASSWORD_SALT] = salt
        }
    }

    val securityQuestionIndexFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[SECURITY_QUESTION_INDEX] ?: 0
    }

    val securityQuestionAnswerHashFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SECURITY_QUESTION_ANSWER_HASH]
    }

    suspend fun setSecurityQuestionDetails(index: Int, hash: String) {
        context.dataStore.edit { prefs ->
            prefs[SECURITY_QUESTION_INDEX] = index
            prefs[SECURITY_QUESTION_ANSWER_HASH] = hash
        }
    }

    val lockBehaviorFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LOCK_BEHAVIOR] ?: "IMMEDIATELY"
    }

    suspend fun setLockBehavior(behavior: String) {
        context.dataStore.edit { prefs ->
            prefs[LOCK_BEHAVIOR] = behavior
        }
    }

    val lockDelayMsFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LOCK_DELAY_MS] ?: 0L
    }

    suspend fun setLockDelayMs(delayMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[LOCK_DELAY_MS] = delayMs
        }
    }

    val isAppLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_APPLOCK_ENABLED] ?: true
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_APPLOCK_ENABLED] = enabled
        }
    }

    val lockAppLockItselfFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[LOCK_APPLOCK_ITSELF] ?: true
    }

    suspend fun setLockAppLockItself(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[LOCK_APPLOCK_ITSELF] = enabled
        }
    }

    val autoRestartProtectionFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_RESTART_PROTECTION] ?: true
    }

    suspend fun setAutoRestartProtection(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_RESTART_PROTECTION] = enabled
        }
    }

    val newAppDetectionFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NEW_APP_DETECTION] ?: true
    }

    suspend fun setNewAppDetection(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NEW_APP_DETECTION] = enabled
        }
    }

    val darkModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE] ?: "SYSTEM"
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE] = mode
        }
    }

    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[VIBRATION_ENABLED] ?: true
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[VIBRATION_ENABLED] = enabled
        }
    }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED] ?: true
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED] = enabled
        }
    }
    
    // Clear credentials on reset
    suspend fun clearCredentials() {
        context.dataStore.edit { prefs ->
            prefs.remove(PASSWORD_HASH)
            prefs.remove(PASSWORD_SALT)
            prefs.remove(SECURITY_QUESTION_ANSWER_HASH)
            prefs.remove(IS_ONBOARDED)
        }
    }
}
