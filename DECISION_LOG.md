# Chronos Decision Log

Scan date: 2026-09-24
Scope: source, tests, data files, scripts, configuration, and compiled artifacts under `/home/runner/workspace`.

## Status

- Documentation updated: `architecture.md`, `readme.md`, `DECISION_LOG.md`, `SESSION_STATE.json`.
- Main Java source count: 29 files.
- Test source count: 11 files, containing 156 `@Test` methods.
- No build system or main-application run script was found.
- The current environment has no `java` or `javac`.
- JavaFX and PostgreSQL JDBC driver JARs are not bundled.
- JavaFX and PostgreSQL JDBC are external runtime requirements.

## Confirmed discrepancies

### D-001 — No reproducible main-project build

Severity: Critical
Status: Confirmed

Evidence:

- No `pom.xml`, `build.gradle`, `settings.gradle`, `Makefile`, or main `run.sh` exists.
- `src/Main.java` imports JavaFX classes, and `src/services/PostgresUserDAO.java:19-24` loads the PostgreSQL driver.
- The workspace contains the JUnit runner but not JavaFX or PostgreSQL JDBC JARs.
- The current environment has no `java` or `javac` executable.

Decision: Treat the workspace as a source snapshot. Add a build tool or checked-in scripts and document external dependencies before claiming a reproducible run.

### D-002 — `user-creator` scripts are machine-specific

Severity: High
Status: Confirmed

Evidence:

- `user-creator/compile.bat:2-9`, `run.bat:2-8`, and `run-cli.bat:2-8` use absolute Windows paths such as `E:\.jdks\ms-21.0.10` and a user-specific Maven cache path.
- The scripts target the separate `com.chronos.usercrud` tool, not the main `src/Main.java` application.

Decision: Keep the user-creator tool separate, but replace absolute paths with configurable environment variables or provide portable scripts.

### D-003 — Stale compiled artifacts

Severity: High
Status: Confirmed

Evidence:

- `src/Main.class` is 1,229 bytes, while `out/Main.class` is 8,856 bytes. The source tree also contains `Task$Priority.class` and `Task$Status.class`, although the current `Task.java` uses top-level `Priority` and `Status` types.

Decision: Delete generated classes from source trees, rebuild from Java sources, and add `**/*.class` to `.gitignore`.

### D-004 — `plan_storeSessions_fix.md` is stale

Severity: Medium
Status: Confirmed

Evidence:

- `plan_storeSessions_fix.md:47-65` proposes guarding `PomodoroView.complete()` before incrementing and saving.
- `src/jfx/PomodoroView.java:454-464` already contains the guard using `task.is_store_sessions()`.

Decision: Archive or update the plan. Do not apply the same fix again.

### D-005 — Guest access can mutate users

Severity: High
Status: Confirmed

Evidence:

- `MainView.isTabVisible()` exposes the Users tab to guests (`src/jfx/MainView.java:243-248`).
- `UserView` disables task assignment for guests but leaves add/delete controls active (`src/jfx/UserView.java:75-109,127-171`).
- The add/delete handlers create or remove users and persist the registry without a guest check.

Decision: Either hide the Users tab for guests or disable all mutating user actions for `currentUser == null`.

### D-006 — Task completion counter is not updated

Severity: High
Status: Confirmed

Evidence:

- `TaskView` changes task status through `TaskService.update_task_status()` (`src/jfx/TaskView.java:179-188`).
- `UserService.mark_task_completed_for_user()` exists (`src/services/UserService.java:137-141`) but is not called by the task completion UI path.

Decision: Update the assigned user's completion count transactionally when a task enters `COMPLETED`, or remove the counter from reports if it is not intended to be maintained.

### D-007 — Priority and urgent-task semantics are inconsistent

Severity: Medium
Status: Confirmed

Evidence:

- `TaskService.sort_by_priority()` uses enum `compareTo()` (`src/services/TaskService.java:194-198`), producing `LOW` to `URGENT`, while its comment says urgent first.
- `get_urgent_incomplete_tasks()` excludes only `COMPLETED` (`src/services/TaskService.java:181-190`), so cancelled tasks can be counted as urgent incomplete.

Decision: Use an explicit descending priority comparator and define whether cancelled tasks are incomplete.

### D-008 — Task completion date is not persisted

Severity: High
Status: Confirmed

Evidence:

- `TaskView` displays `Task.get_completed_at()` (`src/jfx/TaskView.java:349-357`).
- `JsonStorage.append_tasks_to_json()` and `load_tasks_from_json()` do not write or read `completedAt` (`src/services/JsonStorage.java:223-257,388-441`).

Decision: Add `completedAt` round-trip support and a persistence regression test.

### D-009 — JsonStorage has cross-user and session-history gaps

Severity: High
Status: Confirmed

Evidence:

