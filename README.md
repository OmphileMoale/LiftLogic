# LiftLogic — gym workout tracker (Kotlin + XML)

LiftLogic is a native Android gym app. **The UI is built only with XML layouts and Kotlin**
(Fragments + ViewBinding). Jetpack Compose is not used anywhere.

## Download the app (debug APK)

**[Download the latest debug APK: app-debug-LiftLogicGym.apk](https://github.com/OmphileMoale/LiftLogic/releases/latest)**
(open the link, then tap `app-debug-LiftLogicGym.apk` under **Assets**)

To install it on an Android phone (Android 8.0 / API 26 or higher):

1. Download the APK on the phone (or copy it over from a computer).
2. Open the file. If Android asks, allow **Install unknown apps** for your browser or file manager.
3. Tap **Install**, then open **LiftLogic**.

Every push to `main` is also built by GitHub Actions. The newest build can be downloaded from the **Actions** tab
(open the latest run, then **Artifacts**).

## Features

| Area | What it does |
|------|--------------|
| Accounts | Register, log in, stay signed in (SessionManager), salted SHA-256 password hashing, change password |
| Onboarding | Goal, experience, height, body weight and weekly workout target after sign-up (also "Edit profile") |
| Exercise library | ~55 built-in exercises with instructions, search, muscle-group filters, custom exercises |
| Online exercises | Browse the wger.de REST API (Retrofit) by category and import exercises into your library |
| Routines | 6 built-in programs (Full Body A/B, Push, Pull, Legs, Upper Strength); create, edit, reorder (drag), copy, delete your own |
| Workout logger | Several exercises per workout, weight / reps / ✓ per set, "previous" column from last time (tap to copy), add / swipe-delete sets, rename, discard. In-progress workout is saved in Room, so it survives the app closing |
| Rest timer | Starts when a set is ticked, ±15 s / skip, vibration + notification when rest is over (keeps running when you leave the screen) |
| Finishing | Removes unticked sets, detects personal records (estimated 1RM), awards XP and badges, summary screen, share, save as routine |
| History | All workouts, workout detail, repeat a workout, delete |
| Progress | Totals, weekly-volume bar chart, body-weight chart + log, personal records list, per-exercise 1RM chart and history |
| Gamification | XP and levels, day streak, weekly goal progress, 8 badges |
| Tools | One-rep-max calculator with % table, barbell plate calculator |
| Settings | kg / lb everywhere, default rest time, rest alerts, daily reminder (AlarmManager, re-scheduled after reboot), dark mode, accent colour, language (English / Afrikaans / isiZulu), profile picture |

## Architecture

```
MainActivity (single activity, BottomNavigationView, NavHostFragment)
 └─ res/navigation/nav_graph.xml  → every screen is a Fragment with an XML layout
ui/        Fragments, RecyclerView adapters, ViewModels, custom Canvas charts (LineChartView, BarChartView)
data/      Repositories (User, Exercise, Routine, Workout, BodyWeight), SessionManager, seed data
data/db/   Room entities, DAOs, AppDatabase (KSP)
network/   Retrofit service + DTOs for wger.de
notifications/  NotificationHelper, RestTimer, ReminderScheduler/Receiver, BootReceiver
util/      Units (kg/lb), OneRepMax, PlateCalculator, Stats, Formatters, AppPrefs, PasswordHasher
AppContainer  simple manual dependency injection (one instance per app)
```

* **MVVM**: screens observe `StateFlow`s from ViewModels/repositories with `repeatOnLifecycle`.
* **Room** is the single source of truth (weights stored in kg, converted for display).
* Edits typed during a workout go through a write queue so values are always saved in order.

## Build

Open in Android Studio, let Gradle sync, then Run. Unit tests: `./gradlew test`.
Room uses **KSP** (kapt crashes with Kotlin 2.0: "BuiltinsVirtualFileProvider duplicated").
The database is version 2; older prototype data is cleared on first launch.

## Testing and continuous integration

* Unit tests (JUnit) cover the training logic. Run them with `./gradlew test`.
* GitHub Actions (`android-build.yml`) builds the debug APK on every push and uploads it as an artifact.

## Release notes

### Version 2.0 (final PoE, Part 3)

Updates since the Part 1 prototype and the Part 2 submission:

* **Rebuilt with Kotlin and XML layouts.** Every screen is a Fragment with an XML layout, using ViewBinding and the
  Navigation component. Jetpack Compose has been removed.
* **Multi-language support:** English, Afrikaans and isiZulu, switchable in Settings.
* **Gamification (innovative feature):** XP, levels, day streak, weekly goal progress and 8 achievement badges,
  awarded when a workout is finished.
* **Rest timer (innovative feature):** starts when a set is ticked, with vibration and a notification when rest is over.
* **Share workout summary (innovative feature):** share the result of a finished workout, or save it as a routine.
* **Progress tracking:** personal records from estimated 1RM, weekly-volume chart, body-weight chart and per-exercise charts.
* **Exercise library:** about 55 built-in exercises, plus browsing and importing from the wger.de REST API.
* **Routines:** 6 built-in programs, plus your own routines (create, edit, reorder, copy, delete).
* **Offline-first logging:** an in-progress workout is saved in Room and survives the app being closed.
* **Tools and settings:** 1RM calculator, plate calculator, kg / lb, dark mode, accent colour, daily reminder.
