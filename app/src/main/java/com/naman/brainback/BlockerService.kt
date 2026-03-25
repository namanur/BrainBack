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

        // SELF-PRESERVATION LOGIC
        if (pkg == "com.android.settings" && frictionManager.isLocked()) {
            if (hasNodeByText(root, "Brainback")) {
                Log.d("Brainback", "Self-preservation triggered — blocking settings access")
                performGlobalAction(GLOBAL_ACTION_BACK)
                root.recycle()
                return
            }
        }

        when {
            pkg == "com.google.android.youtube" -> {
                if (isShortsView(root)) {
                    executeImmediateBlock(pkg)
                }
            }
            pkg == "com.instagram.android" -> {
                if (isInstagramReels(root)) {
                    executeImmediateBlock(pkg)
                }
            }
            pkg == "com.snapchat.android" -> {
                if (isSnapchatSpotlight(root)) {
                    executeImmediateBlock(pkg)
                }
            }
            isKnownBrowser(pkg) -> {
                if (isBrowserShorts(root, pkg)) {
                    executeImmediateBlock(pkg)
                }
            }
        }

        root.recycle()
    }

    private fun executeImmediateBlock(pkg: String) {
        Log.d("Brainback", "Short-form detected in $pkg — immediate block")
        incrementBlockCount(pkg)
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun incrementBlockCount(pkg: String) {
        val prefs = getSharedPreferences("brainback_stats", Context.MODE_PRIVATE)
        val currentCount = prefs.getInt("block_count_$pkg", 0)
        prefs.edit().putInt("block_count_$pkg", currentCount + 1).apply()
        
        val totalBlocks = prefs.getInt("total_blocks", 0)
        prefs.edit().putInt("total_blocks", totalBlocks + 1).apply()
    }

    private fun isKnownBrowser(pkg: String): Boolean {
        return listOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
            "com.brave.browser", "org.mozilla.firefox", "org.mozilla.focus",
            "com.opera.browser", "com.microsoft.emmx", "com.sec.android.app.sbrowser",
            "com.vivaldi.browser"
        ).contains(pkg)
    }

    private fun isShortsView(root: AccessibilityNodeInfo): Boolean {
        val shortsViewIds = listOf(
            "com.google.android.youtube:id/reel_player_fragment_container",
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/shorts_player_view"
        )
        return hasNodeByAnyId(root, shortsViewIds)
    }

    private fun isInstagramReels(root: AccessibilityNodeInfo): Boolean {
        val reelsIds = listOf(
            "com.instagram.android:id/reels_video_container",
            "com.instagram.android:id/clips_video_container"
        )
        return hasNodeByAnyId(root, reelsIds) || hasNodeByText(root, "Reels")
    }

    private fun isSnapchatSpotlight(root: AccessibilityNodeInfo): Boolean {
        // Spotlight typically has a "Spotlight" label or specific vertical pager IDs
        val spotlightIds = listOf(
            "com.snapchat.android:id/spotlight_tab_container",
            "com.snapchat.android:id/spotlight_vertical_pager"
        )
        // We look for "Spotlight" text but must ensure it's not just the bottom navigation tab label
        return hasNodeByAnyId(root, spotlightIds) || (hasNodeByText(root, "Spotlight") && !hasNodeByText(root, "Chat"))
    }

    private fun isBrowserShorts(root: AccessibilityNodeInfo, pkg: String): Boolean {
        val urlBarIds = listOf(
            "com.android.chrome:id/url_bar",
            "com.brave.browser:id/url_bar",
            "org.mozilla.firefox:id/url_bar_title",
            "com.opera.browser:id/url_field",
            "com.microsoft.emmx:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.vivaldi.browser:id/url_bar"
        )

        for (id in urlBarIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) {
                val url = nodes[0].text?.toString()?.lowercase() ?: ""
                if (url.contains("/shorts") || url.contains("/reels")) {
                    return true
                }
            }
        }
        return false
    }

    private fun hasNodeByAnyId(root: AccessibilityNodeInfo, ids: List<String>): Boolean {
        for (id in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (!nodes.isNullOrEmpty()) return true
        }
        return false
    }

    private fun hasNodeByText(root: AccessibilityNodeInfo, text: String): Boolean {
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return !nodes.isNullOrEmpty()
    }

    override fun onInterrupt() {}
}
