# Skill: android-hardlock-timer

Reliable, reboot-persistent countdown timers for behavioral intervention.

## Pattern: SharedPreferences Sync
Never rely on `CountDownTimer` state alone. Always write the `lock_start_time` to SharedPreferences using `commit()` immediately.

```kotlin
fun startLock(context: Context) {
    prefs.edit()
        .putLong("lock_start_time", System.currentTimeMillis())
        .putBoolean("is_active", true)
        .commit() // Mandatory for critical state
}
```

## Pattern: Recovery on Launch
On every `onResume()`, calculate the elapsed time and resume or terminate the lock.

```kotlin
val elapsed = System.currentTimeMillis() - prefs.getLong("lock_start_time", 0)
val remaining = (30 * 60 * 1000L) - elapsed
if (remaining > 0) {
    // Re-initialize timer with `remaining`
} else {
    // Terminate lock
}
```

## Pattern: Boot Restoration
Use a `BOOT_COMPLETED` receiver to re-register the firewall guard if a lock was active before the reboot.

## Common Pitfalls
- **Using apply()**: `apply()` is asynchronous and can lose data if the process is killed immediately after the call. Use `commit()`.
- **System Clock Changes**: Users might change the system time to cheat. Mitigation: Periodically sync with a network time or use `SystemClock.elapsedRealtime()` (though this resets on reboot).
- **UI Inconsistency**: Ensure the "Start" button is physically disabled (`isEnabled = false`) to prevent multiple overlapping timers.
