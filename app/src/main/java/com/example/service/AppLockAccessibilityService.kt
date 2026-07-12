package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import com.example.AppLockApplication
import com.example.LockScreenActivity
import com.example.data.AppLockRepository
import com.example.security.AppLockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: AppLockRepository
    private lateinit var appLockManager: AppLockManager

    private var protectedPackages = setOf<String>()
    private var isAppLockEnabled = true
    private var lockAppLockItself = true
    private var lockBehavior = "IMMEDIATELY"
    private var lockDelayMs = 0L

    private var previousPackage: String? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                appLockManager.clearWhitelist()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as AppLockApplication
        repository = app.container.repository
        appLockManager = app.container.appLockManager

        // Register screen off receiver to lock all apps when screen goes off
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)

        // Reactively observe configurations from preferences and DB to keep local service cache fast & lightweight
        serviceScope.launch {
            repository.protectedApps.collect { apps ->
                protectedPackages = apps.map { it.packageName }.toSet()
            }
        }
        serviceScope.launch {
            repository.isAppLockEnabled.collect { enabled ->
                isAppLockEnabled = enabled
            }
        }
        serviceScope.launch {
            repository.lockAppLockItself.collect { enabled ->
                lockAppLockItself = enabled
            }
        }
        serviceScope.launch {
            repository.lockBehavior.collect { behavior ->
                lockBehavior = behavior
            }
        }
        serviceScope.launch {
            repository.lockDelayMs.collect { delay ->
                lockDelayMs = delay
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!isAppLockEnabled) return

        val packageName = event.packageName?.toString() ?: return
        val ourPackageName = packageNameContext()

        // Ignore standard Android package changes that shouldn't be intercepted
        if (packageName == "android" || packageName == "com.android.systemui" || packageName == "com.android.launcher3" || packageName.contains("launcher")) {
            return
        }

        // Handle leaving a package in the LockState manager
        if (previousPackage != packageName) {
            appLockManager.onPackageChanged(packageName, previousPackage, lockBehavior, lockDelayMs)
            previousPackage = packageName
        }

        // Determine if target application needs locking
        val isTargetProtected = protectedPackages.contains(packageName) || 
                                (packageName == ourPackageName && lockAppLockItself)

        if (isTargetProtected) {
            val isWhitelisted = appLockManager.isAppWhitelisted(packageName, lockBehavior, lockDelayMs)
            if (!isWhitelisted) {
                // If it's not whitelisted, launch the overlay lock screen
                launchLockOverlay(packageName)
            }
        }
    }

    private fun launchLockOverlay(targetPackageName: String) {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(LockScreenActivity.EXTRA_PACKAGE_NAME, targetPackageName)
        }
        startActivity(intent)
    }

    private fun packageNameContext(): String {
        return applicationContext.packageName
    }

    override fun onInterrupt() {
        // Required callback
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        serviceScope.cancel()
    }
}
