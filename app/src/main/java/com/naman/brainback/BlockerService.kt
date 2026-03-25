package com.naman.brainback

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.naman.brainback.data.BlockEvent
import com.naman.brainback.data.BrainbackDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BlockerService : AccessibilityService() {

    private lateinit var frictionManager: FrictionManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isBlockingActive = true
    
    // Debounce to prevent multiple logs for the same block event
    private var lastBlockTime = 0L
    private val DEBOUNCE_MS = 2000L

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "is_blocking_active") {
            isBlockingActive = prefs.getBoolean(key, true)
            Log.d("Brainback", "Blocking state changed: $isBlockingActive")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val prefs = getSharedPreferences("brainback_prefs", Context.MODE_PRIVATE)
        isBlockingActive = prefs.getBoolean("is_blocking_active", true)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        
        frictionManager = FrictionManager(this)
        Log.d("Brainback", "Service connected. Blocking active: $isBlockingActive")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // BUG FIX 2: Guard check first
        if (!isBlockingActive) return

        val pkg = event.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return

        // FORTRESS HARDENING: Extreme Self-Defense during Lock
        if (frictionManager.isLocked()) {
            val isSystemSettings = pkg.contains("settings")
            val isInstaller = pkg.contains("packageinstaller") || pkg.contains("installer")

            if (isSystemSettings || isInstaller) {
                if (hasNodeByText(root, "Brainback")) {
                    Log.d("Brainback", "Fortress: Blocking access to $pkg to prevent bypass.")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    root.recycle()
                    return
                }
            }
        }

        // Standard Firewall Logic
        when {
            pkg == "com.google.android.youtube" -> if (isShortsView(root)) executeBlock(pkg, "YouTube Shorts")
            pkg == "com.instagram.android" -> if (isInstagramReels(root)) executeBlock(pkg, "Instagram Reels")
            pkg == "com.snapchat.android" -> if (isSnapchatSpotlight(root)) executeBlock(pkg, "Snapchat Spotlight")
            isKnownBrowser(pkg) -> if (isBrowserShorts(root, pkg)) executeBlock(pkg, "Browser Shorts")
        }

        root.recycle()
    }

    private fun executeBlock(pkg: String, label: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlockTime < DEBOUNCE_MS) return
        lastBlockTime = currentTime

        Log.d("Brainback", "Block triggered for $pkg ($label)")
        
        // Asynchronous logging to Room
        serviceScope.launch(Dispatchers.IO) {
            val database = BrainbackDatabase.getDatabase(applicationContext)
            database.blockEventDao().insert(BlockEvent(packageName = pkg, appLabel = label))
        }

        // Standard back action
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        // Update legacy counters for widget/ui parity
        incrementLegacyStats(pkg)
    }

    private fun incrementLegacyStats(pkg: String) {
        val prefs = getSharedPreferences("brainback_stats", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("block_count_$pkg", prefs.getInt("block_count_$pkg", 0) + 1)
            putInt("total_blocks", prefs.getInt("total_blocks", 0) + 1)
        }.commit()
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

    override fun onDestroy() {
        super.onDestroy()
        val prefs = getSharedPreferences("brainback_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onInterrupt() {}
}
