package com.proyectofinal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MaintenanceBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val resultadoPendiente = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    MaintenanceNotificationScheduler.scheduleAll(context.applicationContext)
                } finally {
                    resultadoPendiente.finish()
                }
            }
        }
    }
}
