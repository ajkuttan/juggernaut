# Calorie Tracker Android App

This repository now includes a native Android calorie tracker app built with Kotlin + Jetpack Compose.

## Features
- Set and save a daily calorie goal.
- Add meals with calories, protein, carbs, and fat.
- View daily totals and progress.
- Delete individual meals or clear all meals.
- Data persistence via `SharedPreferences`.

## Open in Android Studio
1. Open Android Studio.
2. Choose **Open** and select this repository root.
3. Let Gradle sync.
4. Run the `app` configuration on an emulator or device.

## Project structure
- `app/src/main/java/com/example/calorietracker/MainActivity.kt` — App UI + logic.
- `app/src/main/AndroidManifest.xml` — Android manifest.
- `app/build.gradle.kts` — App build config.
- Root `build.gradle.kts` + `settings.gradle.kts` — Gradle project setup.
