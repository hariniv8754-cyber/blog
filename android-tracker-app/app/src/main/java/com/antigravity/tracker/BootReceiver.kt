package com.antigravity.tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            val prefs = context.getSharedPreferences("quantum_tracker_prefs", Context.MODE_PRIVATE)
            val isTracking = prefs.getBoolean("is_tracking", false)
            val roomId = prefs.getString("room_id", "default-room") ?: "default-room"
            val userName = prefs.getString("user_name", "Friend (Android)") ?: "Friend (Android)"

            if (isTracking) {
                val serviceIntent = Intent(context, TrackingService::class.java).apply {
                    action = TrackingService.ACTION_START
                    putExtra(TrackingService.EXTRA_ROOM_ID, roomId)
                    putExtra(TrackingService.EXTRA_USER_NAME, userName)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
