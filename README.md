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

## Notes

- Passwords are hashed and salted locally; there is no backend auth server — all accounts live only on the device's local database.
- App ID: `com.example.liftlogic`, version `1.0` (`versionCode 1`).
