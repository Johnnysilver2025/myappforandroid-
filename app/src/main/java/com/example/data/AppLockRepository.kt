package com.example.data

import android.util.Base64
import com.example.security.CryptoManager
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class BackupModel(
    val version: Int,
    val passwordType: String,
    val passwordHash: String,
    val passwordSalt: String,
    val securityQuestionIndex: Int,
    val securityQuestionAnswerHash: String,
    val lockBehavior: String,
    val lockDelayMs: Long,
    val lockAppLockItself: Boolean,
    val protectedApps: List<String>
)

class AppLockRepository(
    private val appLockDao: AppLockDao,
    private val preferencesManager: PreferencesManager,
    private val cryptoManager: CryptoManager
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val backupAdapter = moshi.adapter(BackupModel::class.java)

    // Exposed Flows from Preferences
    val isOnboarded: Flow<Boolean> = preferencesManager.isOnboardedFlow
    val passwordType: Flow<String> = preferencesManager.passwordTypeFlow
    val passwordHash: Flow<String?> = preferencesManager.passwordHashFlow
    val passwordSalt: Flow<String?> = preferencesManager.passwordSaltFlow
    val securityQuestionIndex: Flow<Int> = preferencesManager.securityQuestionIndexFlow
    val securityQuestionAnswerHash: Flow<String?> = preferencesManager.securityQuestionAnswerHashFlow
    val lockBehavior: Flow<String> = preferencesManager.lockBehaviorFlow
    val lockDelayMs: Flow<Long> = preferencesManager.lockDelayMsFlow
    val isAppLockEnabled: Flow<Boolean> = preferencesManager.isAppLockEnabledFlow
    val lockAppLockItself: Flow<Boolean> = preferencesManager.lockAppLockItselfFlow
    val autoRestartProtection: Flow<Boolean> = preferencesManager.autoRestartProtectionFlow
    val newAppDetection: Flow<Boolean> = preferencesManager.newAppDetectionFlow
    val darkMode: Flow<String> = preferencesManager.darkModeFlow
    val vibrationEnabled: Flow<Boolean> = preferencesManager.vibrationEnabledFlow
    val biometricEnabled: Flow<Boolean> = preferencesManager.biometricEnabledFlow

    // Exposed Flows from Room
    val protectedApps: Flow<List<ProtectedApp>> = appLockDao.getProtectedAppsFlow()
    val activityLogs: Flow<List<ActivityLog>> = appLockDao.getActivityLogsFlow()

    // Protected App Management
    suspend fun protectApp(packageName: String) = withContext(Dispatchers.IO) {
        appLockDao.protectApp(ProtectedApp(packageName))
        logEvent("PROTECTION_TOGGLED", "Locked app: $packageName")
    }

    suspend fun unprotectApp(packageName: String) = withContext(Dispatchers.IO) {
        appLockDao.unprotectApp(packageName)
        logEvent("PROTECTION_TOGGLED", "Unlocked app: $packageName")
    }

    suspend fun isAppProtected(packageName: String): Boolean = withContext(Dispatchers.IO) {
        appLockDao.isAppProtected(packageName)
    }

    suspend fun getProtectedPackageNames(): List<String> = withContext(Dispatchers.IO) {
        appLockDao.getProtectedPackageNames()
    }

    // Settings Updates
    suspend fun setOnboarded(onboarded: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setOnboarded(onboarded)
    }

    suspend fun setPasswordDetails(type: String, hash: String, salt: String) = withContext(Dispatchers.IO) {
        preferencesManager.setPasswordType(type)
        preferencesManager.setPasswordDetails(hash, salt)
        logEvent("PASSWORD_CHANGED", "Lock verification updated to $type")
    }

    suspend fun setSecurityQuestionDetails(index: Int, hash: String) = withContext(Dispatchers.IO) {
        preferencesManager.setSecurityQuestionDetails(index, hash)
        logEvent("SECURITY_RESET_CONFIGURED", "Security recovery question index: $index updated")
    }

    suspend fun setLockBehavior(behavior: String) = withContext(Dispatchers.IO) {
        preferencesManager.setLockBehavior(behavior)
        logEvent("SETTINGS_CHANGED", "Lock behavior set to $behavior")
    }

    suspend fun setLockDelayMs(delayMs: Long) = withContext(Dispatchers.IO) {
        preferencesManager.setLockDelayMs(delayMs)
    }

    suspend fun setAppLockEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setAppLockEnabled(enabled)
        logEvent("SETTINGS_CHANGED", "AppLock global protection set to $enabled")
    }

    suspend fun setLockAppLockItself(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setLockAppLockItself(enabled)
        logEvent("SETTINGS_CHANGED", "Self protection set to $enabled")
    }

    suspend fun setAutoRestartProtection(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setAutoRestartProtection(enabled)
    }

    suspend fun setNewAppDetection(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setNewAppDetection(enabled)
    }

    suspend fun setDarkMode(mode: String) = withContext(Dispatchers.IO) {
        preferencesManager.setDarkMode(mode)
    }

    suspend fun setVibrationEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setVibrationEnabled(enabled)
    }

    suspend fun setBiometricEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesManager.setBiometricEnabled(enabled)
    }

    suspend fun resetAll() = withContext(Dispatchers.IO) {
        preferencesManager.clearCredentials()
        appLockDao.clearAllProtectedApps()
        appLockDao.clearLogs()
    }

    // Activity Logs
    suspend fun logEvent(eventType: String, details: String) = withContext(Dispatchers.IO) {
        appLockDao.insertLog(ActivityLog(eventType = eventType, details = details))
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        appLockDao.clearLogs()
    }

    // Secure Offline Backup & Restore
    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val backup = BackupModel(
            version = 1,
            passwordType = preferencesManager.passwordTypeFlow.first(),
            passwordHash = preferencesManager.passwordHashFlow.first() ?: "",
            passwordSalt = preferencesManager.passwordSaltFlow.first() ?: "",
            securityQuestionIndex = preferencesManager.securityQuestionIndexFlow.first(),
            securityQuestionAnswerHash = preferencesManager.securityQuestionAnswerHashFlow.first() ?: "",
            lockBehavior = preferencesManager.lockBehaviorFlow.first(),
            lockDelayMs = preferencesManager.lockDelayMsFlow.first(),
            lockAppLockItself = preferencesManager.lockAppLockItselfFlow.first(),
            protectedApps = appLockDao.getProtectedPackageNames()
        )

        val jsonString = backupAdapter.toJson(backup)
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        
        // Encrypt JSON payload using Android Keystore Master Key
        val (encryptedBytes, iv) = cryptoManager.encrypt(jsonBytes)
        
        // Combine IV and Encrypted Bytes: [IV Length (1 byte)] + [IV] + [Encrypted Content]
        val combined = ByteArray(1 + iv.size + encryptedBytes.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)
        
        logEvent("BACKUP_EXPORTED", "Local encrypted settings exported")
        Base64.encodeToString(combined, Base64.DEFAULT or Base64.NO_WRAP)
    }

    suspend fun importBackup(backupBase64: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val combined = Base64.decode(backupBase64, Base64.DEFAULT)
            if (combined.isEmpty()) return@withContext false
            
            val ivSize = combined[0].toInt()
            if (ivSize <= 0 || combined.size < 1 + ivSize) return@withContext false
            
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 1, iv, 0, ivSize)
            
            val encryptedSize = combined.size - 1 - ivSize
            if (encryptedSize <= 0) return@withContext false
            
            val encryptedBytes = ByteArray(encryptedSize)
            System.arraycopy(combined, 1 + ivSize, encryptedBytes, 0, encryptedSize)
            
            // Decrypt JSON bytes using Android Keystore Master Key
            val jsonBytes = cryptoManager.decrypt(encryptedBytes, iv)
            val jsonString = String(jsonBytes, Charsets.UTF_8)
            
            val backup = backupAdapter.fromJson(jsonString) ?: return@withContext false
            
            // Restore Preferences
            preferencesManager.setPasswordType(backup.passwordType)
            preferencesManager.setPasswordDetails(backup.passwordHash, backup.passwordSalt)
            preferencesManager.setSecurityQuestionDetails(backup.securityQuestionIndex, backup.securityQuestionAnswerHash)
            preferencesManager.setLockBehavior(backup.lockBehavior)
            preferencesManager.setLockDelayMs(backup.lockDelayMs)
            preferencesManager.setLockAppLockItself(backup.lockAppLockItself)
            preferencesManager.setOnboarded(true)
            
            // Restore Room Database (clear and batch insert)
            appLockDao.clearAllProtectedApps()
            val appsToInsert = backup.protectedApps.map { ProtectedApp(it) }
            appLockDao.insertProtectedApps(appsToInsert)
            
            logEvent("BACKUP_RESTORED", "Local encrypted settings imported")
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
