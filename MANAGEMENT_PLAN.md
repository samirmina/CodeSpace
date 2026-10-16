# Chronos — Maintenance, Bug Fix & Expansion Plan

> **Generated from codebase scan on 2026-09-25**  
> Scope: `src/` (main app), `test/` (unit tests), `user-creator/` (standalone tool), JSON data files, `*.bat` scripts.

---

## 1. EXECUTIVE SUMMARY

Chronos is a **Java 21 / JavaFX 21 desktop Pomodoro & task manager** with:
- **Per-user JSON persistence** (`data_users.json` + `data_<userId>.json`)
- **PostgreSQL user sync** (optional, hardcoded credentials)
- **5-tab circular UI**: Timer → Tasks → Users → Reports → Sessions
- **No build system** (Maven/Gradle/Make) — compiled via Windows-only `.bat` with absolute paths
- **No Java runtime in current environment** — cannot compile/run/test locally
- **156 `@Test` methods** across 11 test files (JUnit 5, console runner bundled)

---

## 2. MAINTENANCE BACKLOG

| ID | Area | Issue | Severity | Effort | Notes |
|---|---|---|---|---|---|
| M-01 | Build | **No reproducible build** — no `pom.xml`/`build.gradle`/Makefile | Critical | M | Add Maven or Gradle; vendor JavaFX + Postgres JDBC |
| M-02 | Scripts | **Windows-only `.bat` with absolute paths** (`E:\.jdks\...`, `C:\Users\Meena\...`) | High | S | Replace with portable scripts or `mvnw`/`gradlew` |
| M-03 | Dependencies | **JavaFX & Postgres JDBC not bundled** — only JUnit runner in `lib/` | High | S | Declare in build file; use `jlink`/`jpackage` for distribution |
| M-04 | Artifacts | **Stale `.class` files in source tree** (`src/Main.class`, `src/models/Task$Priority.class`, etc.) | Medium | S | Delete; add `**/*.class` to `.gitignore` |
| M-05 | Config | `.replit` declares `nodejs-20` module — mismatch for Java project | Low | S | Remove or update to Java module |
| M-06 | Data | `data_users.json` empty; `data_1.json` may be stale | Low | S | Document expected schema; add migration versioning |
| M-07 | Tests | Tests not runnable without Java; no CI config | Medium | M | Add GitHub Actions / GitLab CI with JDK 21 + JavaFX |
| M-08 | Logging | `System.out.println` throughout; no structured logging (SLF4J/Logback) | Low | M | Introduce logging facade; configure levels |
| M-09 | Code style | Inconsistent brace style, mixed `this.` usage, long methods (>200 lines in views) | Low | L | Apply Spotless/Checkstyle; extract view helpers |
| M-10 | Dead code | `plan_storeSessions_fix.md` describes fix already in `PomodoroView.complete()` | Low | S | Archive or delete stale plan |

---

## 3. BUG FIXES

| ID | Component | Bug | Severity | Evidence | Fix Sketch |
|---|---|---|---|---|---|
| B-01 | `UserView` / `MainView` | **Guest users can access Users tab** (`idx==2`) and see Add/Delete controls | High | `MainView.isTabVisible()` returns `true` for `idx==2` when `currentUser==null`; `UserView` disables only Assign, not Add/Delete | In `isTabVisible()`: return `false` for `idx==2` when guest; OR in `UserView` constructor: disable `btnAdd`/`btnDelete` for guests |
| B-02 | `JsonStorage` | **`Task.completedAt` not serialized/deserialized** — lost on save/load | High | `append_tasks_to_json()` omits `completedAt`; `load_tasks_from_json()` doesn't read it | Add `"completedAt": "..."` to JSON; parse on load; default to `null` for legacy data |
| B-03 | `ReportView` / `SessionsView` | **Inconsistent `storeSessions` filtering** — some aggregates filter, others don't | Medium | `ReportView.gather_data()` filters at lines 143, 163, 174, 187; `SessionsView.is_pomodoro_visible()` filters at line 373; but `PomodoroService.get_total_sessions()` (line 103) returns **all** sessions | Add `get_total_stored_sessions()` / `get_total_stored_focus_time()` to `PomodoroService`; use in overview stats |
| B-04 | `PomodoroView.complete()` | **Saves user data even when `storeSessions=false`** (line 461-462) | Medium | Guard at 455-457 checks `storeSession` for increment, but save at 461-462 only checks `currentUser != null` | Move `storage.save_user_data()` inside `if (storeSession)` block |
| B-05 | `UserView` | **Add/Delete user persists without guest check** (lines 127-143, 162-172) | High | Handlers call `storage.save_user_registry()` unconditionally | Add `if (currentUser == null) return;` or disable buttons for guests |
| B-06 | `TaskView` | **Completed tasks view shows `get_completed_at()` which may be null** (line 356) | Low | `Task.set_status()` sets `completedAt` only when transitioning **to** COMPLETED; legacy data may lack it | Safe-guard: `t.get_completed_at() != null ? ... : "Unknown"` (already at line 356) — verify |
| B-07 | `PostgresUserDAO` | **Hardcoded DB credentials** (`postgres`/`postgres` on localhost) | Medium | Lines 15-17 | Externalize to config file / env vars; support connection pooling |
| B-08 | `JsonStorage.migrate_legacy_data_if_needed()` | **Migration reloads JSON but doesn't re-merge PostgreSQL users** | Low | Lines 55-77: migration calls `load_data()` which clears userService, then saves per-user files; but pg users already merged before migration (lines 45-51) are lost | Re-merge pg users after migration (already done at lines 58-64 in `Main.start()`) — verify order |
| B-09 | `Task` model | **`completionTimeMinutes` not set automatically** when status → COMPLETED | Low | `Task.set_status()` sets `completedAt` but not `completionTimeMinutes` | Compute duration from `createdAt` to `completedAt` on completion; or add explicit `markCompleted()` method |
| B-10 | `Pomodoro` model | **`sessionDurations` can diverge from `sessionHistory` size** | Low | `increment_sessions()` adds to both lists, but `reset_sessions()` clears both; direct JSON load may create mismatch | Assert equal sizes in `get_total_focus_time_minutes()`; fix on load |