- `JsonStorage` keeps mutable `categories`, `tags`, and `settings` fields on one instance (`src/services/JsonStorage.java:27-29`).
- `load_categories_from_json()` clears and repopulates the shared category list (`src/services/JsonStorage.java:538-549`), while `save_user_data()` writes that shared list into each user's file.
- When `completedSessions > 0` but `sessionDurations` is empty, loading fabricates durations without session history (`src/services/JsonStorage.java:522-526`).
- `SessionsView` limits history display to the minimum of history and durations (`src/jfx/SessionsView.java:397-403`).

Decision: Make persistence state per-user or pass state explicitly; synchronize session count, duration, and history; add round-trip tests.

### D-010 — `storeSessions` filtering is incomplete in aggregates

Severity: High
Status: Confirmed

Evidence:

- Timer completion correctly checks `storeSessions` before incrementing and saving (`src/jfx/PomodoroView.java:454-464`).
- `PomodoroService` total-session and focus-time methods aggregate all records (`src/services/PomodoroService.java:102-118`).
- `PomodoroView` displays those unfiltered totals (`src/jfx/PomodoroView.java:268-303,329-340`).
- `ReportView` filters most session data but obtains the most-productive task from an unfiltered aggregate (`src/jfx/ReportView.java:137-193`, `src/services/PomodoroService.java:234-247`).

Decision: Filter by `storeSessions` at the service boundary or introduce explicit stored-session query methods.

### D-011 — Settings validation and policy controls are incomplete

Severity: Medium
Status: Confirmed

Evidence:

- `SettingsService.validate()` is defined but has no callers (`src/services/SettingsService.java:44-76`).
- `AppSettings.userManagement` and `roleManagement` are persisted but have no runtime enforcement in the scanned source.
- `SettingsView` hard-codes the denominator as 10 (`src/jfx/SettingsView.java:50`).

Decision: Call validation on save, enforce management flags at action boundaries, and derive the denominator from a named setting count.

### D-012 — Startup performs duplicate PostgreSQL merge and duplicate saves

Severity: Medium
Status: Confirmed

Evidence:

- `Main.start()` runs the same PostgreSQL-to-`UserService` merge twice (`src/Main.java:45-51,58-64`).
- `save_and_exit()` is registered as both the close handler and a shutdown hook (`src/Main.java:102-113`).

Decision: Keep one merge and one persistence path, or make persistence idempotent.

### D-013 — Legacy and runtime data ownership is unclear

Severity: Medium
Status: Confirmed

Evidence:

- `data_users.json` is empty, `data_1.json` contains a `DUMMY` task, and `src/data.json` is a legacy-format file.
- `JsonStorage` resolves `data.json` relative to the process working directory (`src/services/JsonStorage.java:24-26,42-55`), so `src/data.json` is not the root legacy file when launched normally.
- `Status.DUMMY` is exposed in the task edit UI (`src/jfx/TaskView.java:106`) and can appear in statistics/reports.

Decision: Define canonical data-file locations, remove test residue from runtime data, and decide whether `DUMMY` belongs in the production status enum.

### D-014 — Resource loading depends on the working directory

Severity: Medium
Status: Confirmed

Evidence:

- `Main` loads `icon.png` as a relative file (`src/Main.java:70-74`).
- `AudioPlayer` loads or generates `notification.wav` relative to the current directory (`src/utils/AudioPlayer.java:12-19`).

Decision: Load resources from the classpath or document the working-directory contract.

### D-015 — Dead or unfinished code is present

Severity: Low
Status: Confirmed

Evidence:

- `SettingsService.validate()` has no callers.
- `PomodoroService.start_session()`, `stop_session()`, and active-session queries have no UI callers.
- `SessionsView.ovMost` and `ovRecent` are created but never populated with `setText`.
- `src/utils/UIUtils.java` and `src/utils/IconGenerator.java` are absent while compiled classes remain.
- `src/jfx/UserView.java.bak` has an obsolete constructor signature.

Decision: Remove or deliberately document dead APIs and artifacts; do not compile backup files.

## Speculative observations

These require product or history confirmation:

- `data_1.json` may be a leftover test fixture because its task is named `Work Shift-dummy`, has `storeSessions: false`, and the current registry is empty.
- `src/data.json` may be an obsolete duplicate of a legacy file rather than an intended migration source.
- Orphan `UIUtils` and `IconGenerator` classes may indicate a removed UI utility or an incomplete build.
- The `.replit` Node.js declaration may be leftover tooling configuration.
- The role hierarchy is internally consistent with its comment, but guest access is represented by `null` rather than an explicit `Role.GUEST`.

## Decisions recorded

1. Java source is the source of truth; compiled artifacts are not.
2. The main app and `user-creator` are separate runtime products.
3. PostgreSQL is an optional user-registry source with silent JSON fallback, but hard-coded credentials are not production-safe.
4. `storeSessions` should be enforced consistently by service queries, UI displays, and persistence.
5. Documentation should describe the current snapshot accurately rather than imply that it is immediately runnable.
6. Persistence, migration, guest authorization, and resource-loading gaps should be addressed before production use.

## Validation performed

- Source and test files were scanned.
- File counts and artifact sizes were checked.
- No Java runtime was available, so compilation and JUnit execution were not performed.
- `SESSION_STATE.json` is intended to be machine-readable state for this scan.
