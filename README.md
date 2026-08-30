# Fitu - AI Fitness Coach

**Your personal AI-powered fitness coach in your pocket.**

Fitu is an Android app that combines real-time pose-detection exercise coaching,
AI nutrition analysis, automatic step tracking and workout planning - all built
with modern Android development practices.

![Build](https://github.com/FakeAbid11/Fitu/actions/workflows/fitu-apk.yml/badge.svg)

## Features

- **AI Coach** - Real-time rep counting and form feedback using the camera and
  ML Kit pose detection (push-ups, squats, dumbbell curls, crunches, plank hold
  time + form scoring). Anti-phantom-rep engine: angle smoothing, frame
  debouncing, hysteresis and per-exercise minimum rep intervals.
- **Nutrition** - Snap a photo of your food and Gemini AI estimates calories and
  macros. Text search, portion control, review-before-save flow and local
  caching of previous queries.
- **Steps** - Foreground step-counter service using the hardware step sensor,
  with an accelerometer software fallback, auto-restart after boot, milestone
  notifications and a home-screen widget.
- **Workout Generator** - AI-generated workout plans saved on device.
- **Dashboard** - Daily progress rings, weekly charts, streaks, goal
  celebrations and skeleton loading states.
- **Backup & Restore** - Export/restore all data (steps, meals, workouts,
  profile) as JSON. Dark theme, metric/imperial units, haptics and more.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Persistence | Room (schema-exported), DataStore, EncryptedSharedPreferences |
| AI | Google Gemini (generativeai SDK) |
| Vision | ML Kit Pose Detection (accurate) + CameraX |
| Architecture | MVVM, StateFlow/Coroutines, layered single-module structure |
| CI/CD | GitHub Actions (signed APK/AAB releases in the cloud) |

## Getting Started

### 1. Gemini API Key

The AI features require a free Google AI (Gemini) API key:

1. Get a key at https://aistudio.google.com/apikey
2. Enter it during in-app onboarding - it is stored encrypted on device
   (EncryptedSharedPreferences, AES-256).

### 2. Build & Run

Open the project in Android Studio and press Run, or from the command line:

```bash
./gradlew assembleDebug
```

- Min SDK: 26 (Android 8.0) - Target SDK: 34 (Android 14)

### 3. Run Tests

```bash
./gradlew testDebugUnitTest
```

Unit tests cover the rep-counting state machine, plank tracking and angle math.
Tests run inside the Fitu Build workflow before every release build.

## Building Releases in the Cloud (no high-end PC needed)

This project is configured to build fully on GitHub Actions:

1. Go to **Actions -> Fitu Build -> Run workflow**
2. (Optional) Set `app_version` (e.g. `2.5.0`) and `version_code` - or leave empty
3. Tests run first, then a signed APK is built and attached to a GitHub Release

Required repository secrets (Settings -> Secrets and variables -> Actions):

| Secret | Purpose |
|---|---|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing key alias (e.g. `fitu`) |
| `KEY_PASSWORD` | Signing key password |

The keystore file itself is **never** committed to git (see `.gitignore`).

## Project Structure

```
app/src/main/java/com/fitu/
  aicoach/     # Pose detection, rep counting, plank tracking (pure logic + unit tested)
  data/        # Room database, repositories, step service, boot receiver
  domain/      # Repository interfaces
  di/          # Hilt modules (incl. Gemini client with retry + fallback)
  navigation/  # Compose navigation graph
  ui/          # Screens, components, theme, ViewModels
  util/        # Helpers (units, haptics, battery optimization)
  widget/      # Home-screen steps widget
```

## Known Limitations / Roadmap

- Database v9 has no migration path from earlier versions yet - add migrations
  in `DatabaseMigrations.kt` before shipping the first public update.
- Migration from kapt to KSP (faster builds) is planned.
- Each user supplies their own Gemini API key; a backend proxy would be needed
  for public distribution.