---

## 4. EXPANSION OPPORTUNITIES

| ID | Feature | Description | Priority | Effort | Dependencies |
|---|---|---|---|---|---|
| E-01 | **Build System** | Add Maven/Gradle with JavaFX plugin, dependency management, `jpackage` profile | P0 | M | M-01, M-03 |
| E-02 | **CI/CD Pipeline** | GitHub Actions: compile, test, package (native installer) on push/PR | P0 | M | E-01 |
| E-03 | **Settings Persistence** | Per-user `AppSettings` in `data_<userId>.json` (currently only global) | P1 | S | JsonStorage |
| E-04 | **Task Categories/Tags UI** | CRUD for `Category`/`Tag` models (exist in model + JSON but no UI) | P1 | M | TaskView, JsonStorage |
| E-05 | **Data Export/Import** | CSV/JSON export of tasks, sessions, reports; import wizard | P1 | M | ReportService, JsonStorage |
| E-06 | **Keyboard Shortcuts** | Global hotkeys: Start/Pause timer, New task, Switch tabs | P1 | S | MainView, PomodoroView |
| E-07 | **Notifications** | System tray / OS notifications on session complete (uses `AudioPlayer` only now) | P1 | M | AudioPlayer, JavaFX `Stage` |
| E-08 | **Multi-user Task Assignment** | Drag-drop task assignment in TaskView; notify assignee | P2 | M | UserService, TaskService |
| E-09 | **Recurring Tasks** | `Task.recurrenceRule` (RRULE); auto-create next occurrence on completion | P2 | L | Task model, scheduler |
| E-10 | **Team Reports** | Aggregate reports across users (role-gated via `teamReportAccess` setting) | P2 | M | ReportService, UserService |
| E-11 | **Plugin/Extension API** | Load external JARs for custom reports, integrations (Jira, GitHub, etc.) | P3 | L | ClassLoader isolation |
| E-12 | **Mobile/Tablet Companion** | React Native / Flutter app syncing via REST API (Spring Boot backend) | P3 | XL | New backend |

---

## 5. TECH DEBT & ARCHITECTURAL IMPROVEMENTS

| ID | Area | Description |
|---|---|---|
| T-01 | **Service Layer** | Extract interfaces (`TaskService`, `UserService`, `PomodoroService`) for testability & swapable storage |
| T-02 | **Persistence Abstraction** | `StorageBackend` interface → `JsonStorageImpl`, `PostgresStorageImpl`, `SqliteStorageImpl` |
| T-03 | **Event Bus** | Replace direct service calls in views with events (`TaskCreated`, `SessionCompleted`, `UserLoggedIn`) |
| T-04 | **Dependency Injection** | Lightweight DI (Dagger/Hilt/Manual) to break `Main` → services → views circular wiring |
| T-05 | **View Models** | Extract presentation logic from `*View` classes into `*ViewModel` (MVVM) |
| T-06 | **Date/Time Handling** | Standardize on `java.time` everywhere; remove `javafx.util.Pair` → use `AbstractMap.SimpleEntry` or record |
| T-07 | **Error Handling** | Replace `System.err.println` + empty catch blocks with `Result<T, E>` or checked exceptions |
| T-08 | **Concurrency** | Services use plain `HashMap` — not thread-safe; add `ReadWriteLock` or use `ConcurrentHashMap` |
| T-09 | **Test Coverage** | Current tests cover service logic only; add UI tests (TestFX), integration tests (Testcontainers for Postgres) |
| T-10 | **Documentation** | Keep `architecture.md`, `readme.md`, `DECISION_LOG.md`, `SESSION_STATE.json` in sync with code |

