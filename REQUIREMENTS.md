# Pomodoro Floating Bubble App - Requirements

## 1. Core State Machine & Cycle Logic
*   **Durations:** Focus: 25m | Short Rest: 5m | Long Rest: 15m (triggers every 4 Focus sessions).
*   **States:** `idle`, `focus`, `wait_short_rest`, `short_rest`, `wait_long_rest`, `long_rest`, `wait_focus`.
*   **Manual Transition Guards:** When a timer hits 00:00, the app does *not* automatically start the next phase. It enters a paused `wait_*` state, emits a notification/sound, and waits for user interaction to begin the next phase.
*   **Architecture:** The state machine (Kotlin Flow) must be strictly decoupled from the UI layer.

## 2. Floating UI & Visual Behavior
*   **Collapsible Layout (Jetpack Compose):**
    *   **Collapsed View:** A small circular bubble displaying a clock icon or just the remaining time and a circular progress stroke.
    *   **Expanded View:** A horizontal pill layout containing: `[<] (Rewind)`, `MM:SS (Play/Pause)`, and `[>] (Skip)`.
    *   **Auto-Collapse:** 60-second inactivity timer automatically collapses the expanded view without interrupting the countdown.
*   **Touch Interactions:**
    *   **Tap (Collapsed):** Expands the widget.
    *   **Tap (Expanded - Center):** Play/Pause toggle. Paused state visually indicates it (e.g., strikethrough or dimmed).
    *   **Long Press (or Double Tap):** Full Reset (stops timers, resets state to `idle`, collapses widget).
    *   **Drag:** Repositions the floating overlay on the screen.
*   **Color Palette:**
    *   Focus: Deep Crimson Red (`#c62828`).
    *   Short Rest: Sapphire Blue (`#1565c0`).
    *   Long Rest: Emerald Green (`#2e7d32`).
*   **Cycle Progress:** A visual indicator (like a segmented circular stroke) showing how many Focus sessions are complete (1/4, 2/4, etc.).

## 3. External Integrations (Android Replacements)
*   **Time Tracking:** Replace Watson with a local SQLite (Room DB) session logger to track completed focus blocks.
*   **Alerts:** Use Android `NotificationManager` for desktop-like alerts and `MediaPlayer` / `SoundPool` to play a `bell.wav` when a stage requires user attention.

## 4. Technical Stack & Android Constraints
*   **Language & UI:** Kotlin, Jetpack Compose. Terminal-driven build (Gradle CLI), no Android Studio required.
*   **Overlay API:** `WindowManager` with `LayoutParams.TYPE_APPLICATION_OVERLAY`.
*   **Compose in Service:** You MUST manually inject `ViewTreeLifecycleOwner`, `SavedStateRegistryOwner`, and `ViewTreeViewModelStoreOwner` into the WindowManager view (or use `JetOverlay` library) to prevent Compose crashes outside an Activity.
*   **Foreground Service (Android 14+):** The background service MUST be a Foreground Service.
    *   In `AndroidManifest.xml`, declare `android:foregroundServiceType="specialUse"`.
    *   In `startForeground()`, pass `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`.
    *   Include a user-visible persistent notification for the Foreground Service showing the current timer.
*   **Permissions:** Gracefully request `SYSTEM_ALERT_WINDOW` and `POST_NOTIFICATIONS`.

## 5. Execution Steps for Agent
1. Scaffold a terminal-driven Android Gradle project (Kotlin + Compose).
2. Configure `AndroidManifest.xml` (Permissions, Foreground Service with `specialUse`).
3. Build the decoupled Pomodoro State Machine using Kotlin Flow.
4. Implement the Jetpack Compose UI (Collapsed/Expanded states, Auto-collapse timer, Colors).
5. Build the Foreground Service, integrating `NotificationManager`, `MediaPlayer`, and the Compose-to-WindowManager lifecycle bridge.
