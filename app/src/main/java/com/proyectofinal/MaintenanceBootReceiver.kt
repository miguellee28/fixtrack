package com.proyectofinal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MaintenanceBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MaintenanceNotificationScheduler.scheduleAll(context)
        }
    }
}
