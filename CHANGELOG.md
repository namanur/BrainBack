# Changelog

## [v6.0] - 2026-03-25
### Fixed
- Hard Lock timer now persists across app restarts using SharedPreferences commit() and recovery logic.
- Aggressive blocking now correctly stops when Hard Lock expires or during break windows.
- Stats accuracy overhaul: Migrated to UsageEvents API for precise today-only measurement.
- AccessibilityService now reacts to firewall toggle changes in real time via preference listeners.

### Added
- **Weekly Performance Dashboard**: Integrated Vico charts for 7-day screen time and unlock trends.
- **Persistent Event Logging**: Room database for detailed block intervention history.
- **Fortress Self-Defense**: Integrated Device Admin to prevent uninstallation during active lock periods.
- **Boot Persistence**: BroadcastReceiver to restore lock state after device reboot.
- **Extended AI Export**: Version 2 of JSON export including full weekly performance data.
- **Pre-Lock Checklist**: Guided UX to ensure all mandatory permissions are granted before starting a lock.
