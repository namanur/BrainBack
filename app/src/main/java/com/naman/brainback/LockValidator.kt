package com.naman.brainback

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

class LockValidator(private val context: Context) {

    fun isSystemReady(): Boolean {
        return hasAccessibilityService() && hasUsageStatsPermission() && canDrawOverlays()
    }

    fun hasAccessibilityService(): Boolean {
        val expectedService = "${context.packageName}/com.naman.brainback.BlockerService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(expectedService)
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }
}
