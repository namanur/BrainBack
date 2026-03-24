package com.naman.brainback

import android.content.Context
import java.security.SecureRandom

class FrictionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("friction_lock", Context.MODE_PRIVATE)

    fun startLock(minutes: Int = 30) {
        val password = generatePassword()
        val endTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        prefs.edit().apply {
            putString("temp_password", password)
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

    fun getPasswordIfExpired(): String? {
        return if (System.currentTimeMillis() >= prefs.getLong("lock_until", 0)) {
            prefs.getString("temp_password", null)
        } else null
    }

    fun unlock(input: String): Boolean {
        val actual = prefs.getString("temp_password", null)
        return if (actual != null && input == actual) {
            prefs.edit().clear().apply()
            true
        } else false
    }

    private fun generatePassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$"
        val random = SecureRandom()
        return (1..16)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }
}
