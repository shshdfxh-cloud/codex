# TimeTracker Android Prototype

This workspace now contains a native Android prototype built with Kotlin and Jetpack Compose.

## Implemented

- Full-screen timer landing page with a large circular timer
- Tap to start, tap again to stop
- After stopping, choose a category and enter a note before saving
- Local persistence with SharedPreferences JSON storage for categories, records, running timer state, and unsaved draft
- Bottom navigation with `Timer`, `Overview`, and `Settings`
- Daily overview ring for 24 hours with gray gaps for untracked time
- Color-coded time block list
- Swipe left on a time block to reveal `Edit` and `Delete`
- Expand a time block to see the recorded note
- Edit dialog for category, start time, duration, end time, and note
- Category settings for add, edit, delete, and color adjustment

## Files

- `app/src/main/java/com/example/timetracker/MainActivity.kt`
- `app/src/main/java/com/example/timetracker/Models.kt`
- `app/src/main/java/com/example/timetracker/TimeTrackerApp.kt`
- `app/src/main/java/com/example/timetracker/OverviewComponents.kt`
- `app/src/main/java/com/example/timetracker/TimeUtils.kt`

## Notes

- A local build toolchain was installed under `_local_tools` for this workspace.
- The project now includes Gradle wrapper files: `gradlew`, `gradlew.bat`, and `gradle/wrapper/*`.
- A debug APK has been built successfully at `app/build/outputs/apk/debug/app-debug.apk`.

## Beginner Run Steps

1. Install Android Studio if you do not have it yet.
2. Open Android Studio and choose `Open`.
3. Select this folder: `C:\Users\xbw\Desktop\Codex`.
4. Wait for Gradle sync. If Android Studio asks to install Android SDK components, click `Install` or `OK`.
5. Create an emulator from `Device Manager` if you do not have one already.
6. Click the green Run button to install and start the app.
7. If you want an APK file, use `Build > Build APK(s)`.

## Command Line Build

- Rebuild debug APK: `gradlew.bat assembleDebug`
- Output APK: `app/build/outputs/apk/debug/app-debug.apk`
Test change from Codex
