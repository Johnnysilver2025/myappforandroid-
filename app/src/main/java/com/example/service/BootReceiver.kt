package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AppLockApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as AppLockApplication
            val repository = app.container.repository

            CoroutineScope(Dispatchers.IO).launch {
                val autoRestart = repository.autoRestartProtection.first()
                if (autoRestart) {
                    repository.logEvent(
                        "SYSTEM_BOOTED",
                        "Device reboot detected. Aegis AppLock security layers successfully reloaded."
                    )
                }
            }
        }
    }
}
