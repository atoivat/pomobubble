# PomoBubble 🍅🫧

A minimalist, floating Pomodoro timer and focus analytics app for Android, designed with a clean aesthetic and real-time state synchronization.

---

## ✨ Features

- **Floating Overlay Bubble & Pill View**: 
  - Toggle between a compact circle bubble (with progress stroke) and an expanded pill view.
  - Single tap toggles Play/Pause; click & hold switches between bubble and pill view without resetting the timer.
- **Real-Time Synchronization**: 
  - Shared state machine keeps the floating overlay bubble and the main dashboard UI in perfect sync.
- **Dynamic Phase Aesthetics**:
  - Background colors animate based on the active timer phase:
    - 🔴 **Focus**: `#C62828`
    - 🔵 **Short Rest**: `#1565C0`
    - 🟢 **Long Rest**: `#2E7D32`
- **Smart Focus Logging**:
  - Sessions are logged to a Room database only upon completion (25 mins) or interruption (logging exact spanned focus time).
- **Minimalist Dashboard & Analytics**:
  - Hero timer display with phase navigation (`<` and `>`) and Play/Pause.
  - Today's summary card (total focus hours/minutes & completed sessions).
  - Log of recent sessions.
  - **GitHub-style Contribution Heatmap Calendar**: 12-week contribution grid scaling white opacity based on daily focus time.
- **Custom Adaptive Icon**: Programmatically rendered clock timer icon.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Database**: Room Persistence Library
- **Concurrency & State**: Kotlin Coroutines & `StateFlow`
- **Architecture**: Clean Architecture / State Machine with TDD (Test-Driven Development)
- **Overlay**: Android WindowManager & System Alert Window Service

---

## 🤖 AI Development Attribution

This project was built and refined with AI pair programming assistance powered by **Gemini 3.6 Flash** via the **Antigravity CLI** (`agy`).

---

## 🚀 Building & Running

### Requirements
- Android SDK (API Level 26+)
- JDK 21
- Gradle

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Run Unit Tests
```bash
./gradlew test
```

### Install via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 License

MIT License.
