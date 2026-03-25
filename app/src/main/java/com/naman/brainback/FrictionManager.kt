package com.naman.brainback

import android.content.Context
import java.security.SecureRandom

class FrictionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("brainback_prefs", Context.MODE_PRIVATE)

    private val quotes = listOf(
        "I am in control of my digital life.",
        "Focus is a muscle I am building today.",
        "Short form content is a loop I choose to break.",
        "My attention is my most valuable resource.",
        "Peace is found in the space between scrolls."
    )

    // START A LOCK (PUNISHMENT OR FOCUS)
    fun startLock(minutes: Int, isFocusMode: Boolean = false) {
        val quote = quotes[SecureRandom().nextInt(quotes.size)]
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (minutes * 60 * 1000)
        
        prefs.edit().apply {
            putString("active_quote", quote)
            putLong("lock_start_time", startTime)
            putLong("lock_until", endTime)
            putBoolean("is_locked", true)
            putBoolean("is_focus_mode", isFocusMode)
            putBoolean("is_blocking_active", true)
        }.commit() // Use commit for critical state
    }

    // START THE BREAK
    fun startBreak(minutes: Int) {
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        prefs.edit().apply {
            putLong("break_until", endTime)
            putBoolean("is_on_break", true)
            putBoolean("is_locked", false)
            putBoolean("is_blocking_active", false) // Stop blocking during break
            putString("active_quote", null)
        }.commit()
    }

    fun setBlockingActive(active: Boolean) {
        prefs.edit().putBoolean("is_blocking_active", active).commit()
    }

    fun isBlockingActive(): Boolean {
        return prefs.getBoolean("is_blocking_active", true)
    }

    fun isOnBreak(): Boolean {
        val endTime = prefs.getLong("break_until", 0)
        val active = prefs.getBoolean("is_on_break", false)
        if (System.currentTimeMillis() > endTime && active) {
            prefs.edit().putBoolean("is_on_break", false).commit()
            return false
        }
        return active
    }

    fun isLocked(): Boolean {
        val endTime = prefs.getLong("lock_until", 0)
        val locked = prefs.getBoolean("is_locked", false)
        if (System.currentTimeMillis() >= endTime && locked) {
            // Lock expired, but we don't clear it here anymore
            // The UI will handle the transition to "Challenge Pending"
            return false
        }
        return locked && System.currentTimeMillis() < endTime
    }

    fun getRemainingMillis(): Long {
        val lockUntil = prefs.getLong("lock_until", 0)
        val breakUntil = prefs.getLong("break_until", 0)
        
        return if (isLocked()) {
            (lockUntil - System.currentTimeMillis()).coerceAtLeast(0)
        } else if (isOnBreak()) {
            (breakUntil - System.currentTimeMillis()).coerceAtLeast(0)
        } else 0L
    }

    fun getQuoteIfExpired(): String? {
        val lockUntil = prefs.getLong("lock_until", 0)
        val isLocked = prefs.getBoolean("is_locked", false)
        return if (System.currentTimeMillis() >= lockUntil && isLocked) {
            prefs.getString("active_quote", null)
        } else null
    }

    fun unlock(input: String): Boolean {
        val actual = prefs.getString("active_quote", null)
        return if (actual != null && input.trim().equals(actual.trim(), ignoreCase = true)) {
            // Once unlocked, user gets a break (default 30 mins)
            startBreak(30)
            true
        } else false
    }
    
    fun clearLock() {
        prefs.edit().apply {
            putBoolean("is_locked", false)
            putLong("lock_start_time", 0L)
            putLong("lock_until", 0L)
            putString("active_quote", null)
            putBoolean("is_blocking_active", false)
        }.commit()
    }
}
