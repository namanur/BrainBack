# Brainback Architecture

## Component Diagram
```
[UI] MainActivity / Compose
      ↓
[Logic] WeeklyPerformanceViewModel
      ↓
[Data] UsageStatsRepository / Room Database / SharedPreferences
      ↑
[Service] BlockerService (AccessibilityService)
```

## Data Flow: Blocking Pipeline
1. **Event**: User opens YouTube Shorts.
2. **Detection**: BlockerService receive WindowStateChanged event.
3. **Guard**: Checks `is_blocking_active` pref.
4. **Logic**: Identifies `reel_player_fragment_container` view ID.
5. **Action**: Fires `GLOBAL_ACTION_BACK`.
6. **Log**: Async insert into Room `block_events` table.

## Data Flow: Stats Pipeline
1. **Request**: UI opens Weekly Dashboard.
2. **Aggregate**: ViewModel calls Repository.
3. **Query**: Repository parses Android `UsageEvents`.
4. **Display**: Vico renders charts from aggregated data.

## SharedPreferences Registry
- `brainback_prefs`: `is_blocking_active`, `lock_start_time`, `lock_until`, `is_locked`, `active_quote`.
- `brainback_stats`: `total_blocks`, `block_count_<package>`.
