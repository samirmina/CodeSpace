# Chronos

Chronos is a Java 21 Pomodoro & Task Manager — a Spring Boot web application with a JavaFX rendering layer. All HTML is generated programmatically in Java; there are no Thymeleaf templates, HTML files, or CSS files in the repository. The entire UI is rendered by Java code.

## Features

- **Pomodoro timer** — task selection, custom duration, pause/reset/complete, session tracking
- **Task management** — CRUD with priority, status, tags, due dates, user assignment, session-storage preference
- **User management** — CRUD, roles (Admin/Developer/Manager), role-based settings visibility
- **Sessions** — pomodoro session history and active session tracking
- **Reports** — task statistics, completion rates, top performers, status breakdown
- **Settings** — theme, default duration, notification sound, auto-start, show-all-tasks, daily focus goal
- **Multi-user** — session-scoped authentication, per-user data isolation
- **PostgreSQL** — optional persistent storage via Neon or any PostgreSQL database

## Tech Stack

- Java 21
- Spring Boot 3.3 (web layer)
- Thymeleaf-free — all views generated in Java via `HtmlRenderer`
- PostgreSQL (Neon or self-hosted)
- Maven 3.9+
- Docker (Dockerfile + docker-compose for app + Postgres)

## Project Layout

```
.
├── README.md                    ← this file
├── pom.xml                      ← Maven build
├── docker/Dockerfile            ← Docker build for Spring Boot web app
├── docker/docker-compose.yml    ← app + Postgres containers
├── src/
│   ├── Main.java                ← JavaFX desktop entry point
│   ├── web/
│   │   ├── ChronosWebApplication.java   ← Spring Boot entry point
│   │   ├── controller/
│   │   │   └── WebController.java       ← all UI routes, HTML via renderer
│   │   └── renderer/
│   │       └── HtmlRenderer.java        ← programmatic HTML + inline CSS
│   ├── jfx/                       ← JavaFX desktop GUI views
│   ├── models/                    ← data models (User, Task, Pomodoro, etc.)
│   ├── services/                  ← business logic (TaskService, UserService, etc.)
│   └── utils/                     ← file storage helpers
├── test/                          ← JUnit 5 tests (156 tests)
└── .gitignore
```

## Build & Run

### Local (Maven)

```bash
# Compile
mvn compile

# Run tests (156 tests)
mvn test

# Run Spring Boot web app
mvn spring-boot:run
```

The web app starts on `http://localhost:8080`.

### Docker

```bash
# Build and run with PostgreSQL
docker-compose up --build
```

The web app is available on port 8080, Postgres on 5433.

## Pages

| Route | Description |
|-------|-------------|
| `/` | Home / landing page |
| `/login` | User selection |
| `/timer` | Pomodoro timer dashboard |
| `/tasks` | Task management |
| `/users` | User administration |
| `/sessions` | Pomodoro session history |
| `/reports` | Reports and statistics |
| `/settings` | Application settings |
| `/logout` | Sign out |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/chronos` | PostgreSQL connection URL |
| `DB_USER` | `chronos` | Database user |
| `DB_PASSWORD` | `chronos` | Database password |
| `SPRING_PROFILES_ACTIVE` | — | Set to `production` for deploy |

## Testing

```bash
mvn test
```

156 tests cover models and core services.

## Architecture

The project is **100% Java**. There are no HTML, CSS, or JavaScript files. All UI rendering is done programmatically by `HtmlRenderer`, which builds HTML strings with inline CSS directly in Java code. The Spring Boot `WebController` maps HTTP routes to renderer methods, returning `text/html` responses.
