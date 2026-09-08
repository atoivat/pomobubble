# GEMINI.md - Development Guidelines for Agentic AI

## Project: PomoBubble (Android Floating Pomodoro Overlay)

### 1. Architectural Principles
* **State Machine Isolation:** Business logic (`PomodoroStateMachine`) must be pure Kotlin using `StateFlow`/`SharedFlow`, completely free of Android framework classes (`View`, `Context`, `Service`).
* **Time Calculation Precision:** Do not rely on naive 1-second decrement loops (`remainingSeconds--`). Compute remaining time using target timestamp diffs via `SystemClock.elapsedRealtime()` to avoid Doze mode and background thread drift.
* **UI & Service Decoupling:** Jetpack Compose composables must strictly receive immutable state and emit UI events. Never leak `Service` or `WindowManager` instances into composables.

### 2. Android Lifecycle & System Constraints
* **Compose in WindowManager:** Always attach `ViewTreeLifecycleOwner`, `SavedStateRegistryOwner`, and `ViewTreeViewModelStoreOwner` to the `ComposeView` before calling `windowManager.addView()`. Explicitly call `dispose()` on service shutdown to prevent memory leaks.
* **Touch & WindowManager Flags:** Use `FLAG_NOT_FOCUSABLE` and `FLAG_NOT_TOUCH_MODAL` dynamically to ensure touch gestures (drag/tap) work smoothly without blocking underlying system interaction when collapsed.
* **Permissions & Android 14+ FGS:** Handle `SYSTEM_ALERT_WINDOW` routing via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. Android 14+ Foreground Service must declare `specialUse` type and supply appropriate manifest property tags.

### 3. Execution & Verification Rules
* **Incremental Implementation:** Build components step-by-step (1. Pure State Machine + Unit Tests -> 2. Project Scaffolding & Permissions -> 3. WindowManager Foreground Service Bridge -> 4. Compose UI & Animations).
* **Terminal Verification:** Execute `./gradlew test` and `./gradlew assembleDebug` to verify compilation and test suites after every structural change. Never mark a step complete without passing CLI builds.
* **Zero AI Overuse:** Prefer modular, concise components over massive monolithic files. Do not swallow exceptions or return fake mock fallbacks to pass builds.

### 4. Mentorship & Step-by-Step Learning
* **Educational Approach:** Explain key Android concepts, build setup decisions, and architectural choices step-by-step as code is written.
* **No Monolithic Dumps:** Introduce files and modules incrementally so the user understands the exact role of each configuration file and component.

