# LiftLogic

LiftLogic is a native Android workout-tracking app built with Kotlin and Jetpack Compose. It lets a user create an account, log workouts (exercises, sets, reps, and weight), track body metrics over time, browse an exercise library pulled from a public API, and earn badges/XP for consistency.

This project builds to `app-debug.apk`, which is the installable app package.

## Features

- **Account system** — local registration and login with salted, hashed passwords (no server required).
- **Workout logging** — start an active workout session, log sets (exercise, weight, reps, set type), and see a summary when you finish.
- **Exercise library** — search and browse exercises pulled live from the [wger.de](https://wger.de) public exercise database API.
- **Progress tracking** — log body weight over time and view progress.
- **Gamification** — XP and streak tracking, with unlockable badges.
- **Profile & settings** — edit profile, change email/username/password, switch units (kg/lb), toggle notifications, and choose a light/dark theme.
- **Localization** — English, Afrikaans, and Zulu string resources included.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Navigation | Jetpack Navigation Compose |
| Local storage | Room (SQLite) |
| Networking | Retrofit + Gson + OkHttp (with logging interceptor) |
| Architecture | ViewModel + Repository pattern |
| Min SDK / Target SDK | 26 / 34 |

## Project Structure

```
app/src/main/java/com/example/liftlogic/
├── data/            # Room entities, DAOs, database, auth & workout repositories
├── network/         # Retrofit service + models for the wger.de exercise API
├── ui/
│   ├── screens/      # Compose screens (Home, Login, Register, Active Workout, Library, Progress, Profile, Settings...)
│   ├── theme/        # Color, typography, and theme definitions
│   ├── AppViewModel.kt        # Shared app-level view model
│   └── LiftLogicNavGraph.kt   # Navigation graph / routes
├── util/            # Utilities (e.g. password hashing)
└── MainActivity.kt  # App entry point
```

## Data Model

Room database (`liftlogic.db`) with the following tables:

- **users** — account info, hashed password, units preference, XP, streak
- **workouts** — one row per completed workout session
- **exercise_sets** — individual sets belonging to a workout
- **body_metrics** — body weight logged over time
- **badges** — unlocked achievement badges

## Networking

The app calls the public [wger.de](https://wger.de) REST API to fetch and search exercises (`api/v2/exercise/` and `api/v2/exercise/search/`). An internet connection is required for the exercise library; workout logging and progress tracking work offline via the local Room database.

## Purpose & Design Considerations

LiftLogic was built to give a gym-goer a single, offline-first place to plan and log workouts without needing a backend server or account on a third-party service. Key design decisions:

- **Jetpack Compose over XML layouts** — chosen for a more modern, declarative UI approach and faster iteration on screen design.
- **Room (local SQLite) over a remote database** — the app works fully offline for logging workouts and tracking progress, since a gym is not always a reliable place to have signal; only the exercise library needs network access.
- **Repository pattern (AuthRepository, Workout repositories)** — separates data access from UI/ViewModel logic, making the code easier to test and reason about.
- **Local, hashed authentication** — since there's no backend, passwords are salted and hashed on-device rather than stored in plain text, to still follow secure practice even without a server.
- **wger.de public API for the exercise library** — rather than manually building an exercise database, the app pulls from an existing, actively maintained open exercise database.

## GitHub & GitHub Actions

The project is version-controlled on GitHub, with the full Kotlin source pushed as plain files (no zip archives), and commit history reflecting incremental development (initial Gradle scaffold → feature commits → refinements).

A GitHub Actions workflow (`.github/workflows/android-build.yml`) automatically builds the app on every push and pull request to `main`:

1. Checks out the repository.
2. Sets up JDK 21 (Temurin distribution).
3. Grants execute permission to the Gradle wrapper.
4. Runs `./gradlew assembleDebug` to build the debug APK.
5. Uploads the resulting `app-debug.apk` as a downloadable build artifact.

This means every commit to `main` is automatically verified to still build successfully, and a fresh debug APK is always available from the Actions tab without needing to build locally.

## Video Presentation

A walkthrough of the app's features, with narration: **https://youtu.be/7aytCMmxjxA**

## Permissions

- `INTERNET` — required to fetch exercises from the wger.de API
- `ACCESS_NETWORK_STATE` — used to check connectivity

## Getting Started (Building from Source)

1. Open the `LiftLogic/` folder in Android Studio (Giraffe or newer recommended).
2. Let Gradle sync and download dependencies.
3. Run the app on an emulator or physical device (min Android 8.0 / API 26).

## Installing the Prebuilt APK

This repo is paired with a prebuilt debug package, `app-debug.apk`. To install it on an Android phone:

1. Unzip it if it was sent as `app-debug-apk.zip`.
2. Transfer `app-debug.apk` to your phone (email, cloud drive, USB, or messaging app).
3. On the phone, enable **Install unknown apps** for the app you'll use to open the file (Settings → Apps → Special app access → Install unknown apps).
4. Tap the APK file and confirm the install prompt.
5. Open **LiftLogic** from your app drawer.

> This is a **debug build**, not a Play Store release — expect debug logging and no code obfuscation/minification (`isMinifyEnabled = false`).

## Repository

Full source code: https://github.com/OmphileMoale/LiftLogic

## Notes

- Passwords are hashed and salted locally; there is no backend auth server — all accounts live only on the device's local database.
- App ID: `com.example.liftlogic`, version `1.0` (`versionCode 1`).
