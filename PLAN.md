# Brainback v6.0 Implementation Plan

## 1. Dependency Graph & Order of Operations
1.  **Skills Implementation**: Create reusable patterns for Accessibility, Stats, Room, and HardLock.
2.  **Data Layer Upgrade**: 
    *   Setup Room Database for `BlockEvent` logging.
    *   Create `UsageStatsRepository` for aggregated data.
3.  **Bug Fixes**:
    *   Harden `FrictionManager` and `MainActivity` for persistent lock states.
    *   Add `is_blocking_active` guard to `BlockerService`.
4.  **New Features**:
    *   Implement `BootReceiver` for lock persistence after restart.
    *   Build "Weekly Performance" screen with Vico charts.
    *   Extend JSON Export to v2.
5.  **Documentation**: Update README, Architecture, and Setup guides.

## 2. File Changes
### Create
- `app/src/main/java/com/naman/brainback/data/BlockEvent.kt` (Room Entity)
- `app/src/main/java/com/naman/brainback/data/BlockEventDao.kt` (Room DAO)
- `app/src/main/java/com/naman/brainback/data/BrainbackDatabase.kt` (Room Database)
- `app/src/main/java/com/naman/brainback/data/UsageStatsRepository.kt` (Repository)
- `app/src/main/java/com/naman/brainback/BootReceiver.kt` (BroadcastReceiver)
- `app/src/main/java/com/naman/brainback/ui/WeeklyPerformanceScreen.kt` (New Screen)
- `docs/SETUP_GUIDE.md`
- `docs/ARCHITECTURE.md`

### Modify
- `app/src/main/java/com/naman/brainback/BlockerService.kt`: Add Room logging, real-time pref listener, and blocking guard.
- `app/src/main/java/com/naman/brainback/FrictionManager.kt`: Use `commit()` and ensure robust state.
- `app/src/main/java/com/naman/brainback/MainActivity.kt`: Refactor UI, implement timer persistence, and new navigation.
- `app/src/main/AndroidManifest.xml`: Add `RECEIVE_BOOT_COMPLETED` and register `BootReceiver`.
- `app/build.gradle.kts`: Add Room, Vico, and Boot permissions.

## 3. New Dependencies
- **Room**: `androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler`.
- **Vico Charts**: `com.patrykandpatrick.vico:compose`, `com.patrykandpatrick.vico:core`.
- **Coroutines**: `org.jetbrains.kotlinx:kotlinx-coroutines-android`.

## 4. Risks & Edge Cases
- **Device Reboot**: Lock must persist. Handled by `BootReceiver`.
- **Stat Latency**: `UsageStatsManager` can be slow. Handled by 1-hour caching in Repository.
- **Accessibility Service Crash**: System might kill the service. Handled by `is_blocking_active` persistent state.
- **Noisy UIs**: Multiple events per block. Handled by adding a timestamp-based debounce in `BlockerService`.
