# Skill: android-usagestats

Accurate retrieval and aggregation of Digital Wellbeing data using `UsageStatsManager`.

## Pattern: Precise Today Stats
Use `UsageEvents` instead of `queryUsageStats` for high accuracy. Iterating over events allows precise measurement of foreground/background transitions.

```kotlin
val events = usageStatsManager.queryEvents(startOfDay, System.currentTimeMillis())
val event = UsageEvents.Event()
while (events.hasNextEvent()) {
    events.getNextEvent(event)
    // Process MOVE_TO_FOREGROUND and MOVE_TO_BACKGROUND
}
```

## Pattern: Counting Unlocks
Phone unlocks can be counted by filtering `UsageEvents` for the `KEYGUARD_HIDDEN` event type.

```kotlin
if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
    unlockCount++
}
```

## Pattern: Caching & TTL
`UsageStatsManager` queries are CPU intensive. Always cache results in SharedPreferences or a Repository with a Time-To-Live (e.g., 1 hour).

## Common Pitfalls
- **Missing Permission**: `PACKAGE_USAGE_STATS` is a "Special App Access" permission. Redirect users to `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
- **Query Ranges**: Android may discard granular data older than 7 days.
- **Manufacturer Quirks**: Some Chinese OEMs (Xiaomi, Oppo) may restrict event access unless the app is whitelisted from battery optimization.
