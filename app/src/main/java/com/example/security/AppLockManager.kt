package com.example.security

import android.content.Context
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

class AppLockManager(context: Context) {
    // Whitelist structure: packageName -> timestamp (either of unlock or last exit)
    private val unlockedApps = ConcurrentHashMap<String, Long>()

    private val prefs = context.getSharedPreferences("app_lock_security_state", Context.MODE_PRIVATE)

    fun isAppWhitelisted(packageName: String, lockBehavior: String, lockDelayMs: Long): Boolean {
        if (isLockedOut()) return false
        
        val unlockTime = unlockedApps[packageName] ?: return false
        
        return when (lockBehavior) {
            "IMMEDIATELY" -> {
                // Locked immediately upon exit, but remains unlocked while user is interacting with it.
                // The transition logic is coordinated by onPackageChanged.
                true
            }
            "SCREEN_OFF" -> {
                // Stays unlocked until screen is turned off or explicitly locked
                true
            }
            "CUSTOM_DELAY" -> {
                // Stays unlocked if the elapsed time since last exit is within the delay threshold
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - unlockTime
                val isWithinDelay = elapsed < lockDelayMs
                if (!isWithinDelay) {
                    unlockedApps.remove(packageName)
                }
                isWithinDelay
            }
            else -> true
        }
    }

    fun whitelistApp(packageName: String) {
        unlockedApps[packageName] = SystemClock.elapsedRealtime()
    }

    fun lockApp(packageName: String) {
        unlockedApps.remove(packageName)
    }

    fun clearWhitelist() {
        unlockedApps.clear()
    }

    fun onPackageChanged(currentPackage: String?, previousPackage: String?, lockBehavior: String, lockDelayMs: Long) {
        val now = SystemClock.elapsedRealtime()
        
        if (previousPackage != null && previousPackage != currentPackage) {
            // User left a protected app
            if (lockBehavior == "IMMEDIATELY") {
                unlockedApps.remove(previousPackage)
            } else if (lockBehavior == "CUSTOM_DELAY") {
                // Save the exit time as the starting point for the custom lock delay countdown
                if (unlockedApps.containsKey(previousPackage)) {
                    unlockedApps[previousPackage] = now
                }
            }
        }
    }

    // Failed attempts & lockout
    fun handleFailedAttempt() {
        val currentFailed = getFailedAttempts() + 1
        setFailedAttempts(currentFailed)
        if (currentFailed >= 5) {
            val lockoutDuration = when {
                currentFailed >= 10 -> 120_000L // 2 minutes
                currentFailed >= 7 -> 60_000L  // 1 minute
                else -> 30_000L                 // 30 seconds
            }
            setLockoutEndTime(SystemClock.elapsedRealtime() + lockoutDuration)
        }
    }

    fun resetFailedAttempts() {
        setFailedAttempts(0)
        setLockoutEndTime(0L)
    }

    fun isLockedOut(): Boolean {
        return SystemClock.elapsedRealtime() < getLockoutEndTime()
    }

    fun getLockoutRemainingSeconds(): Long {
        val remainingMs = getLockoutEndTime() - SystemClock.elapsedRealtime()
        return if (remainingMs > 0) remainingMs / 1000 else 0L
    }
    
    fun getFailedAttempts(): Int {
        return prefs.getInt("failed_attempts", 0)
    }

    private fun setFailedAttempts(attempts: Int) {
        prefs.edit().putInt("failed_attempts", attempts).apply()
    }

    private fun getLockoutEndTime(): Long {
        return prefs.getLong("lockout_end_time", 0L)
    }

    private fun setLockoutEndTime(endTime: Long) {
        prefs.edit().putLong("lockout_end_time", endTime).apply()
    }
}
