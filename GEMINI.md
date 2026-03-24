# Brainback - Android Attention Firewall

Brainback is a specialized Android application designed to block short-form content (YouTube Shorts, Instagram Reels, and browser-based short-form URLs) at the OS level using Android's Accessibility Services. It acts as a surgical intervention to break dopamine-driven scrolling loops.

## Project Overview
- **Purpose**: A behavioral circuit breaker that intercepts specific UI signals and triggers system-level actions (like back navigation) to exit short-form content.
- **Target Platform**: Android (Min SDK 27, Target SDK 36)
- **Primary Technology**: Kotlin, Android Accessibility Service API.
- **Architecture**: 
    - `AccessibilityService` (`BlockerService.kt`): The core engine that monitors UI events, detects Short/Reel patterns, and triggers blocking actions.
    - `MainActivity.kt`: User interface for enabling/disabling the service and monitoring its status.
    - `accessibility_service_config.xml`: Configuration for the service, defining monitored packages and event types.

## Building and Running
The project uses the Gradle build system.

- **Build Project**: `./gradlew assembleDebug`
- **Install on Device**: `./gradlew installDebug`
- **Clean Project**: `./gradlew clean`
- **Run Tests**: `./gradlew test` (Unit tests) or `./gradlew connectedAndroidTest` (Instrumented tests)

## Development Conventions
- **Detection Logic**: Prefers View IDs over broad text matches to minimize false positives.
- **Stability First**: Detection must be heuristic and adaptable to server-driven UI changes in target apps (YouTube, Instagram, Browsers).
- **Tooling**: Uses `adb-inspector` (custom skill) for real-time UI hierarchy analysis to identify new blocking signals.

## Key Files
- `app/src/main/java/com/naman/brainback/BlockerService.kt`: Contains the primary detection and blocking logic.
- `app/src/main/res/xml/accessibility_service_config.xml`: Defines the list of apps being monitored by the firewall.
- `app/src/main/java/com/naman/brainback/MainActivity.kt`: Handles service status checks and activation.
- `app/src/main/res/layout/activity_main.xml`: The dashboard UI.

## Roadmap & Features (Planned)
- [ ] **Usage Analytics**: Track and display the number of blocked attempts per app.
- [ ] **Data Visualization**: Show pie charts/bar graphs for screen time and blocking events.
- [ ] **Digital Wellbeing Integration**: Fetch and display native Android screen time data.
- [ ] **Custom Overlays**: Design and implement "Blocked" overlays using Google Stitch.
