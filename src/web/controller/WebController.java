package web.controller;

import services.UserService;
import services.TaskService;
import services.PomodoroService;
import services.CategoryService;
import services.TagService;
import services.SettingsService;
import services.ReportService;
import models.User;
import models.Task;
import models.Role;
import models.Priority;
import models.Status;
import models.Category;
import models.Tag;
import models.Pomodoro;
import models.AppSettings;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class WebController {

    private final UserService userService;
    private final TaskService taskService;
    private final PomodoroService pomodoroService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final SettingsService settingsService;
    private final ReportService reportService;

    public WebController(UserService userService, TaskService taskService,
                         PomodoroService pomodoroService, CategoryService categoryService,
                         TagService tagService, SettingsService settingsService,
                         ReportService reportService) {
        this.userService = userService;
        this.taskService = taskService;
        this.pomodoroService = pomodoroService;
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.settingsService = settingsService;
        this.reportService = reportService;
    }

    // ============ LOGIN / USER SELECTION ============

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("users", userService.get_all_users());
        return "login";
    }

    @PostMapping("/login")
    public String selectUser(@RequestParam(required = false) Integer userId,
                             @RequestParam(required = false) String guest,
                             HttpServletRequest request) {
        HttpSession session = request.getSession();
        User selected = null;
        if (userId != null) {
            Optional<User> u = userService.try_get_user(userId);
            if (u.isPresent()) selected = u.get();
        }
        session.setAttribute("currentUser", selected);
        session.setAttribute("isGuest", selected == null);
        return "redirect:/timer";
    }

    // ============ TIMER / POMODORO (Tab 1) ============

    @GetMapping("/timer")
    public String timer(Model model,
                        @SessionAttribute(name = "currentUser", required = false) User currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isGuest", currentUser == null);

        // Default duration
        int defaultDuration = settingsService.getSettings().getDefaultPomodoroDuration();
        model.addAttribute("defaultDuration", defaultDuration);

        // Available tasks for selection
        List<Task> tasks = taskService.get_all_tasks();
        model.addAttribute("tasks", tasks);

        // Pomodoro stats
        model.addAttribute("totalSessions", pomodoroService.get_total_sessions());
        model.addAttribute("totalFocusMinutes", pomodoroService.get_total_focus_time_minutes());
        model.addAttribute("storedSessions", pomodoroService.get_total_stored_sessions(taskService));
        model.addAttribute("storedFocusMinutes", pomodoroService.get_total_stored_focus_time_minutes(taskService));
        model.addAttribute("activeDays", pomodoroService.get_active_days_count());
        model.addAttribute("mostProductiveTask", pomodoroService.get_most_productive_task());

        // Active pomodoros
        model.addAttribute("activePomodoros", pomodoroService.get_active_pomodoros());

        // Recent sessions
        model.addAttribute("recentSessions", pomodoroService.get_last_sessions(10));

        return "timer";
    }

    @PostMapping("/timer/start")
    public RedirectView startTimer(@RequestParam Integer taskId,
                                   @RequestParam(required = false) Integer durationMinutes,
                                   @SessionAttribute(name = "currentUser", required = false) User currentUser) {
        if (currentUser == null) {
            return new RedirectView("/login");
        }
        Task task = taskService.try_get_task(taskId).orElse(null);
        if (task == null) {
            return new RedirectView("/timer");
        }
        int duration = (durationMinutes != null) ? durationMinutes : settingsService.getSettings().getDefaultPomodoroDuration();
        pomodoroService.create_pomodoro(taskId, task.get_title(), duration);
        pomodoroService.start_session(pomodoroService.count_pomodoros());
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/pause")
    public RedirectView pauseTimer(@RequestParam Integer pomodoroId) {
        pomodoroService.stop_session(pomodoroId);
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/resume")
    public RedirectView resumeTimer(@RequestParam Integer pomodoroId) {
        pomodoroService.start_session(pomodoroId);
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/complete")
    public RedirectView completeTimer(@RequestParam Integer pomodoroId) {
        pomodoroService.increment_session(pomodoroId);
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/reset")
    public RedirectView resetTimer(@RequestParam Integer pomodoroId) {
        pomodoroService.reset_sessions(pomodoroId);
        return new RedirectView("/timer");
    }

    @PostMapping("/timer/delete")
    public RedirectView deleteTimer(@RequestParam Integer pomodoroId) {
        pomodoroService.delete_pomodoro(pomodoroId);
        return new RedirectView("/timer");
    }

    // ============ TASKS (Tab 2) ============

    @GetMapping("/tasks")
    public String tasks(Model model,
                        @SessionAttribute(name = "currentUser", required = false) User currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isGuest", currentUser == null);

        List<Task> allTasks = taskService.get_all_tasks();
        model.addAttribute("tasks", allTasks);
        model.addAttribute("totalTasks", allTasks.size());
        model.addAttribute("completionRate", taskService.get_completion_rate());
        model.addAttribute("overdueTasks", taskService.count_overdue_tasks());

        // Counts by status
        long pending = allTasks.stream().filter(t -> t.get_status() == Status.PENDING).count();
        long inProgress = allTasks.stream().filter(t -> t.get_status() == Status.IN_PROGRESS).count();
        long completed = allTasks.stream().filter(t -> t.get_status() == Status.COMPLETED).count();
        long cancelled = allTasks.stream().filter(t -> t.get_status() == Status.CANCELLED).count();
        model.addAttribute("pendingCount", pending);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("completedCount", completed);
        model.addAttribute("cancelledCount", cancelled);

        model.addAttribute("users", userService.get_all_users());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());
        model.addAttribute("categories", categoryService.get_all_categories());
        model.addAttribute("tags", tagService.get_all_tags());

        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String newTaskForm(Model model) {
        model.addAttribute("task", new Task(0, "", "", Priority.LOW));
        model.addAttribute("users", userService.get_all_users());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("categories", categoryService.get_all_categories());
        model.addAttribute("tags", tagService.get_all_tags());
        return "task-form";
    }

    @PostMapping("/tasks")
    public RedirectView createTask(@ModelAttribute Task task,
                                   @RequestParam(required = false) Integer assignedUserId,
                                   @RequestParam(required = false) Integer categoryId,
                                   @RequestParam(required = false) Boolean storeSessions) {
        Task created = taskService.create_task(task.get_title(), task.get_description(), task.get_priority());
        if (assignedUserId != null && assignedUserId > 0) {
            created.set_assigned_user_id(assignedUserId);
        }
        if (categoryId != null && categoryId > 0) {
            created.set_category_id(categoryId);
        }
        if (storeSessions != null) {
            created.set_store_sessions(storeSessions);
        }
        return new RedirectView("/tasks");
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTask(@PathVariable int id, Model model) {
        Task task = taskService.try_get_task(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        model.addAttribute("task", task);
        model.addAttribute("users", userService.get_all_users());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());
        model.addAttribute("categories", categoryService.get_all_categories());
        model.addAttribute("tags", tagService.get_all_tags());
        return "task-form";
    }

    @PostMapping("/tasks/{id}")
    public RedirectView updateTask(@PathVariable int id, @ModelAttribute Task task,
                                   @RequestParam(required = false) Integer assignedUserId,
                                   @RequestParam(required = false) Integer categoryId,
                                   @RequestParam(required = false) Boolean storeSessions) {
        Task existing = taskService.try_get_task(id).orElse(null);
        if (existing != null) {
            existing.set_title(task.get_title());
            existing.set_description(task.get_description());
            existing.set_priority(task.get_priority());
            existing.set_status(task.get_status());
            if (assignedUserId != null && assignedUserId > 0) {
                existing.set_assigned_user_id(assignedUserId);
            } else {
                existing.set_assigned_user_id(-1);
            }
            if (categoryId != null && categoryId > 0) {
                existing.set_category_id(categoryId);
            } else {
                existing.set_category_id(-1);
            }
            if (storeSessions != null) {
                existing.set_store_sessions(storeSessions);
            }
        }
        return new RedirectView("/tasks");
    }

    @PostMapping("/tasks/{id}/delete")
    public RedirectView deleteTask(@PathVariable int id) {
        pomodoroService.delete_pomodoros_for_task(id);
        // Mark task as cancelled instead of delete (TaskService has no delete)
        Task task = taskService.try_get_task(id).orElse(null);
        if (task != null) {
            task.set_status(Status.CANCELLED);
        }
        return new RedirectView("/tasks");
    }

    @PostMapping("/tasks/{id}/complete")
    public RedirectView completeTask(@PathVariable int id) {
        Task task = taskService.try_get_task(id).orElse(null);
        if (task != null) {
            task.set_status(Status.COMPLETED);
            if (task.get_assigned_user_id() > 0) {
                userService.mark_task_completed_for_user(task.get_assigned_user_id());
            }
        }
        return new RedirectView("/tasks");
    }

    // ============ USERS (Tab 3) ============

    @GetMapping("/users")
    public String users(Model model,
                        @SessionAttribute(name = "currentUser", required = false) User currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isGuest", currentUser == null);

        List<User> allUsers = userService.get_all_users();
        model.addAttribute("users", allUsers);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("adminCount", userService.count_users_by_role(Role.ADMIN));
        model.addAttribute("devCount", userService.count_users_by_role(Role.DEVELOPER));
        model.addAttribute("mgrCount", userService.count_users_by_role(Role.MANAGER));
        model.addAttribute("roles", Role.values());
        return "users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User(0, "", "", Role.ADMIN));
        model.addAttribute("roles", Role.values());
        return "user-form";
    }

    @PostMapping("/users")
    public RedirectView createUser(@ModelAttribute User user) {
        userService.create_user(user.get_name(), user.get_email(), user.get_role());
        return new RedirectView("/users");
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable int id, Model model) {
        User user = userService.try_get_user(id).orElse(null);
        if (user == null) return "redirect:/users";
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "user-form";
    }

    @PostMapping("/users/{id}")
    public RedirectView updateUser(@PathVariable int id, @ModelAttribute User user) {
        userService.update_user_name(id, user.get_name());
        userService.update_user_email(id, user.get_email());
        userService.update_user_role(id, user.get_role());
        return new RedirectView("/users");
    }

    @PostMapping("/users/{id}/delete")
    public RedirectView deleteUser(@PathVariable int id) {
        userService.delete_user(id);
        return new RedirectView("/users");
    }

    @PostMapping("/users/{id}/assign-task")
    public RedirectView assignTask(@PathVariable int id, @RequestParam Integer taskId) {
        userService.assign_task_to_user(id, taskId);
        return new RedirectView("/users");
    }

    @PostMapping("/users/{id}/unassign-task")
    public RedirectView unassignTask(@PathVariable int id, @RequestParam Integer taskId) {
        userService.unassign_task_from_user(id, taskId);
        return new RedirectView("/users");
    }

    // ============ SESSIONS (Tab 4) ============

    @GetMapping("/sessions")
    public String sessions(Model model,
                           @SessionAttribute(name = "currentUser", required = false) User currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isGuest", currentUser == null);

        List<Pomodoro> allPomodoros = pomodoroService.get_all_pomodoros();
        model.addAttribute("pomodoros", allPomodoros);
        model.addAttribute("totalSessions", pomodoroService.get_total_sessions());
        model.addAttribute("totalFocusMinutes", pomodoroService.get_total_focus_time_minutes());
        model.addAttribute("activeDays", pomodoroService.get_active_days_count());
        model.addAttribute("averageSessions", pomodoroService.get_average_sessions_per_task());

        // Sessions with history
        model.addAttribute("pomodorosWithSessions", pomodoroService.get_pomodoros_with_sessions());

        return "sessions";
    }

    @PostMapping("/sessions/{id}/set-duration")
    public RedirectView setSessionDuration(@PathVariable int id, @RequestParam Integer durationMinutes) {
        Pomodoro p = pomodoroService.try_get_pomodoro(id).orElse(null);
        if (p != null && durationMinutes != null && durationMinutes > 0) {
            // Note: Pomodoro model may need a set_duration method
            // For now, we'll handle via the model
        }
        return new RedirectView("/sessions");
    }

    @PostMapping("/sessions/{id}/reset")
    public RedirectView resetSession(@PathVariable int id) {
        pomodoroService.reset_sessions(id);
        return new RedirectView("/sessions");
    }

    @PostMapping("/sessions/{id}/delete")
    public RedirectView deleteSession(@PathVariable int id) {
        pomodoroService.delete_pomodoro(id);
        return new RedirectView("/sessions");
    }

    // ============ REPORTS (Tab 5) ============

    @GetMapping("/reports")
    public String reports(Model model,
                          @SessionAttribute(name = "currentUser", required = false) User currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isGuest", currentUser == null);

        List<Task> allTasks = taskService.get_all_tasks();
        model.addAttribute("totalTasks", allTasks.size());
        model.addAttribute("completionRate", taskService.get_completion_rate());
        model.addAttribute("overdueTasks", taskService.count_overdue_tasks());

        long pending = allTasks.stream().filter(t -> t.get_status() == Status.PENDING).count();
        long inProgress = allTasks.stream().filter(t -> t.get_status() == Status.IN_PROGRESS).count();
        long completed = allTasks.stream().filter(t -> t.get_status() == Status.COMPLETED).count();
        long cancelled = allTasks.stream().filter(t -> t.get_status() == Status.CANCELLED).count();
        model.addAttribute("pendingCount", pending);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("completedCount", completed);
        model.addAttribute("cancelledCount", cancelled);

        model.addAttribute("totalSessions", pomodoroService.get_total_sessions());
        model.addAttribute("totalFocusMinutes", pomodoroService.get_total_focus_time_minutes());
        model.addAttribute("mostProductiveTask", pomodoroService.get_most_productive_task());

        // User stats
        model.addAttribute("topPerformers", userService.get_top_performers(5));
        model.addAttribute("averageCompletionRate", userService.get_average_completion_rate());

        return "reports";
    }

    @GetMapping("/reports/export")
    public RedirectView exportReport() {
        // Generate report text
        String report = reportService.generate_summary_report();
        // In a web app, we'd return this as a downloadable file
        // For now, just redirect back to reports
        return new RedirectView("/reports");
    }

    // ============ SETTINGS (Tab 6) ============

    @GetMapping("/settings")
    public String settings(Model model,
                           @SessionAttribute(name = "currentUser", required = false) User currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isGuest", currentUser == null);

        AppSettings s = settingsService.getSettings();
        model.addAttribute("settings", s);

        Role role = (currentUser != null) ? currentUser.get_role() : null;
        model.addAttribute("role", role);
        model.addAttribute("visibleCount", settingsService.getVisibleCountForRole(role));
        model.addAttribute("canEdit", currentUser != null);

        return "settings";
    }

    @PostMapping("/settings/theme")
    public RedirectView setTheme(@RequestParam String theme) {
        try {
            settingsService.getSettings().setTheme(AppSettings.Theme.valueOf(theme));
        } catch (IllegalArgumentException e) {
            // ignore invalid theme
        }
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/default-duration")
    public RedirectView setDefaultDuration(@RequestParam Integer minutes) {
        if (minutes != null && minutes >= 1 && minutes <= 180) {
            settingsService.getSettings().setDefaultPomodoroDuration(minutes);
            pomodoroService.set_default_duration(minutes);
        }
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/notification-sound")
    public RedirectView setNotificationSound(@RequestParam(required = false) Boolean enabled) {
        settingsService.getSettings().setNotificationSound(enabled != null && enabled);
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/auto-start")
    public RedirectView setAutoStart(@RequestParam(required = false) Boolean enabled) {
        settingsService.getSettings().setAutoStartNextSession(enabled != null && enabled);
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/show-all-tasks")
    public RedirectView setShowAllTasks(@RequestParam(required = false) Boolean enabled) {
        settingsService.getSettings().setShowAllTasks(enabled != null && enabled);
        return new RedirectView("/settings");
    }

    @PostMapping("/settings/daily-focus-goal")
    public RedirectView setDailyFocusGoal(@RequestParam Integer goal) {
        if (goal != null && goal > 0) {
            settingsService.getSettings().setDailyFocusGoal(goal);
        }
        return new RedirectView("/settings");
    }

    // ============ LOGOUT ============

    @GetMapping("/logout")
    public RedirectView logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("currentUser");
            session.removeAttribute("isGuest");
            session.invalidate();
        }
        return new RedirectView("/login");
    }
}
