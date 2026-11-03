package web.controller;

import models.AppSettings;
import models.Priority;
import models.Role;
import models.Status;
import models.Task;
import models.User;
import models.Pomodoro;
import services.PomodoroService;
import services.SettingsService;
import services.TaskService;
import services.UserService;
import web.renderer.HtmlRenderer;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * WebController - handles all web UI routes with multi-user support.
 * Uses HtmlRenderer for programmatic HTML generation - no external HTML/CSS files.
 * All HTML is generated in Java code.
 */
@Controller
@RequestMapping("/")
public class WebController {

    private final UserService userService;
    private final TaskService taskService;
    private final PomodoroService pomodoroService;
    private final SettingsService settingsService;
    private final HtmlRenderer renderer;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public WebController(UserService userService, TaskService taskService,
                         PomodoroService pomodoroService, SettingsService settingsService,
                         HtmlRenderer renderer) {
        this.userService = userService;
        this.taskService = taskService;
        this.pomodoroService = pomodoroService;
        this.settingsService = settingsService;
        this.renderer = renderer;
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    private boolean authenticated(HttpSession session) {
        return session.getAttribute("currentUser") != null;
    }

    @GetMapping("/")
    public Object home(HttpSession session) {
        User u = currentUser(session);
        if (u == null) {
            String html = renderer.renderHomePage(null);
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
        }
        return new RedirectView("/timer");
    }

    @GetMapping("/login")
    public Object login(HttpSession session) {
        if (authenticated(session)) {
            return new RedirectView("/timer");
        }
        String html = renderer.renderLoginPage(
            userService.get_all_users(),
            (String) session.getAttribute("flashMessage"),
            (String) session.getAttribute("flashType")
        );
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashType");
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @PostMapping("/login")
    public RedirectView selectUser(@RequestParam("email") String email, HttpSession session) {
        User u = currentUser(session);
        if (u != null) return new RedirectView("/timer");

        // Create user on demand if they don't exist
        Optional<User> existing = userService.get_all_users().stream()
            .filter(usr -> usr.get_email().equalsIgnoreCase(email.trim()))
            .findFirst();

        if (existing.isEmpty()) {
            // Auto-create user with email as name, DEVELOPER role by default
            String name = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
            u = userService.create_user(name, email.trim(), Role.DEVELOPER);
            System.out.println("✓ Auto-created user: " + u.get_name() + " (" + u.get_email() + ")");
        } else {
            u = existing.get();
        }

        session.setAttribute("currentUser", u);
        session.setAttribute("isGuest", false);
        return new RedirectView("/timer");
    }

    @GetMapping("/logout")
    public RedirectView logout(HttpSession session) {
        if (session != null) {
            session.removeAttribute("currentUser");
            session.removeAttribute("isGuest");
            session.invalidate();
        }
        return new RedirectView("/login");
    }

    @GetMapping("/timer")
    public Object timer(HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");

        long userId = u.get_id();
        List<Task> tasks = taskService.get_all_tasks(userId);

        String html = renderer.renderTimerPage(
            u, tasks,
            settingsService.getSettings().getDefaultPomodoroDuration(),
            pomodoroService.get_total_sessions(),
            pomodoroService.get_total_focus_time_minutes(),
            pomodoroService.get_total_stored_sessions(taskService),
            pomodoroService.get_total_stored_focus_time_minutes(taskService),
            pomodoroService.get_active_days_count(),
            pomodoroService.get_most_productive_task(),
            pomodoroService.get_active_pomodoros(),
            pomodoroService.get_last_sessions(10),
            (String) session.getAttribute("flashMessage"),
            (String) session.getAttribute("flashType")
        );
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashType");
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @PostMapping("/timer/start")
    public RedirectView startTimer(@RequestParam("taskId") Integer taskId,
                                   @RequestParam("durationMinutes") Integer durationMinutes,
                                   HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");
        Task task = taskService.try_get_task(taskId).orElse(null);
        if (task == null) return new RedirectView("/timer");
        int duration = (durationMinutes != null) ? durationMinutes : settingsService.getSettings().getDefaultPomodoroDuration();
        pomodoroService.create_pomodoro(taskId, task.get_title(), duration, u.get_id());
        pomodoroService.start_session(pomodoroService.count_pomodoros());
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/pause")
    public RedirectView pauseTimer(@RequestParam Integer pomodoroId, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        pomodoroService.stop_session(pomodoroId);
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/resume")
    public RedirectView resumeTimer(@RequestParam Integer pomodoroId, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        pomodoroService.start_session(pomodoroId);
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/complete")
    public RedirectView completeTimer(@RequestParam Integer pomodoroId, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        pomodoroService.increment_session(pomodoroId);
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/reset")
    public RedirectView resetTimer(@RequestParam Integer pomodoroId, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        pomodoroService.reset_sessions(pomodoroId);
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/delete")
    public RedirectView deleteTimer(@RequestParam Integer pomodoroId, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        pomodoroService.delete_pomodoro(pomodoroId);
        return new RedirectView("/timer");
    }

    @GetMapping("/tasks")
    public Object tasks(HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");

        List<Task> allTasks = taskService.get_all_tasks(u.get_id());

        String html = renderer.renderTasksPage(
            u, allTasks, allTasks.size(),
            taskService.get_completion_rate(),
            taskService.count_overdue_tasks(),
            countStatus(allTasks, Status.PENDING),
            countStatus(allTasks, Status.IN_PROGRESS),
            countStatus(allTasks, Status.COMPLETED),
            countStatus(allTasks, Status.CANCELLED),
            userService.get_all_users(),
            List.of(Status.values()),
            List.of(Priority.values()),
            (String) session.getAttribute("flashMessage"),
            (String) session.getAttribute("flashType")
        );
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashType");
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @PostMapping("/tasks")
    public RedirectView createTask(@RequestParam("title") String title,
                                   @RequestParam("description") String description,
                                   @RequestParam("priority") Priority priority,
                                   @RequestParam("assignedUserId") Integer assignedUserId,
                                   @RequestParam("storeSessions") Boolean storeSessions,
                                   HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");
        Task created = taskService.create_task(title, description, priority, u.get_id());
        if (assignedUserId != null && assignedUserId > 0) {
            created.set_assigned_user_id(assignedUserId);
        }
        if (storeSessions != null) {
            created.set_store_sessions(storeSessions);
        }
        return new RedirectView("/tasks");
    }

    @PostMapping("/tasks/{id}/delete")
    public RedirectView deleteTask(@PathVariable int id, HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");
        pomodoroService.delete_pomodoros_for_task(id);
        taskService.delete_task(id, u.get_id());
        return new RedirectView("/tasks");
    }

    @PostMapping("/tasks/{id}/complete")
    public RedirectView completeTask(@PathVariable int id, HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");
        Task task = taskService.try_get_task(id, u.get_id()).orElse(null);
        if (task != null) {
            task.set_status(Status.COMPLETED);
            if (task.get_assigned_user_id() > 0) {
                userService.mark_task_completed_for_user(task.get_assigned_user_id());
            }
        }
        return new RedirectView("/tasks");
    }

    @GetMapping("/users")
    public Object users(HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");

        List<User> allUsers = userService.get_all_users();
        String html = renderer.renderUsersPage(
            u, allUsers, allUsers.size(),
            userService.count_users_by_role(Role.ADMIN),
            userService.count_users_by_role(Role.DEVELOPER),
            userService.count_users_by_role(Role.MANAGER),
            (String) session.getAttribute("flashMessage"),
            (String) session.getAttribute("flashType")
        );
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashType");
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @PostMapping("/users")
    public RedirectView createUser(@RequestParam("name") String name,
                                   @RequestParam("email") String email,
                                   @RequestParam("role") Role role,
                                   HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");
        userService.create_user(name, email, role);
        return new RedirectView("/users");
    }

    @PostMapping("/users/{id}/delete")
    public RedirectView deleteUser(@PathVariable int id, HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");
        userService.delete_user(id);
        return new RedirectView("/users");
    }

    @GetMapping("/sessions")
    public Object sessions(HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");

        String html = renderer.renderSessionsPage(
            u, pomodoroService.get_all_pomodoros(),
            pomodoroService.get_total_sessions(),
            pomodoroService.get_total_focus_time_minutes(),
            pomodoroService.get_active_days_count(),
            pomodoroService.get_average_sessions_per_task(),
            (String) session.getAttribute("flashMessage"),
            (String) session.getAttribute("flashType")
        );
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashType");
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @PostMapping("/sessions/{id}/delete")
    public RedirectView deleteSession(@PathVariable int id, HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");
        pomodoroService.delete_pomodoro(id);
        return new RedirectView("/sessions");
    }

    @GetMapping("/reports")
    public Object reports(HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");

        List<Task> allTasks = taskService.get_all_tasks(u.get_id());
        String html = renderer.renderReportsPage(
            u, allTasks.size(),
            taskService.get_completion_rate(),
            taskService.count_overdue_tasks(),
            countStatus(allTasks, Status.PENDING),
            countStatus(allTasks, Status.IN_PROGRESS),
            countStatus(allTasks, Status.COMPLETED),
            countStatus(allTasks, Status.CANCELLED),
            pomodoroService.get_total_sessions(),
            pomodoroService.get_total_focus_time_minutes(),
            pomodoroService.get_most_productive_task(),
            userService.get_top_performers(5),
            userService.get_average_completion_rate(),
            (String) session.getAttribute("flashMessage"),
            (String) session.getAttribute("flashType")
        );
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashType");
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @GetMapping("/settings")
    public Object settings(HttpSession session) {
        User u = currentUser(session);
        if (u == null) return new RedirectView("/login");

        AppSettings s = settingsService.getSettings();
        Role role = u.get_role();
        String html = renderer.renderSettingsPage(
            u, s, role, visibleCountForRole(role), true,
            (String) session.getAttribute("flashMessage"),
            (String) session.getAttribute("flashType")
        );
        session.removeAttribute("flashMessage");
        session.removeAttribute("flashType");
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(html);
    }

    @PostMapping("/settings/theme")
    public RedirectView setTheme(@RequestParam String theme, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        try {
            settingsService.getSettings().setTheme(AppSettings.Theme.valueOf(theme));
        } catch (IllegalArgumentException e) { /* ignore */ }
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/default-duration")
    public RedirectView setDefaultDuration(@RequestParam Integer minutes, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        if (minutes != null && minutes >= 1 && minutes <= 180) {
            settingsService.getSettings().setDefaultPomodoroDuration(minutes);
            pomodoroService.set_default_duration(minutes);
        }
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/notification-sound")
    public RedirectView setNotificationSound(@RequestParam("enabled") Boolean enabled, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        settingsService.getSettings().setNotificationSound(enabled != null && enabled);
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/auto-start")
    public RedirectView setAutoStart(@RequestParam("enabled") Boolean enabled, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        settingsService.getSettings().setAutoStartNextSession(enabled != null && enabled);
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/show-all-tasks")
    public RedirectView setShowAllTasks(@RequestParam("enabled") Boolean enabled, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        settingsService.getSettings().setShowAllTasks(enabled != null && enabled);
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/daily-focus-goal")
    public RedirectView setDailyFocusGoal(@RequestParam Integer goal, HttpSession session) {
        if (currentUser(session) == null) return new RedirectView("/login");
        if (goal != null && goal > 0) {
            settingsService.getSettings().setDailyFocusGoal(goal);
        }
        return new RedirectView("/settings");
    }

    // ---- Helpers ----

    private int countStatus(List<Task> tasks, Status status) {
        int c = 0;
        for (Task t : tasks) if (t.get_status() == status) c++;
        return c;
    }

    private int visibleCountForRole(Role role) {
        return switch (role) {
            case ADMIN -> 4;
            case DEVELOPER -> 10;
            case MANAGER -> 6;
            default -> 2;
        };
    }
}