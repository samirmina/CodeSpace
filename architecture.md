# Chronos Architecture

Last scanned: 2026-09-24

## 1. System summary

Chronos is a Java 21 and JavaFX 21 desktop productivity application. The main program provides a 760x760 transparent, watch-style interface for Pomodoro timing, task management, user administration, reports, session history, and role-filtered settings.

The primary data path is local JSON persistence. A PostgreSQL database is used as an additional user-registry source. The repository also contains an independent `user-creator/` JavaFX/CLI utility for direct PostgreSQL user CRUD.

Java source is the intended source of truth. Compiled `.class` files under `src/` and `out/` are build artifacts and are not authoritative.

## 2. Runtime entry points

| Entry point | Location | Responsibility |
|---|---|---|
| Main desktop application | `src/Main.java:16` | Initialize services, merge JSON/PostgreSQL users, show login, and open the main shell |
| User Manager GUI | `user-creator/src/main/java/com/chronos/usercrud/UserCrudApp.java:19` | Direct PostgreSQL user CRUD |
| User Manager CLI | `user-creator/src/main/java/com/chronos/usercrud/UserCrudCLI.java:11` | Terminal-based PostgreSQL user CRUD |

`Main.main()` calls JavaFX `launch(args)` (`src/Main.java:16-19`). `Main.start()` creates the service layer, loads the user registry, attempts the PostgreSQL merge, migrates legacy data when applicable, and displays `LoginView` (`src/Main.java:31-78`). A `Timeline` polls the login view and opens `MainView` after selection (`src/Main.java:80-100`).

## 3. Logical architecture

```text
JavaFX bootstrap
  src/Main.java
       |
       +-- UserService -------- data_users.json
       |       |
       |       +-- PostgresUserDAO -- localhost PostgreSQL
       |
       +-- TaskService --------- data_<userId>.json
       +-- PomodoroService ----- data_<userId>.json
       +-- SettingsService ----- data_<userId>.json
       +-- JsonStorage --------- legacy data.json migration
       +-- ReportService -------- text/CSV export
       |
       +-- LoginView
       +-- MainView
              +-- PomodoroView
              +-- TaskView
              +-- UserView
              +-- ReportView
              +-- SessionsView
```

The application has no formal repository, service-interface, or dependency-injection layer. JavaFX views directly hold service references, services own in-memory state, and persistence is called from both services and views.

## 4. Source modules

| Module | Location | Responsibility |
|---|---|---|
| Bootstrap | `src/Main.java` | Composition, login/main/logout lifecycle, shutdown persistence |
| JavaFX views | `src/jfx/` | Screens, navigation, canvas rendering, dialogs, and animations |
| Domain models | `src/models/` | `User`, `Task`, `Pomodoro`, `AppSettings`, enums, `Category`, and `Tag` |
| Services | `src/services/` | CRUD, timing aggregates, reports, JSON persistence, and PostgreSQL access |
| Utilities | `src/utils/` | File I/O and audio playback/WAV generation |
| Tests | `test/` | JUnit tests for models and core services |
| User administration tool | `user-creator/` | Independent PostgreSQL user manager |

The main source tree contains 29 Java files. The test tree contains 11 Java files and 156 `@Test` methods.

## 5. UI architecture

### Main shell

`MainView` implements the circular watch shell (`src/jfx/MainView.java:14-180`). Five edge buttons select Timer, Tasks, Users, Reports, and Sessions (`src/jfx/MainView.java:26-28`). Chevron buttons and vertical swipe gestures switch tabs (`src/jfx/MainView.java:243-261`).

Guest sessions expose Timer, Users, and Reports (`src/jfx/MainView.java:243-248`). The outer window is draggable when mouse input occurs outside the inner content radius (`src/jfx/MainView.java:163-177`).

### Shared mechanics

`SwipeContainer` stacks overview/content pages and animates horizontal transitions (`src/jfx/SwipeContainer.java:20-97`). `ViewCoordinator` centralizes the 660x660 content coordinate system, font sizing, text fitting, stat cards, and canvas helpers (`src/jfx/ViewCoordinator.java:14-38,121-234`).

### Main views

| View | Behavior |
|---|---|
| `LoginView` | Selects a registered profile or continues as guest (`src/jfx/LoginView.java:26-81`) |
| `PomodoroView` | Canvas timer, task/duration selection, complications, and completion dialog (`src/jfx/PomodoroView.java:69-149,449-603`) |
| `TaskView` | Add/edit/delete/list incomplete and completed tasks (`src/jfx/TaskView.java:69-188,326-407`) |
| `UserView` | Add/delete/assign users and open role-filtered settings (`src/jfx/UserView.java:41-221,266-354`) |
| `ReportView` | Five canvas/settings pages and text-report export (`src/jfx/ReportView.java:44-76,137-193,383-395`) |
| `SessionsView` | Session statistics, charts, recent sessions, and session actions (`src/jfx/SessionsView.java:49-80,246-374,386-430`) |
| `SettingsView` | Role-filtered settings in a separate transparent stage (`src/jfx/SettingsView.java:35-72,75-205`) |

