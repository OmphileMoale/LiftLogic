# LiftLogic — gym workout tracker (Kotlin + XML)

LiftLogic is a native Android gym app. **The UI is built only with XML layouts and Kotlin**
(Fragments + ViewBinding). Jetpack Compose is not used anywhere.

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
