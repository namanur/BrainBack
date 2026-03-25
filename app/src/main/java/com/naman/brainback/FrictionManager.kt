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

    fun startLock(minutes: Int = 30) {
        val quote = quotes[SecureRandom().nextInt(quotes.size)]
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        prefs.edit().apply {
            putString("active_quote", quote)
            putLong("lock_until", endTime)
            putBoolean("is_locked", true)
            apply()
        }
    }

    fun isLocked(): Boolean {
        val endTime = prefs.getLong("lock_until", 0)
        return System.currentTimeMillis() < endTime && prefs.getBoolean("is_locked", false)
    }

    fun getRemainingMillis(): Long {
        val endTime = prefs.getLong("lock_until", 0)
        return (endTime - System.currentTimeMillis()).coerceAtLeast(0)
    }

    fun getQuoteIfExpired(): String? {
        return if (System.currentTimeMillis() >= prefs.getLong("lock_until", 0) && prefs.getBoolean("is_locked", false)) {
            prefs.getString("active_quote", null)
        } else null
    }

    fun unlock(input: String): Boolean {
        val actual = prefs.getString("active_quote", null)
        // Clean input to be case-insensitive and trimmed
        return if (actual != null && input.trim().equals(actual.trim(), ignoreCase = true)) {
            prefs.edit().clear().apply()
            true
        } else false
    }
}