---

## 6. IMMEDIATE NEXT STEPS (Priority Order)

1. **Add Maven build** (`pom.xml`) with:
   - `javafx-maven-plugin` (or `org.openjfx:javafx-*` deps)
   - `postgresql:postgresql` JDBC driver
   - `junit-jupiter` + `junit-platform-console-standalone` for test execution
   - `maven-shade-plugin` or `jpackage` for native installer

2. **Fix Critical Bugs** (B-01, B-02, B-04, B-05) — guest access + data loss

3. **Delete stale `.class` files** + add `.gitignore`

4. **Portable run script** (`run.sh` / `mvnw` wrapper) to replace `.bat`

5. **Externalize DB config** (B-07) — `config.properties` or env vars

6. **CI pipeline** — GitHub Actions with `setup-java`, `cache` Maven, run tests

7. **Settings persistence per-user** (E-03) — low effort, high value

8. **Categories/Tags UI** (E-04) — model + JSON already exist

---

## 7. TEST STRATEGY

| Layer | Current | Target |
|---|---|---|
| Unit (services) | 156 tests, good coverage | Maintain; add mutation testing (Pitest) |
| Integration (JSON/Postgres) | None | Testcontainers for Postgres; temp-dir JSON tests |
| UI (JavaFX) | None | TestFX for critical flows (login, timer, CRUD) |
| Contract | None | Pact/REST-assured if API added (E-12) |

---

## 8. RISK REGISTER

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Cannot build/run without Windows machine | High | Blocks all dev | **Fix M-01 immediately** |
| Data corruption during migration | Medium | High | Add migration version + backup + rollback test |
| PostgreSQL unavailable in CI | Medium | Medium | Use Testcontainers; make PG optional at runtime |
| JavaFX packaging complexity | Medium | High | Use `jpackage` via Maven plugin; test on Linux/macOS/Windows |
| Guest data leakage (B-01, B-05) | High | High | **Fix before any release** |

---

## 9. FILE MAP (Key Entry Points)

```
src/
├── Main.java                    ← App entry, login → main view, user switching
├── services/
│   ├── JsonStorage.java         ← Core persistence (per-user JSON + migration)
│   ├── TaskService.java         ← Task CRUD, filters, stats
│   ├── PomodoroService.java     ← Session CRUD, aggregates
│   ├── UserService.java         ← User CRUD, roles, assignments
│   ├── PostgresUserDAO.java     ← PostgreSQL sync (hardcoded creds)
│   ├── SettingsService.java     ← Role-gated settings validation
│   └── ReportService.java       ← Report generation (text)
├── models/                      ← POJOs: Task, Pomodoro, User, Category, Tag, Role, Priority, Status, AppSettings
└── jfx/                         ← JavaFX Views (5 tabs + Login + Settings)
    ├── MainView.java            ← Circular nav, tab visibility (guest bug here)
    ├── PomodoroView.java        ← Timer, session completion (storeSessions logic)
    ├── TaskView.java            ← Task CRUD, completed history
    ├── UserView.java            ← User CRUD, assign tasks (guest bug here)
    ├── ReportView.java          ← Charts, export (storeSessions filtering)
    ├── SessionsView.java        ← Session history, detail, reset/delete
    ├── LoginView.java           ← User picker
    └── SettingsView.java        ← Role-gated settings editor

user-creator/                    ← Standalone PostgreSQL user management (GUI + CLI)
├── src/main/java/com/chronos/usercrud/
│   ├── UserCrudApp.java         ← JavaFX GUI
│   ├── UserCrudCLI.java         ← CLI
│   ├── UserDAO.java             ← JDBC wrapper
│   └── User.java                ← Model (duplicate of main)
├── compile.bat / run.bat        ← Windows-only, absolute paths
└── run-cli.bat
```

---

## 10. DECISION LOG REFERENCE

See `DECISION_LOG.md` for evidence-backed entries:
- D-001: No reproducible build
- D-002: Machine-specific `.bat` scripts
- D-003: Stale compiled artifacts
- D-004: Stale `plan_storeSessions_fix.md`
- D-005: Guest user mutation access
- D-006: `Task.completedAt` not persisted
- D-007: Inconsistent `storeSessions` filtering
- D-008: Hardcoded PostgreSQL credentials
- D-009: `completionTimeMinutes` not auto-set
- D-010: `sessionDurations`/`sessionHistory` size mismatch risk