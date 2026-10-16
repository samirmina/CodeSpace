— Project Rules & Engineering Standards

## Core Principle: No Incomplete Circles

Every task must be closed end-to-end before moving on. A feature is not "done" when code is written — it is done when it is fully integrated, non-breaking, and verified in the JavaFX UI flow. Stubs, TODO comments, dead wiring, layout overlaps, and corrupted persistent JSON states are unacceptable as final states.

---

## Rule 0: Read First & Non-Destructive Edits

* **Read Before Modifying:** Before modifying any file, inspect its place in the architecture (`View` $\rightarrow$ `ViewCoordinator` $\rightarrow$ `Service` $\rightarrow$ `JsonStorage`).
* **Preserve Non-Defective Scope:** A fix in one view or service must not destroy unrelated functionality. When updating shared components, preserve all keys, handlers, and logic not related to the defect.
* **No Blanket Overwrites:** Deleting, overwriting, or replacing an entire file to "simplify" a fix is prohibited if valid, unrelated logic or drawing methods exist within it.

---

## Rule 1: Architecture Hierarchy & Dependency Flow

The system strictly follows a 4-layer unidirectional dependency flow:

$$\text{UI Layer } (src/jfx/) \longrightarrow \text{Service Layer } (src/services/) \longrightarrow \text{Model Layer } (src/models/) \longrightarrow \text{Persistence } (src/utils/ / JsonStorage.java)$$

* **No Layer Leaks:** Models must contain data entities and enums only—no JavaFX controls or service logic. Services must handle business logic and CRUD—no direct UI manipulation.
* **Central Navigation & Shell:** `MainView` acts as the circular shell and tab coordinator. Tab views (`PomodoroView`, `TaskView`, `UserView`, `SessionsView`, `ReportView`) manage tab-specific controls.
* **Swipe Stack Integrity:** Every swipeable page container must use `SwipeContainer`. When modifying page views, ensure `last-page` restoration and swipe gesture transitions function properly without breaking navigation.

---

## Rule 2: Single Source of Truth for UI Sizing & Layout

* **Mandatory ViewCoordinator Usage:** Every user-facing text label, card, and shape across all UI views **must** be sized, formatted, and centered exclusively through `ViewCoordinator`.
* **Zero Hardcoded Font Sizes & Dimensions:** Hardcoded font sizes, fixed element pixel coordinates outside `ViewCoordinator` bounds ($W=660, R=330, MARGIN=132$), or manual string truncations are strictly forbidden.
* **Anti-Congestion Rule:** Always route text rendering through `ViewCoordinator`'s anti-congestion fitters (`fitText`, `fitTextWrap`, `fitLabel`, `fitStatValue`, `fitDetailTitle`, `fitDetail`, `applyFont`). Every label must render cleanly without clipping or trailing ellipses under all standard dataset lengths.

---

## Rule 3: Single Source of Truth for Data & Business Logic

* **Unified Computation:** Service methods in `TaskService`, `UserService`, `PomodoroService`, and `ReportService` are the **sole** authoritative source for calculations (e.g., focus times, overdue counts, most-productive tasks). Never duplicate business or analytics calculations inside JavaFX UI views or Canvas renderers.
* **Central Configuration & Styling:** Visual styling (colors, CSS classes, button variants, card styles) belongs in `src/style.css`. Component dimensions and dynamic sizing logic belong in `ViewCoordinator.java`. Magic styling constants in view code are forbidden.

---

## Rule 4: Root-Cause Verification & Regression Prevention

When a UI glitch, state synchronization error, or runtime exception occurs, perform a full root-cause trace back through the pipeline before making edits:

$$\text{CSS / ViewCoordinator} \longrightarrow \text{View Layout} \longrightarrow \text{SwipeContainer} \longrightarrow \text{Service Logic} \longrightarrow \text{JsonStorage}$$

Report findings in this standard format:

* **What it does:** [Current bug or UI defect]
* **What it should do:** [Expected behavior]
* **Root cause:** [Causal chain leading to the underlying fault]
* **Fix:** [Code change eliminating the root cause]
* **Files involved:** [Affected file paths]

Never apply a surface patch to a `View` file if the underlying root cause is in `ViewCoordinator` font-scaling or `JsonStorage` deserialization.

---

## Rule 5: Persistence & State Safety (`JsonStorage`)

* **ID Generator Integrity:** Any update to `JsonStorage` or model entities must ensure atomic ID generation updates correctly upon JSON loading.
* **Atomic Save-on-Exit:** Operations modifying `Task`, `User`, `Pomodoro`, `Category`, or `Tag` entities must sync gracefully with `JsonStorage`. Ensure application shutdown hooks in `Main.java` safely save state without throwing file lock or parsing exceptions.
* **Corrupt File Resilience:** `JsonStorage` must handle empty, missing, or malformed JSON files gracefully without crashing the UI thread on application startup.

---

## Rule 6: JavaFX UI Event Thread Discipline

* **Thread Safety:** Long-running operations, timer counts (`AnimationTimer`, `Timeline`), or disk I/O must never block the JavaFX Application Thread.
* **UI Lifecycle Discipline:** Avoid tearing down and re-instantiating heavy view containers dynamically inside tick loops or frequent event listeners. Update existing node properties, visual nodes, or canvas layers in place.
* **Overlay & Stage Management:** Always-on-top notification Stages and completion overlays must properly handle focus, transparency, and disposal without leaving dangling background threads or unhandled JavaFX Stage instances.

---

## Rule 7: Unit Testing & Verification Protocol

* **Model & Logic Verification:** Run existing unit tests (`test/models/*Test.java` and `test/services/*Test.java`) after modifying business rules, models, or storage handling.
* **Runtime Smoke Testing:** Code passing compilation is necessary but **insufficient**. Before declaring success, launch the application via `Main.java` and perform a runtime smoke test verifying:
1. Circular shell bounds, edge hover buttons, and tab switching render cleanly.
2. Text and stats dynamically scale without truncation via `ViewCoordinator`.
3. CRUD actions update view state and persist to JSON storage cleanly.