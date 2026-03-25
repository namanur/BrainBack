# Skill: android-accessibility-service

Efficient and robust implementation of Android Accessibility Services for behavioral blocking.

## Pattern: SharedPreferences Guard
Always check a persistent boolean before executing heavy UI tree inspections or blocking actions.

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    if (!prefs.getBoolean("is_active", true)) return
    
    // Core logic here
}
```

## Pattern: Real-time Configuration
Use `OnSharedPreferenceChangeListener` to react to UI toggles without restarting the service.

```kotlin
private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    if (key == "is_active") {
        this.isActive = prefs.getBoolean(key, true)
    }
}

override fun onServiceConnected() {
    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.registerOnSharedPreferenceChangeListener(prefListener)
}
```

## Pattern: System Action Trigger
Use `performGlobalAction(GLOBAL_ACTION_BACK)` to exit loops. Mimics user behavior and is less disruptive than killing the app.

## Common Pitfalls
- **Memory-only state**: Services can be killed. Always persist state in SharedPreferences with `commit()`.
- **Heavy UI Tree Inspection**: Only inspect when necessary (e.g., when the package name matches a target).
- **Missing recycle()**: Always call `root.recycle()` to avoid memory leaks in AccessibilityNodeInfo objects.
