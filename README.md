# 💰 SmartSpend – Personal Budget Tracker

> **OPSC6311 – POE Part 3 | CodeVision Team**
> Rosebank College (The Independent Institute of Education) | 2026

---

## 📱 App Overview

SmartSpend is an Android budget tracking application built in **Kotlin** that helps users take control of their personal finances. Users can log daily expenses, set spending goals, track their category-level budget performance, and stay motivated through a built-in gamification system featuring XP, levels, streaks, and badges.

The app runs on a real Android device and uses a local **Room (SQLite)** database to persist all user data — no internet connection required.

---

## 🎯 Purpose

The goal of SmartSpend is to make personal budgeting accessible, visual, and rewarding. Many people struggle to stay within a budget because tracking feels tedious. SmartSpend solves this by:

- Making expense logging quick and frictionless
- Giving users instant visual feedback on their spending vs goals
- Rewarding consistent financial behaviour through gamification

**Target user:** Any Android user who wants to manage their day-to-day spending in South African Rands (R).

---

## ✨ Features

### Core Features (POE Requirements)

#### 📊 Spending Graph with Min & Max Goals
Users can view a **bar chart** showing the amount spent per category over a user-selectable period. The graph overlays the user's **minimum and maximum budget goals** so they can instantly see where they stand visually.

#### 📈 Visual Budget Performance
The home screen displays a real-time visual indicator showing whether the user is:
- ✅ Within their budget
- ⚠️ Below their minimum goal (may be under-spending)
- ❌ Over their maximum budget

The app uses clear colour coding and descriptive text to make this immediately understandable.

#### 🏆 Gamification System
SmartSpend includes a full gamification engine to reward good financial habits:

| Element | Description |
|---|---|
| **XP (Experience Points)** | Earned for logging expenses (+10 XP), setting budget goals (+10 XP), adding categories (+5 XP) |
| **Levels** | 5 levels (Level 1–5), with an XP progress bar shown on the home screen |
| **Streaks** | Daily logging streak tracked and displayed — 3-day and 7-day streak milestones |
| **Weekly Challenge** | "Log 3 expenses this week" with live progress tracking |
| **Badges** | 8 unlockable badges: Starter, Saver, Budget Master, Expense Champion, Streak Hero, Week Warrior, Goal Setter, Insightful |

### Custom Features (Own Features)

#### 🧾 Custom Feature 1: Receipt Photo Attachment
When logging an expense, the user can **attach a photo of their receipt** directly from their phone's gallery. The photo is saved alongside the expense record and displayed in the expense card view. This gives users a visual record and makes it easier to verify purchases later.

> **Where to find it:** Tap "Add Expense" → tap "Attach Receipt Photo" to pick an image from the gallery.

#### 🔔 Custom Feature 2: Bill Reminders
Users can add **upcoming bill reminders** (e.g. rent, subscriptions, data) with a name, amount, and due date. The home screen shows a list of upcoming bills so the user can plan their budget in advance and avoid unexpected shortfalls.

> **Where to find it:** On the home screen, scroll to the "Bill Reminders" section → tap "Add Reminder".

---

## 🗂️ App Screens

| Screen | Description |
|---|---|
| **Welcome / Login** | User registration and login with User ID and password |
| **Home (MainActivity)** | Dashboard with XP bar, streak, budget status, spending graph, badges, bill reminders, smart insights |
| **Add Expense Dialog** | Log expense with amount, category, date, description, and optional receipt photo |
| **Spending Graph Dialog** | Bar chart per category with min/max goal lines, selectable period |
| **Badges & Rewards Dialog** | View all earned and locked badges |
| **Smart Insights Dialog** | Auto-generated tips based on spending patterns |
| **Budget Goal Dialog** | Set minimum and maximum spending goals |
| **Manage Categories** | Add or view custom expense categories |
| **Bill Reminders** | Add and view upcoming recurring bills |

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Kotlin** | Primary programming language |
| **Android SDK** | App framework |
| **Room (SQLite)** | Local database for expenses, users, badges, reminders |
| **Android Gradle Plugin 8.7.3** | Build system |
| **Kotlin Coroutines** | Async database operations |
| **Custom Canvas Drawing** | Bar chart rendered with Android Canvas API (no third-party chart library) |
| **SharedPreferences** | Lightweight persistence for XP, levels, streaks |

---

## 🗄️ Data Classes

| Class | Fields |
|---|---|
| `ExpenseRecord` | amount, category, date, description, imageUri |
| `UserProfile` | email, password (hashed), walletBalance |
| `BillReminder` | name, amount, dueDate |
| `Badge` | name, icon, earned (boolean) |

---

## 🎨 Design Considerations

- **Colour palette:** Blue-dominant (`#0074D9`, `#005BB5`) with gold accents (`#FFD700`) for gamification elements, and green/red for budget status indicators
- **Single Activity architecture:** The entire app runs within `MainActivity` using dialogs and dynamic views — keeping the navigation lightweight
- **Expense categories:** Food, Shopping, Entertainment, School, Other (user can add custom categories)
- **South African context:** All amounts displayed in Rands (R)
- **Accessibility:** Clear labels, readable font sizes, colour + text combined for status (not colour alone)

---

## 🔁 Version Control – GitHub

This repository was initialised with a README and all source code was committed and pushed progressively as features were built. The commit history reflects the development journey from initial setup through to the final feature additions.

**Branching strategy:** Development was done on the `main` branch with commits made at meaningful milestones (login system, expense logging, gamification, graph feature, bill reminders, receipt photo).

---

## ⚙️ GitHub Actions – Automated Build & Test

This project uses **GitHub Actions** to automatically build and test the app on every push to the `main` branch.

The workflow (`.github/workflows/build.yml`) performs the following steps:
1. Checks out the source code
2. Sets up JDK 17
3. Grants execute permission to the Gradle wrapper
4. Runs unit tests via `./gradlew test`
5. Builds the debug APK via `./gradlew assembleDebug`

This ensures the project builds correctly in a clean environment — not just on a local machine — and that core logic passes automated tests.

See: [`.github/workflows/build.yml`](.github/workflows/build.yml)

---

## 🎬 Demo Video

Watch the full demonstration of SmartSpend running on a real Android device:

▶️ **[SmartSpend Demo Video – YouTube](YOUR_YOUTUBE_LINK_HERE)**

> The video demonstrates: login & registration, adding expenses with receipt photos, setting budget goals, viewing the spending graph, earning XP and badges, adding bill reminders, and viewing smart insights.

---

## 🚀 How to Run the Project

1. Clone this repository
2. Open in **Android Studio**
3. Let Gradle sync
4. Connect a physical Android device (API 24+) or use an emulator
5. Click **Run ▶️**

> The app was built and tested on a real Android phone as required by the POE brief.

---

## 👥 Team

**CodeVision**
- Omphile 
- 

**Module:** OPSC6311 – Open Source Coding  
**Institution:** Rosebank College (The Independent Institute of Education)  
**Submission:** POE Part 3 – Final App Development | 2026