## 6. Domain and service model

The primary services use `HashMap` storage and `AtomicInteger` ID generators:

- `UserService`: user CRUD, assignment, filters, and completion-rate aggregates (`src/services/UserService.java:15-233`).
- `TaskService`: task CRUD, status/priority/tag/user filters, overdue and completion statistics (`src/services/TaskService.java:15-316`).
- `PomodoroService`: task session records, increment/reset/delete operations, and focus-time/productivity aggregates (`src/services/PomodoroService.java:17-302`).
- `SettingsService`: defaults, role visibility, and an unused validation method (`src/services/SettingsService.java:12-76`).
- `ReportService`: text summaries and CSV exports (`src/services/ReportService.java:14-321`).

Task statuses are `PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, and `DUMMY` (`src/models/Status.java:3-5`). Priorities use enum order `LOW`, `MEDIUM`, `HIGH`, `URGENT` (`src/models/Priority.java:3-5`).

## 7. Persistence architecture

`JsonStorage` is a hand-written JSON serializer/parser with no JSON library (`src/services/JsonStorage.java:24-694`).

| File | Purpose |
|---|---|
| `data_users.json` | Shared user registry |
| `data_<userId>.json` | Per-user settings, tasks, stored Pomodoro records, and categories |
| `data.json` | Legacy unified format, migrated when the registry is absent and the legacy file exists |
| `notification.wav` | Generated/loaded notification sound |
| `icon.png` | Window icon |

Migration is implemented at `src/services/JsonStorage.java:52-78`. Per-user data is serialized at `src/services/JsonStorage.java:103-136`. Pomodoros are omitted from disk when their task has `storeSessions == false` (`src/services/JsonStorage.java:121-127`). Sessions and reports filter non-stored tasks while reading (`src/jfx/SessionsView.java:358-374`, `src/jfx/ReportView.java:137-174`).

`PostgresUserDAO` connects to `jdbc:postgresql://localhost:5432/chronos` with hard-coded credentials (`src/services/PostgresUserDAO.java:15-24`). SQL failures are caught and treated as an empty/unavailable registry (`src/services/PostgresUserDAO.java:31-55`).

## 8. Build, dependencies, and tests

There is no Maven, Gradle, Make, or package-manager build file. Compilation is manual `javac` with a JavaFX module path and PostgreSQL JDBC on the classpath.

Required external components:

- JDK 21
- JavaFX 21 SDK (`javafx.controls`, `javafx.graphics`, `javafx.base`)
- PostgreSQL JDBC driver, referenced by scripts as version 42.7.5
- JUnit Platform Console Standalone 1.11.4 for tests

The repository bundles only the JUnit runner under `lib/`. JavaFX and PostgreSQL driver JARs are not present. The current environment also has no `java` or `javac` command, so the inferred commands in `readme.md` were not executed.

Tests cover models and the three core in-memory services. There are no tests for `JsonStorage`, `PostgresUserDAO`, migration, resource loading, or JavaFX views.

## 9. Confirmed discrepancies

The detailed register is in `DECISION_LOG.md`. The highest-impact findings are:

1. The main application has no reproducible build/run configuration.
2. Stale `.class` files exist beside source files, including classes with no Java source.
3. `user-creator/*.bat` scripts contain machine-specific Windows paths.
4. Guest sessions can reach the Users tab and retain add/delete controls.
5. Task completion does not update the assigned user's completion counter.
6. `TaskService.sort_by_priority()` contradicts its “URGENT first” comment.
7. “Urgent incomplete” includes cancelled tasks.
8. `completedAt` is displayed but not persisted.
9. Shared mutable category state in `JsonStorage` can cross user boundaries.
10. Most `AppSettings` values are persisted but not enforced at runtime.
11. The settings validator is never called.
12. `ReportView` and timer aggregates do not consistently filter `storeSessions == false` records.
13. Database credentials are hard-coded and database failures are silently degraded.
14. Runtime resources are loaded relative to the working directory.
15. `plan_storeSessions_fix.md` describes a fix already present in `PomodoroView.complete()`.

## 10. Speculative observations

These are hypotheses for follow-up, not confirmed defects:

- `data_1.json` and its `DUMMY` task may be leftover test/runtime data rather than intended user data.
- `src/data.json` may be an obsolete duplicate legacy file because persistence resolves `data.json` relative to the process working directory.
- Orphan `UIUtils` and `IconGenerator` classes suggest a removed or unfinished refactor.
- The `.replit` Node.js module declaration may be leftover tooling configuration.
- The `SettingsView` role matrix is internally consistent with its comment, but guest behavior is represented by `null` rather than an explicit `GUEST` role.

See `SESSION_STATE.json` for the machine-readable scan state and `DECISION_LOG.md` for evidence, decisions, and recommended actions.
