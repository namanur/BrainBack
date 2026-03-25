package com.naman.brainback

import android.content.Context
import java.security.SecureRandom

class FrictionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("friction_lock", Context.MODE_PRIVATE)

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
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        prefs.edit().apply {
            putString("active_quote", quote)
            putLong("lock_until", endTime)
            putBoolean("is_locked", true)
            putBoolean("is_focus_mode", isFocusMode)
            apply()
        }
    }

    // START THE BREAK
    fun startBreak(minutes: Int) {
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        prefs.edit().apply {
            putLong("break_until", endTime)
            putBoolean("is_on_break", true)
            // Clear lock data
            putBoolean("is_locked", false)
            putString("active_quote", null)
            apply()
        }
    }

    fun isOnBreak(): Boolean {
        val endTime = prefs.getLong("break_until", 0)
        val active = prefs.getBoolean("is_on_break", false)
        if (System.currentTimeMillis() > endTime) {
            prefs.edit().putBoolean("is_on_break", false).apply()
            return false
        }
        return active
    }

    fun isLocked(): Boolean {
        val endTime = prefs.getLong("lock_until", 0)
        return System.currentTimeMillis() < endTime && prefs.getBoolean("is_locked", false)
    }

    fun getRemainingMillis(): Long {
        val endTime = if (isLocked()) prefs.getLong("lock_until", 0) else prefs.getLong("break_until", 0)
        return (endTime - System.currentTimeMillis()).coerceAtLeast(0)
    }

    fun getQuoteIfExpired(): String? {
        return if (System.currentTimeMillis() >= prefs.getLong("lock_until", 0) && prefs.getBoolean("is_locked", false)) {
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
}
