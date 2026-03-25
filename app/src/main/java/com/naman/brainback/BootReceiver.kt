package com.naman.brainback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val frictionManager = FrictionManager(context)
            if (frictionManager.isLocked()) {
                Log.d("Brainback", "Boot detected. Lock is still active. Re-arming firewall.")
                // The AccessibilityService will start automatically if enabled,
                // and it will check frictionManager.isLocked() to re-enable self-defense.
                // We ensure the blocking state is active.
                frictionManager.setBlockingActive(true)
            }
        }
    }
}
