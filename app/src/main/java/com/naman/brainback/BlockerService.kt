package com.naman.brainback

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BlockerService : AccessibilityService() {

    private lateinit var frictionManager: FrictionManager

    override fun onCreate() {
        super.onCreate()
        frictionManager = FrictionManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return

        // FORTRESS HARDENING: Extreme Self-Defense during Lock
        if (frictionManager.isLocked()) {
            val isSystemSettings = pkg.contains("settings")
            val isInstaller = pkg.contains("packageinstaller") || pkg.contains("installer")

            if (isSystemSettings || isInstaller) {
                // If we are in settings or installer AND "Brainback" is visible, block it immediately.
                if (hasNodeByText(root, "Brainback")) {
                    Log.d("Brainback", "Fortress: Blocking access to $pkg to prevent bypass.")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    root.recycle()
                    return
                }
            }
        }

        // Standard Firewall Logic (only if NOT on break)
        if (!frictionManager.isOnBreak()) {
            when {
                pkg == "com.google.android.youtube" -> if (isShortsView(root)) executeBlock(pkg)
                pkg == "com.instagram.android" -> if (isInstagramReels(root)) executeBlock(pkg)
                pkg == "com.snapchat.android" -> if (isSnapchatSpotlight(root)) executeBlock(pkg)
                isKnownBrowser(pkg) -> if (isBrowserShorts(root, pkg)) executeBlock(pkg)
            }
        }

        root.recycle()
    }

    private fun executeBlock(pkg: String) {
        incrementBlockCount(pkg)
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun incrementBlockCount(pkg: String) {
        val prefs = getSharedPreferences("brainback_stats", Context.MODE_PRIVATE)
        prefs.edit().putInt("block_count_$pkg", prefs.getInt("block_count_$pkg", 0) + 1).apply()
        prefs.edit().putInt("total_blocks", prefs.getInt("total_blocks", 0) + 1).apply()
    }

    private fun isKnownBrowser(pkg: String): Boolean = listOf("com.android.chrome", "com.brave.browser", "org.mozilla.firefox").contains(pkg)

    private fun isShortsView(root: AccessibilityNodeInfo): Boolean {
        val ids = listOf("com.google.android.youtube:id/reel_player_fragment_container", "com.google.android.youtube:id/reel_recycler", "com.google.android.youtube:id/shorts_player_view")
        return hasNodeByAnyId(root, ids)
    }

    private fun isInstagramReels(root: AccessibilityNodeInfo): Boolean {
        val ids = listOf("com.instagram.android:id/reels_video_container", "com.instagram.android:id/clips_video_container")
        return hasNodeByAnyId(root, ids) || hasNodeByText(root, "Reels")
    }

    private fun isSnapchatSpotlight(root: AccessibilityNodeInfo): Boolean {
        val ids = listOf("com.snapchat.android:id/spotlight_tab_container", "com.snapchat.android:id/spotlight_vertical_pager")
        return hasNodeByAnyId(root, ids) || (hasNodeByText(root, "Spotlight") && !hasNodeByText(root, "Chat"))
    }

    private fun isBrowserShorts(root: AccessibilityNodeInfo, pkg: String): Boolean {
        val urlBarIds = listOf("com.android.chrome:id/url_bar", "com.brave.browser:id/url_bar", "org.mozilla.firefox:id/url_bar_title")
        for (id in urlBarIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val url = nodes[0].text?.toString()?.lowercase() ?: ""
                if (url.contains("/shorts") || url.contains("/reels")) return true
            }
        }
        return false
    }

    private fun hasNodeByAnyId(root: AccessibilityNodeInfo, ids: List<String>): Boolean {
        for (id in ids) if (!root.findAccessibilityNodeInfosByViewId(id).isNullOrEmpty()) return true
        return false
    }

    private fun hasNodeByText(root: AccessibilityNodeInfo, text: String): Boolean = !root.findAccessibilityNodeInfosByText(text).isNullOrEmpty()

    override fun onInterrupt() {}
}
