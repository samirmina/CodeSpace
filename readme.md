# Chronos

Chronos is a Java 21/JavaFX 21 desktop Pomodoro and task manager with a watch-style interface, per-user JSON persistence, reports, session history, role-filtered settings, and optional PostgreSQL user synchronization.

## Included functionality

- Circular timer with task selection, custom duration, pause/reset, notification, and completion dialog
- Task CRUD with priority, status, tags, due dates, assignment, and session-storage preference
- User CRUD and task assignment
- Role-filtered settings
- Task/session reports and text export
- Session history and productivity views
- Shared JSON user registry plus per-user data files
- Optional PostgreSQL user-registry synchronization
- Separate JavaFX and CLI PostgreSQL user-management tool under `user-creator/`

## Project layout

```text
.
├── architecture.md
├── readme.md
├── DECISION_LOG.md
├── SESSION_STATE.json
├── data_users.json
├── data_1.json
├── lib/
│   └── junit-platform-console-standalone-1.11.4.jar
├── src/
│   ├── Main.java
│   ├── jfx/
│   ├── models/
│   ├── services/
│   └── utils/
├── test/
│   ├── models/
│   └── services/
└── user-creator/
    ├── compile.bat
    ├── run.bat
    ├── run-cli.bat
    └── src/main/java/com/chronos/usercrud/
```

## Current workspace status

This is a source snapshot, not a self-contained runnable project:

- No Maven, Gradle, Make, or main-application build/run script exists.
- JavaFX 21 and the PostgreSQL JDBC driver are not bundled.
- The current environment has no `java` or `javac` command.
- `user-creator/*.bat` scripts contain Windows-specific absolute paths from another machine.
- `.replit` declares `nodejs-20`, which does not match the Java/JavaFX application.
- Compiled `.class` files coexist with source files; some have no corresponding `.java` source.

See `DECISION_LOG.md` for evidence and prioritized discrepancies.

## Requirements

- JDK 21
- JavaFX 21 SDK
- PostgreSQL JDBC driver, if database synchronization is required
- PostgreSQL at `localhost:5432` with database `chronos`, if database synchronization is required
- Run from the workspace root so relative data and resource paths resolve correctly

The DAO uses hard-coded database credentials (`src/services/PostgresUserDAO.java:15-24`). Replace them with environment/configuration before production use.

## Manual build

These commands are inferred from the source layout and the separate `user-creator` scripts because no main build script exists:

```bash
export JAVA_HOME=/path/to/jdk-21
export JAVAFX_HOME=/path/to/javafx-sdk-21.0.2
export PG_JDBC=/path/to/postgresql-42.7.5.jar

mkdir -p out
"$JAVA_HOME/bin/javac" \
  --module-path "$JAVAFX_HOME/lib" \
  --add-modules javafx.controls,javafx.graphics,javafx.base \
  -cp "$PG_JDBC" \
  -d out \
  src/Main.java \
  src/models/*.java \
  src/services/*.java \
  src/utils/*.java \
  src/jfx/*.java
```

Do not compile `src/**/*.class` files. Use Java source as the source of truth.

## Manual run

```bash
"$JAVA_HOME/bin/java" \
  --module-path "$JAVAFX_HOME/lib" \
  --add-modules javafx.controls,javafx.graphics,javafx.base \
  -cp "out:$PG_JDBC" \
  Main
```

If PostgreSQL is unavailable, startup continues using JSON users because DAO SQL errors are swallowed.

## Tests

The repository contains 11 test source files and 156 JUnit test methods. Compile tests against `out`, then run them with the bundled console launcher:

```bash
mkdir -p test-out
"$JAVA_HOME/bin/javac" \
  -cp "out:lib/junit-platform-console-standalone-1.11.4.jar" \
  -d test-out \
  test/models/*.java \
  test/services/*.java

"$JAVA_HOME/bin/java" \
  -jar lib/junit-platform-console-standalone-1.11.4.jar \
  --class-path "out:test-out" \
  --scan-class-path test-out
```

Tests cover models and core services only. JSON persistence, database access, migration, resource loading, and all JavaFX views are untested.

## Data model

- `data_users.json`: shared user registry.
- `data_<userId>.json`: settings, tasks, stored Pomodoro records, and categories for one user.
- `data.json`: legacy unified format. Migration is skipped whenever `data_users.json` exists, even if the registry is empty (`src/services/JsonStorage.java:42-55`).
- `src/data.json`: duplicate legacy-format file that is not used when the process runs from the workspace root.

A task's `storeSessions` flag suppresses session increments and disk persistence when a timer completes (`src/jfx/PomodoroView.java:449-464`, `src/services/JsonStorage.java:121-127`). Sessions and reports filter non-stored tasks while reading data (`src/jfx/SessionsView.java:358-374`, `src/jfx/ReportView.java:137-174`).

## Known discrepancies

- Guest users can open the Users tab and retain add/delete controls (`src/jfx/MainView.java:243-248`, `src/jfx/UserView.java:75-109,127-171`).
- Task completion does not increment the assigned user's completion counter.
- `TaskService.sort_by_priority()` sorts `LOW` to `URGENT`, contrary to its “URGENT first” comment (`src/services/TaskService.java:194-198`).
- “Urgent incomplete” includes cancelled tasks (`src/services/TaskService.java:181-190`).
- `completedAt` is displayed but not serialized by `JsonStorage` (`src/jfx/TaskView.java:349-357`, `src/services/JsonStorage.java:223-257,388-441`).
- Shared mutable category state in `JsonStorage` can cross user boundaries.
- Most `AppSettings` values are persisted but not enforced by runtime behavior.
- `SettingsService.validate()` is never called.
- `ReportView` and timer aggregates do not consistently filter `storeSessions == false` records.
- `plan_storeSessions_fix.md` is stale: its proposed timer fix is already present in source.
- Stale `.class` files, including orphan classes without source, exist under `src/`.
- `src/jfx/UserView.java.bak` is incompatible with current constructors.
- Runtime resources are loaded relative to the working directory rather than as classpath resources.

## Documentation

- `architecture.md`: system structure, data flow, modules, persistence, and runtime model.
- `DECISION_LOG.md`: evidence-backed discrepancy and decision register.
- `SESSION_STATE.json`: machine-readable scan and project state.
