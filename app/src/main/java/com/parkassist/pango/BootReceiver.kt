package com.parkassist.pango

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val repository = ParkingRepository(context)
        if (repository.isMonitoringEnabled()) {
            val serviceIntent = Intent(context, ParkingMonitorService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
