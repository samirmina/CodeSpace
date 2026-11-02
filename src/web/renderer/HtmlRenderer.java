package web.renderer;

import models.Priority;
import models.Role;
import models.Status;
import models.Task;
import models.User;
import models.Pomodoro;
import services.PomodoroService;
import services.TaskService;
import services.UserService;
import org.springframework.stereotype.Component;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * HtmlRenderer - generates all HTML programmatically with inline CSS.
 * No external HTML or CSS files are used - everything is built in Java.
 */
@Component
public class HtmlRenderer {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String CSS = buildCss();

    private static String buildCss() {
        StringBuilder s = new StringBuilder();
        s.append("<style>");
        s.append("* { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }");
        s.append("body { background: #f5f7fa; color: #333; line-height: 1.6; }");
        s.append(".container { max-width: 1200px; margin: 0 auto; padding: 1rem; }");
        s.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 1.5rem 2rem; border-radius: 12px 12px 0 0; display: flex; justify-content: space-between; align-items: center; }");
        s.append(".header h1 { font-size: 1.75rem; }");
        s.append(".nav { display: flex; gap: 0.5rem; }");
        s.append(".nav a { padding: 0.75rem 1.25rem; background: rgba(255,255,255,0.15); color: white; text-decoration: none; border-radius: 8px; font-size: 0.95rem; transition: all 0.2s; }");
        s.append(".nav a:hover { background: rgba(255,255,255,0.25); }");
        s.append(".nav a.active { background: white; color: #667eea; font-weight: 600; }");
        s.append(".card { background: white; border-radius: 12px; padding: 1.5rem; box-shadow: 0 4px 20px rgba(0,0,0,0.08); margin-bottom: 1.5rem; }");
        s.append(".card h2 { font-size: 1.5rem; color: #333; margin-bottom: 1rem; display: flex; align-items: center; gap: 0.5rem; }");
        s.append(".btn { padding: 0.75rem 1.5rem; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; font-size: 0.95rem; transition: all 0.2s; text-decoration: none; display: inline-block; }");
        s.append(".btn-primary { background: #667eea; color: white; }");
        s.append(".btn-primary:hover { background: #5568d3; }");
        s.append(".btn-success { background: #4caf7a; color: white; }");
        s.append(".btn-success:hover { background: #43a047; }");
        s.append(".btn-warning { background: #f57c00; color: white; }");
        s.append(".btn-warning:hover { background: #ef6c00; }");
        s.append(".btn-danger { background: #c62828; color: white; }");
        s.append(".btn-danger:hover { background: #b71c1c; }");
        s.append(".btn-outline { background: transparent; color: #667eea; border: 2px solid #667eea; }");
        s.append(".btn-outline:hover { background: #667eea; color: white; }");
        s.append(".btn-sm { padding: 0.4rem 0.8rem; font-size: 0.85rem; }");
        s.append(".user-badge { display: inline-block; padding: 0.25rem 0.75rem; background: #e8e8e8; color: #555; border-radius: 20px; font-size: 0.85rem; font-weight: 600; margin-bottom: 1rem; }");
        s.append(".user-badge.guest { background: #c62828; color: white; }");
        s.append(".user-badge.admin { background: #1565c0; color: white; }");
        s.append(".user-badge.developer { background: #2e7d32; color: white; }");
        s.append(".user-badge.manager { background: #f57c00; color: white; }");
        s.append(".form-group { margin-bottom: 1rem; }");
        s.append(".form-group label { display: block; font-weight: 600; color: #555; margin-bottom: 0.4rem; }");
        s.append(".form-control { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 8px; font-size: 0.95rem; }");
        s.append(".form-control:focus { outline: none; border-color: #667eea; }");
        s.append(".table { width: 100%; border-collapse: collapse; }");
        s.append(".table th, .table td { padding: 0.75rem; text-align: left; border-bottom: 1px solid #e8e8e8; }");
        s.append(".table th { color: #667eea; font-size: 0.85rem; text-transform: uppercase; font-weight: 600; }");
        s.append(".table tbody tr:hover { background: #f8f9fa; }");
        s.append(".stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 12px; padding: 1.5rem; text-align: center; }");
        s.append(".stat-card h3 { font-size: 2rem; margin-bottom: 0.25rem; }");
        s.append(".stat-card p { font-size: 0.9rem; opacity: 0.9; }");
        s.append(".stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }");
        s.append(".empty-state { text-align: center; color: #888; padding: 2rem; }");
        s.append(".timer-circle { width: 280px; height: 280px; border-radius: 50%; border: 8px solid #e8e8e8; position: relative; margin: 0 auto 1.5rem; display: flex; align-items: center; justify-content: center; background: #fff; }");
        s.append(".timer-circle::before { content: ''; position: absolute; top: 10px; left: 10px; right: 10px; bottom: 10px; border-radius: 50%; border: 4px solid #667eea; mask-image: conic-gradient(from -90deg, #667eea 0% var(--progress, 0%), transparent var(--progress, 0%) 100%); -webkit-mask-image: conic-gradient(from -90deg, #667eea 0% var(--progress, 0%), transparent var(--progress, 0%) 100%); }");
        s.append(".timer-display { font-size: 3rem; font-weight: bold; color: #333; font-family: monospace; z-index: 10; position: relative; }");
        s.append(".timer-status { text-align: center; color: #666; font-weight: bold; margin-bottom: 1rem; font-size: 1.1rem; }");
        s.append(".timer-controls { display: flex; gap: 0.75rem; justify-content: center; margin-bottom: 1.5rem; flex-wrap: wrap; }");
        s.append(".task-selection { background: #f8f9fa; padding: 1.25rem; border-radius: 8px; margin-bottom: 1.5rem; }");
        s.append(".task-selection label { display: block; font-weight: 600; color: #555; margin-bottom: 0.4rem; }");
        s.append(".task-selection select, .task-selection input { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 8px; font-size: 0.95rem; }");
        s.append(".duration-selector { display: flex; gap: 0.5rem; align-items: center; margin-bottom: 1rem; }");
        s.append(".duration-selector label { font-weight: 600; color: #555; }");
        s.append(".duration-selector input { width: 80px; padding: 0.75rem; border: 1px solid #ddd; border-radius: 8px; }");
        s.append(".duration-selector span { font-size: 0.9rem; color: #666; }");
        s.append(".badge { display: inline-block; padding: 0.25rem 0.6rem; border-radius: 6px; font-size: 0.8rem; font-weight: 600; }");
        s.append(".badge-status-pending { background: #e3f2fd; color: #1565c0; }");
        s.append(".badge-status-in-progress { background: #fff3e0; color: #ef6c00; }");
        s.append(".badge-status-completed { background: #e8f5e9; color: #2e7d32; }");
        s.append(".badge-status-cancelled { background: #ffebee; color: #c62828; }");
        s.append(".badge-priority-low { background: #c8e6c9; color: #2e7d32; }");
        s.append(".badge-priority-medium { background: #fff9c4; color: #f57c00; }");
        s.append(".badge-priority-high { background: #ffccbc; color: #c62828; }");
        s.append(".badge-priority-urgent { background: #ffcdd2; color: #b71c1c; }");
        s.append(".flash-message { padding: 1rem; border-radius: 8px; margin-bottom: 1.5rem; }");
        s.append(".flash-success { background: #e8f5e9; color: #2e7d32; border: 1px solid #a5d6a7; }");
        s.append(".flash-error { background: #ffebee; color: #c62828; border: 1px solid #ef9a9a; }");
        s.append(".flash-info { background: #e3f2fd; color: #1565c0; border: 1px solid #90caf9; }");
        s.append("</style>");
        return s.toString();
    }

    private static void s(StringBuilder sb, String text) {
        sb.append(text);
    }

    public String renderHomePage(User currentUser) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos — Pomodoro &amp; Task Manager</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");

        s(sb, "<nav class=\"header\">\n<h1>&#128325; Chronos</h1>\n<div class=\"nav\">\n");
        s(sb, "<a href=\"/timer\" class=\"active\">Timer</a>\n");
        s(sb, "<a href=\"/tasks\">Tasks</a>\n");
        s(sb, "<a href=\"/users\">Users</a>\n");
        s(sb, "<a href=\"/sessions\">Sessions</a>\n");
        s(sb, "<a href=\"/reports\">Reports</a>\n");
        s(sb, "<a href=\"/settings\">Settings</a>\n");
        s(sb, "<a href=\"/logout\" style=\"background: rgba(255,255,255,0.2);\">Logout</a>\n");
        s(sb, "</div>\n</div>\n");

        if (currentUser == null) {
            s(sb, "<div class=\"card\" style=\"max-width: 700px; margin: 2rem auto;\">\n");
            s(sb, "<h2>Welcome to Chronos</h2>\n");
            s(sb, "<p>Chronos is a Pomodoro &amp; Task Manager built entirely in Java. Track your focus sessions, manage tasks, and view productivity reports — all in a clean, professional interface.</p>\n");
            s(sb, "<h3>Key Features</h3>\n");
            s(sb, "<ul>\n<li><strong>&#128325; Pomodoro Timer</strong> — timed focus sessions with task selection and custom duration</li>\n<li><strong>&#128391; Task Management</strong> — CRUD tasks with priority, status, tags, due dates, and user assignment</li>\n<li><strong>&#128103; Multi-User</strong> — role-based access (Admin, Developer, Manager) with per-user data isolation</li>\n<li><strong>&#128198; Sessions</strong> — track every pomodoro session and view active timers</li>\n<li><strong>&#128197; Reports</strong> — completion rates, top performers, and task status breakdowns</li>\n<li><strong>&#9881; Settings</strong> — theme, default duration, notifications, auto-start, and daily focus goals</li>\n</ul>\n");
            s(sb, "<p>Get started by selecting your profile below.</p>\n");
            s(sb, "<form method=\"post\" action=\"/login\">\n");
            s(sb, "<div class=\"form-group\">\n<label>Select Your Profile</label>\n<select name=\"userId\" class=\"form-control\" required>\n");
            s(sb, "<option value=\"\">-- Select a user --</option>\n");
            s(sb, "<option value=\"0\">Guest (read-only)</option>\n");
            s(sb, "</select>\n</div>\n");
            s(sb, "<button type=\"submit\" class=\"btn btn-primary\" style=\"width: 100%;\">Enter Chronos</button>\n");
            s(sb, "</form>\n");
            s(sb, "</div>\n");
        } else {
            s(sb, "<div class=\"card\" style=\"max-width: 700px; margin: 2rem auto;\">\n");
            s(sb, "<h2>Welcome back, " + escapeHtml(currentUser.get_name()) + "!</h2>\n");
            s(sb, "<p>You're logged in as <strong>" + escapeHtml(currentUser.get_name()) + "</strong> (" + currentUser.get_role() + ").</p>\n");
            s(sb, "<div class=\"stat-grid\">\n");
            s(sb, renderStatCard("Timer", "Start a focus session"));
            s(sb, renderStatCard("Tasks", "Manage your task list"));
            s(sb, renderStatCard("Sessions", "View your pomodoro history"));
            s(sb, renderStatCard("Reports", "See your productivity stats"));
            s(sb, "</div>\n");
            s(sb, "<p>Choose a page from the navigation above to get started.</p>\n");
            s(sb, "</div>\n");
        }

        s(sb, "</div>\n</body>\n</html>");
        return sb.toString();
    }

    public String renderLoginPage(List<User> users, String flashMessage, String flashType) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos - Login</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");

        if (flashMessage != null && !flashMessage.isEmpty()) {
            s(sb, renderFlash(flashMessage, flashType));
        }

        s(sb, "<div class=\"card\" style=\"max-width: 500px; margin: 2rem auto;\">\n");
        s(sb, "<h2 style=\"text-align: center; margin-bottom: 1.5rem;\">&#128274; Chronos - Login</h2>\n");

        if (users == null || users.isEmpty()) {
            s(sb, "<div class=\"empty-state\">No users available. Please add users first.</div>\n");
        } else {
            s(sb, "<form method=\"post\" action=\"/login\">\n");
            s(sb, "<div class=\"form-group\">\n<label>Select Your Profile</label>\n<select name=\"userId\" class=\"form-control\" required>\n");
            s(sb, "<option value=\"\">-- Select a user --</option>\n");
            for (User user : users) {
                String roleBadge = switch (user.get_role()) {
                    case ADMIN -> "badge badge-status-pending";
                    case DEVELOPER -> "badge badge-status-in-progress";
                    case MANAGER -> "badge badge-status-completed";
                    default -> "badge badge-status-cancelled";
                };
                s(sb, "<option value=\"" + user.get_id() + "\">" + escapeHtml(user.get_name()) + " <span class=\"" + roleBadge + "\">" + user.get_role() + "</span></option>\n");
            }
            s(sb, "</select>\n</div>\n");
            s(sb, "<button type=\"submit\" class=\"btn btn-primary\" style=\"width: 100%;\">Enter Chronos</button>\n");
            s(sb, "</form>\n");
        }

        s(sb, "</div>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    public String renderTimerPage(User currentUser, List<Task> tasks, int defaultDuration,
                                  int totalSessions, int totalFocusMinutes, int storedSessions,
                                  int storedFocusMinutes, int activeDays, String mostProductiveTask,
                                  List<Pomodoro> activePomodoros,
                                  List<models.RecentSessionDTO> recentSessions,
                                  String flashMessage, String flashType) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos - Timer</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");

        if (flashMessage != null && !flashMessage.isEmpty()) {
            s(sb, renderFlash(flashMessage, flashType));
        }

        s(sb, renderNav(currentUser));
        s(sb, renderUserBadge(currentUser));

        s(sb, "<div class=\"card\">\n<h2>&#128325; Pomodoro Timer</h2>\n");
        s(sb, "<div class=\"timer-circle\" style=\"--progress: 50%;\">\n");
        s(sb, "<div class=\"timer-display\" id=\"timerDisplay\">25:00</div>\n");
        s(sb, "</div>\n");
        s(sb, "<div class=\"timer-status\" id=\"timerStatus\">Select a task to begin</div>\n");
        s(sb, "<div class=\"task-selection\">\n");
        s(sb, "<label for=\"taskSelect\">Select Task</label>\n<select id=\"taskSelect\" class=\"form-control\">\n");
        s(sb, "<option value=\"\">-- No Task --</option>\n");
        if (tasks != null) {
            for (Task task : tasks) {
                s(sb, "<option value=\"" + task.get_id() + "\">" + escapeHtml(task.get_title()) + " [" + task.get_priority() + "]</option>\n");
            }
        }
        s(sb, "</select>\n</div>\n");
        s(sb, "<div class=\"duration-selector\">\n");
        s(sb, "<label for=\"durationInput\">Duration (min)</label>\n");
        s(sb, "<input type=\"number\" id=\"durationInput\" class=\"form-control\" value=\"" + defaultDuration + "\" min=\"1\" max=\"180\"/>\n");
        s(sb, "<span>minutes</span>\n");
        s(sb, "</div>\n");
        s(sb, "<div class=\"timer-controls\">\n");
        s(sb, "<button class=\"btn btn-success btn-sm\" onclick=\"startTimer()\">Start</button>\n");
        s(sb, "<button class=\"btn btn-warning btn-sm\" onclick=\"pauseTimer()\" id=\"btnPause\" disabled>Pause</button>\n");
        s(sb, "<button class=\"btn btn-success btn-sm\" onclick=\"resumeTimer()\" id=\"btnResume\" disabled>Resume</button>\n");
        s(sb, "<button class=\"btn btn-danger btn-sm\" onclick=\"completeTimer()\" id=\"btnComplete\" disabled>Complete</button>\n");
        s(sb, "<button class=\"btn btn-outline btn-sm\" onclick=\"resetTimer()\" id=\"btnReset\" disabled>Reset</button>\n");
        s(sb, "</div>\n");
        s(sb, "</div>\n");

        // Stats
        s(sb, "<div class=\"stat-grid\">\n");
        s(sb, renderStatCard("Total Sessions", String.valueOf(totalSessions)));
        s(sb, renderStatCard("Focus Minutes", String.valueOf(totalFocusMinutes)));
        s(sb, renderStatCard("Stored Sessions", String.valueOf(storedSessions)));
        s(sb, renderStatCard("Active Days", String.valueOf(activeDays)));
        s(sb, "</div>\n");

        if (mostProductiveTask != null && !mostProductiveTask.isEmpty()) {
            s(sb, "<div class=\"card\" style=\"margin-top: 1rem;\">\n");
            s(sb, "<strong>&#127943; Most Productive Task:</strong> " + escapeHtml(mostProductiveTask) + "\n");
            s(sb, "</div>\n");
        }

        if (activePomodoros != null && !activePomodoros.isEmpty()) {
            s(sb, "<div class=\"card\" style=\"margin-top: 1rem;\">\n<h2>&#9201; Active Sessions</h2>\n");
            for (Pomodoro p : activePomodoros) {
                s(sb, "<div style=\"display: flex; justify-content: space-between; padding: 0.75rem; border-bottom: 1px solid #e8e8e8;\">\n");
                s(sb, "<span>" + escapeHtml(p.get_task_name()) + "</span>\n");
                s(sb, "<button class=\"btn btn-danger btn-sm\">Pause</button>\n");
                s(sb, "</div>\n");
            }
            s(sb, "</div>\n");
        }

        if (recentSessions != null && !recentSessions.isEmpty()) {
            s(sb, "<div class=\"card\" style=\"margin-top: 1rem;\">\n<h2>&#128198; Recent Sessions</h2>\n");
            s(sb, "<table class=\"table\">\n<thead>\n<tr><th>Task</th><th>Duration (min)</th><th>Date</th></tr>\n</thead>\n<tbody>\n");
            for (models.RecentSessionDTO session : recentSessions) {
                s(sb, "<tr>\n");
                s(sb, "<td>" + escapeHtml(session.getTaskName()) + "</td>\n");
                s(sb, "<td>" + session.getDurationMinutes() + "</td>\n");
                s(sb, "<td>" + (session.getTimestamp() != null ? session.getTimestamp().format(TS_FMT) : "-") + "</td>\n");
                s(sb, "</tr>\n");
            }
            s(sb, "</tbody>\n</table>\n</div>\n");
        } else {
            s(sb, "<div class=\"empty-state\" style=\"margin-top: 1rem;\">No recent sessions.</div>\n");
        }

        s(sb, "</div>\n</div>\n");
        s(sb, "<script>\n");
        s(sb, "function startTimer() {\n");
        s(sb, "  const taskId = document.getElementById('taskSelect').value;\n");
        s(sb, "  const duration = document.getElementById('durationInput').value;\n");
        s(sb, "  if (!taskId) { alert('Please select a task'); return; }\n");
        s(sb, "  fetch('/timer/start?taskId=' + taskId + '&durationMinutes=' + duration, { method: 'POST' }).then(() => location.reload());\n");
        s(sb, "}\n");
        s(sb, "function pauseTimer() { fetch('/timer/pause', { method: 'POST' }).then(() => location.reload()); }\n");
        s(sb, "function resumeTimer() { fetch('/timer/resume', { method: 'POST' }).then(() => location.reload()); }\n");
        s(sb, "function completeTimer() { fetch('/timer/complete', { method: 'POST' }).then(() => location.reload()); }\n");
        s(sb, "function resetTimer() { fetch('/timer/reset', { method: 'POST' }).then(() => location.reload()); }\n");
        s(sb, "</script>\n");
        s(sb, "</body>\n</html>");
        return sb.toString();
    }

    public String renderTasksPage(User currentUser, List<Task> tasks, int totalTasks, double completionRate,
                                  int overdueTasks, long pendingCount, long inProgressCount,
                                  long completedCount, long cancelledCount,
                                  List<User> allUsers, List<Status> statuses, List<Priority> priorities,
                                  String flashMessage, String flashType) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos - Tasks</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");

        if (flashMessage != null && !flashMessage.isEmpty()) {
            s(sb, renderFlash(flashMessage, flashType));
        }

        s(sb, renderNav(currentUser));
        s(sb, renderUserBadge(currentUser));

        s(sb, "<div class=\"card\">\n<h2>&#128391; Tasks</h2>\n");
        s(sb, "<div class=\"stat-grid\">\n");
        s(sb, renderStatCard("Total Tasks", String.valueOf(totalTasks)));
        s(sb, renderStatCard("Completion Rate", String.format("%.1f%%", completionRate)));
        s(sb, renderStatCard("Overdue Tasks", String.valueOf(overdueTasks)));
        s(sb, renderStatCard("Pending", String.valueOf(pendingCount)));
        s(sb, renderStatCard("In Progress", String.valueOf(inProgressCount)));
        s(sb, renderStatCard("Completed", String.valueOf(completedCount)));
        s(sb, renderStatCard("Cancelled", String.valueOf(cancelledCount)));
        s(sb, "</div>\n");

        // New task form
        s(sb, "<div style=\"margin-bottom: 1.5rem;\"><h3>Add New Task</h3>\n<form method=\"post\" action=\"/tasks\">\n");
        s(sb, "<div class=\"form-group\"><label>Task Title</label><input type=\"text\" name=\"title\" class=\"form-control\" required/></div>\n");
        s(sb, "<div class=\"form-group\"><label>Description</label><textarea name=\"description\" class=\"form-control\" rows=\"3\"></textarea></div>\n");
        s(sb, "<div class=\"form-group\"><label>Priority</label><select name=\"priority\" class=\"form-control\">\n");
        if (priorities != null) {
            for (Priority p : priorities) {
                s(sb, "<option value=\"" + p + "\">" + p + "</option>\n");
            }
        }
        s(sb, "</select>\n</div>\n");
        s(sb, "<button type=\"submit\" class=\"btn btn-primary\">Create Task</button>\n");
        s(sb, "</form>\n</div>\n");

        // Task table
        s(sb, "<table class=\"table\">\n<thead>\n<tr><th>Title</th><th>Priority</th><th>Status</th><th>Assigned User</th><th>Actions</th></tr>\n</thead>\n<tbody>\n");
        if (tasks != null) {
            for (Task task : tasks) {
                s(sb, "<tr>\n");
                s(sb, "<td>" + escapeHtml(task.get_title()) + "</td>\n");
                s(sb, "<td><span class=\"badge badge-priority-" + task.get_priority().name().toLowerCase() + "\">" + task.get_priority() + "</span></td>\n");
                s(sb, "<td><span class=\"badge badge-status-" + task.get_status().name().toLowerCase() + "\">" + task.get_status() + "</span></td>\n");
                s(sb, "<td>" + (task.get_assigned_user_id() > 0 ? "User #" + task.get_assigned_user_id() : "Unassigned") + "</td>\n");
                s(sb, "<td>\n");
                s(sb, "<form method=\"post\" action=\"/tasks/");
                sb.append(task.get_id());
                s(sb, "/complete\" style=\"display:inline;\"><button class=\"btn btn-success btn-sm\">Complete</button></form> ");
                sb.append("<form method=\"post\" action=\"/tasks/");
                sb.append(task.get_id());
                s(sb, "/delete\" style=\"display:inline;\" onsubmit=\"return confirm('Delete this task?');\"><button class=\"btn btn-danger btn-sm\">Delete</button></form>\n");
                s(sb, "</td>\n");
                s(sb, "</tr>\n");
            }
        }
        s(sb, "</tbody>\n</table>\n");
        if (tasks == null || tasks.isEmpty()) {
            s(sb, "<div class=\"empty-state\">No tasks found.</div>\n");
        }
        s(sb, "</div>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    public String renderUsersPage(User currentUser, List<User> users, int totalUsers,
                                  int adminCount, int devCount, int mgrCount,
                                  String flashMessage, String flashType) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos - Users</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");

        if (flashMessage != null && !flashMessage.isEmpty()) {
            s(sb, renderFlash(flashMessage, flashType));
        }

        s(sb, renderNav(currentUser));
        s(sb, renderUserBadge(currentUser));
        s(sb, "<div class=\"card\">\n<h2>&#128103; Users</h2>\n");
        s(sb, "<div class=\"stat-grid\">\n");
        s(sb, renderStatCard("Total Users", String.valueOf(totalUsers)));
        s(sb, renderStatCard("Admins", String.valueOf(adminCount)));
        s(sb, renderStatCard("Developers", String.valueOf(devCount)));
        s(sb, renderStatCard("Managers", String.valueOf(mgrCount)));
        s(sb, "</div>\n");

        // Add user form
        s(sb, "<div style=\"margin-bottom: 1.5rem;\"><h3>Add New User</h3>\n<form method=\"post\" action=\"/users\">\n");
        s(sb, "<div class=\"form-group\"><label>Name</label><input type=\"text\" name=\"name\" class=\"form-control\" required/></div>\n");
        s(sb, "<div class=\"form-group\"><label>Email</label><input type=\"email\" name=\"email\" class=\"form-control\" required/></div>\n");
        s(sb, "<div class=\"form-group\"><label>Role</label><select name=\"role\" class=\"form-control\">\n");
        s(sb, "<option value=\"ADMIN\">Admin</option>\n");
        s(sb, "<option value=\"DEVELOPER\">Developer</option>\n");
        s(sb, "<option value=\"MANAGER\">Manager</option>\n");
        s(sb, "</select></div>\n");
        s(sb, "<button type=\"submit\" class=\"btn btn-primary\">Create User</button>\n");
        s(sb, "</form>\n</div>\n");

        // User table
        s(sb, "<table class=\"table\">\n<thead>\n<tr><th>Name</th><th>Email</th><th>Role</th><th>Actions</th></tr>\n</thead>\n<tbody>\n");
        if (users != null) {
            for (User user : users) {
                s(sb, "<tr>\n");
                s(sb, "<td>" + escapeHtml(user.get_name()) + "</td>\n");
                s(sb, "<td>" + escapeHtml(user.get_email()) + "</td>\n");
                s(sb, "<td><span class=\"badge badge-status-" + user.get_role().name().toLowerCase() + "\">" + user.get_role() + "</span></td>\n");
                s(sb, "<td>\n");
                s(sb, "<form method=\"post\" action=\"/users/");
                sb.append(user.get_id());
                s(sb, "/delete\" style=\"display:inline;\" onsubmit=\"return confirm('Delete this user?');\"><button class=\"btn btn-danger btn-sm\">Delete</button></form>\n");
                s(sb, "</td>\n");
                s(sb, "</tr>\n");
            }
        }
        s(sb, "</tbody>\n</table>\n");
        if (users == null || users.isEmpty()) {
            s(sb, "<div class=\"empty-state\">No users found.</div>\n");
        }
        s(sb, "</div>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    public String renderSessionsPage(User currentUser, List<Pomodoro> pomodoros,
                                     int totalSessions, int totalFocusMinutes, int activeDays,
                                     double averageSessions,
                                     String flashMessage, String flashType) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos - Sessions</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");

        if (flashMessage != null && !flashMessage.isEmpty()) {
            s(sb, renderFlash(flashMessage, flashType));
        }

        s(sb, renderNav(currentUser));
        s(sb, renderUserBadge(currentUser));
        s(sb, "<div class=\"card\">\n<h2>&#128198; Sessions</h2>\n");
        s(sb, "<div class=\"stat-grid\">\n");
        s(sb, renderStatCard("Total Sessions", String.valueOf(totalSessions)));
        s(sb, renderStatCard("Focus Minutes", String.valueOf(totalFocusMinutes)));
        s(sb, renderStatCard("Active Days", String.valueOf(activeDays)));
        s(sb, renderStatCard("Avg Sessions/Task", String.format("%.1f", averageSessions)));
        s(sb, "</div>\n");

        s(sb, "<table class=\"table\">\n<thead>\n<tr><th>Task</th><th>Duration</th><th>Sessions</th><th>Status</th><th>Actions</th></tr>\n</thead>\n<tbody>\n");
        if (pomodoros != null) {
            for (Pomodoro p : pomodoros) {
                s(sb, "<tr>\n");
                s(sb, "<td>" + escapeHtml(p.get_task_name()) + "</td>\n");
                s(sb, "<td>" + p.get_duration_minutes() + " min</td>\n");
                s(sb, "<td>" + p.get_completed_sessions() + "</td>\n");
                s(sb, "<td><span class=\"badge " + (p.is_active() ? "badge-status-in-progress" : "badge-status-completed") + "\">" + (p.is_active() ? "Active" : "Stopped") + "</span></td>\n");
                s(sb, "<td>\n");
                s(sb, "<form method=\"post\" action=\"/sessions/");
                sb.append(p.get_id());
                s(sb, "/reset\" style=\"display:inline;\"><button class=\"btn btn-outline btn-sm\">Reset</button></form> ");
                sb.append("<form method=\"post\" action=\"/sessions/");
                sb.append(p.get_id());
                s(sb, "/delete\" style=\"display:inline;\" onsubmit=\"return confirm('Delete this session?');\"><button class=\"btn btn-danger btn-sm\">Delete</button></form>\n");
                s(sb, "</td>\n");
                s(sb, "</tr>\n");
            }
        }
        s(sb, "</tbody>\n</table>\n");
        if (pomodoros == null || pomodoros.isEmpty()) {
            s(sb, "<div class=\"empty-state\">No sessions found.</div>\n");
        }
        s(sb, "</div>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    public String renderReportsPage(User currentUser, int totalTasks, double completionRate,
                                    int overdueTasks, long pendingCount, long inProgressCount,
                                    long completedCount, long cancelledCount,
                                    int totalSessions, int totalFocusMinutes, String mostProductiveTask,
                                    List<User> topPerformers, double averageCompletionRate,
                                    String flashMessage, String flashType) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos - Reports</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");

        if (flashMessage != null && !flashMessage.isEmpty()) {
            s(sb, renderFlash(flashMessage, flashType));
        }

        s(sb, renderNav(currentUser));
        s(sb, renderUserBadge(currentUser));
        s(sb, "<div class=\"card\">\n<h2>&#128197; Reports</h2>\n");
        s(sb, "<div class=\"stat-grid\">\n");
        s(sb, renderStatCard("Total Tasks", String.valueOf(totalTasks)));
        s(sb, renderStatCard("Completion Rate", String.format("%.1f%%", completionRate)));
        s(sb, renderStatCard("Overdue Tasks", String.valueOf(overdueTasks)));
        s(sb, renderStatCard("Total Sessions", String.valueOf(totalSessions)));
        s(sb, renderStatCard("Focus Minutes", String.valueOf(totalFocusMinutes)));
        s(sb, "</div>\n");

        if (mostProductiveTask != null && !mostProductiveTask.isEmpty()) {
            s(sb, "<div class=\"card\" style=\"margin-top: 1rem;\">\n");
            s(sb, "<strong>&#127943; Most Productive Task:</strong> " + escapeHtml(mostProductiveTask) + "\n");
            s(sb, "</div>\n");
        }

        s(sb, "<div class=\"card\" style=\"margin-top: 1rem;\">\n<h2>&#127943; Top Performers</h2>\n");
        s(sb, "<table class=\"table\">\n<thead>\n<tr><th>#</th><th>User</th><th>Role</th><th>Tasks Completed</th></tr>\n</thead>\n<tbody>\n");
        int rank = 1;
        if (topPerformers != null) {
            for (User user : topPerformers) {
                s(sb, "<tr>\n");
                s(sb, "<td>" + rank++ + "</td>\n");
                s(sb, "<td>" + escapeHtml(user.get_name()) + "</td>\n");
                s(sb, "<td><span class=\"badge badge-status-" + user.get_role().name().toLowerCase() + "\">" + user.get_role() + "</span></td>\n");
                s(sb, "<td>" + user.get_tasks_completed_count() + "</td>\n");
                s(sb, "</tr>\n");
            }
        }
        s(sb, "</tbody>\n</table>\n");
        s(sb, "<p>Average Completion Rate: <strong>" + String.format("%.1f%%", averageCompletionRate) + "</strong></p>\n");
        s(sb, "</div>\n");

        s(sb, "<div class=\"card\" style=\"margin-top: 1rem;\">\n<h2>&#128198; Task Status Breakdown</h2>\n");
        s(sb, "<table class=\"table\">\n<tbody>\n");
        s(sb, "<tr><td>Pending</td><td>" + pendingCount + "</td></tr>\n");
        s(sb, "<tr><td>In Progress</td><td>" + inProgressCount + "</td></tr>\n");
        s(sb, "<tr><td>Completed</td><td>" + completedCount + "</td></tr>\n");
        s(sb, "<tr><td>Cancelled</td><td>" + cancelledCount + "</td></tr>\n");
        s(sb, "</tbody>\n</table>\n</div>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    public String renderSettingsPage(User currentUser, models.AppSettings settings,
                                     Role role, int visibleCount, boolean canEdit,
                                     String flashMessage, String flashType) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos - Settings</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");

        if (flashMessage != null && !flashMessage.isEmpty()) {
            s(sb, renderFlash(flashMessage, flashType));
        }

        s(sb, renderNav(currentUser));
        s(sb, renderUserBadge(currentUser));
        s(sb, "<div class=\"card\">\n<h2>&#9881; Settings</h2>\n");
        s(sb, "<div class=\"form-group\"><label>Theme</label>\n<select name=\"theme\" class=\"form-control\" onchange=\"this.form.submit()\">\n");
        s(sb, "<option value=\"LIGHT\" " + (settings.getTheme() == models.AppSettings.Theme.LIGHT ? "selected" : "") + ">Light</option>\n");
        s(sb, "<option value=\"DARK\" " + (settings.getTheme() == models.AppSettings.Theme.DARK ? "selected" : "") + ">Dark</option>\n");
        s(sb, "</select></div>\n");
        s(sb, "<form method=\"post\" action=\"/settings/theme\">\n<input type=\"hidden\" name=\"theme\" value=\"\"/>\n</form>\n");

        s(sb, "<div class=\"form-group\"><label>Default Pomodoro Duration (minutes)</label><input type=\"number\" name=\"minutes\" class=\"form-control\" value=\"" + settings.getDefaultPomodoroDuration() + "\" min=\"1\" max=\"180\"/></div>\n");
        s(sb, "<form method=\"post\" action=\"/settings/default-duration\">\n<input type=\"hidden\" name=\"minutes\" value=\"\"/>\n<button type=\"submit\" class=\"btn btn-primary btn-sm\">Update Duration</button>\n</form>\n");

        s(sb, "<div class=\"form-group\"><label>Notification Sound</label><label style=\"margin-left: 0.5rem;\"><input type=\"checkbox\" name=\"enabled\" " + (settings.isNotificationSound() ? "checked" : "") + " onchange=\"this.form.submit()\"> Enabled</label></div>\n");
        s(sb, "<form method=\"post\" action=\"/settings/notification-sound\">\n<input type=\"hidden\" name=\"enabled\" value=\"\"/>\n</form>\n");

        s(sb, "<div class=\"form-group\"><label>Auto-start Next Session</label><label style=\"margin-left: 0.5rem;\"><input type=\"checkbox\" name=\"enabled\" " + (settings.isAutoStartNextSession() ? "checked" : "") + " onchange=\"this.form.submit()\"> Enabled</label></div>\n");
        s(sb, "<form method=\"post\" action=\"/settings/auto-start\">\n<input type=\"hidden\" name=\"enabled\" value=\"\"/>\n</form>\n");

        s(sb, "<div class=\"form-group\"><label>Show All Tasks</label><label style=\"margin-left: 0.5rem;\"><input type=\"checkbox\" name=\"enabled\" " + (settings.isShowAllTasks() ? "checked" : "") + " onchange=\"this.form.submit()\"> Enabled</label></div>\n");
        s(sb, "<form method=\"post\" action=\"/settings/show-all-tasks\">\n<input type=\"hidden\" name=\"enabled\" value=\"\"/>\n</form>\n");

        s(sb, "<div class=\"form-group\"><label>Daily Focus Goal (minutes)</label><input type=\"number\" name=\"goal\" class=\"form-control\" value=\"" + settings.getDailyFocusGoal() + "\" min=\"1\"/></div>\n");
        s(sb, "<form method=\"post\" action=\"/settings/daily-focus-goal\">\n<input type=\"hidden\" name=\"goal\" value=\"\"/>\n<button type=\"submit\" class=\"btn btn-primary btn-sm\">Update Goal</button>\n</form>\n");

        s(sb, "<div style=\"margin-top: 1rem;\"><p><strong>Role:</strong> " + role + "</p>\n");
        s(sb, "<p><strong>Visible Settings Count:</strong> " + visibleCount + "</p>\n");
        s(sb, "<p><strong>Can Edit:</strong> " + canEdit + "</p>\n");
        s(sb, "</div>\n</div>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    public String renderErrorPage(String errorMessage, String stackTrace) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>Chronos - Error</title>\n");
        s(sb, CSS);
        sb.append("</head>\n<body>\n<div class=\"container\">\n");
        s(sb, "<div class=\"card\" style=\"max-width: 800px; margin: 2rem auto;\">\n");
        s(sb, "<h2 style=\"color: #c62828;\">&#9888; Error</h2>\n");
        s(sb, "<div class=\"flash-error\">" + escapeHtml(errorMessage) + "</div>\n");
        if (stackTrace != null && !stackTrace.isEmpty()) {
            s(sb, "<details><summary>Stack Trace</summary><pre style=\"background: #f8f9fa; padding: 1rem; border-radius: 8px; overflow-x: auto;\">" + escapeHtml(stackTrace) + "</pre></details>\n");
        }
        s(sb, "<a href=\"/login\" class=\"btn btn-primary\">Back to Login</a>\n");
        s(sb, "</div>\n</div>\n</body>\n</html>");
        return sb.toString();
    }

    // ---- Helpers ----

    private String renderNav(User currentUser) {
        StringBuilder sb = new StringBuilder();
        sb.append("<nav class=\"header\">\n<h1>&#128325; Chronos</h1>\n<div class=\"nav\">\n");
        s(sb, "<a href=\"/timer\" class=\"active\">Timer</a>\n");
        s(sb, "<a href=\"/tasks\">Tasks</a>\n");
        s(sb, "<a href=\"/users\">Users</a>\n");
        s(sb, "<a href=\"/sessions\">Sessions</a>\n");
        s(sb, "<a href=\"/reports\">Reports</a>\n");
        s(sb, "<a href=\"/settings\">Settings</a>\n");
        s(sb, "<a href=\"/logout\" style=\"background: rgba(255,255,255,0.2);\">Logout</a>\n");
        s(sb, "</div>\n</div>\n");
        return sb.toString();
    }

    private String renderUserBadge(User currentUser) {
        if (currentUser == null) {
            return "<div class=\"user-badge guest\">Guest Mode</div>\n";
        }
        String badgeClass = switch (currentUser.get_role()) {
            case ADMIN -> "user-badge admin";
            case DEVELOPER -> "user-badge developer";
            case MANAGER -> "user-badge manager";
            default -> "user-badge";
        };
        return "<div class=\"" + badgeClass + "\">User: " + escapeHtml(currentUser.get_name()) + " (" + currentUser.get_role() + ")</div>\n";
    }

    private String renderStatCard(String label, String value) {
        return "<div class=\"stat-card\">\n<h3>" + escapeHtml(value) + "</h3>\n<p>" + escapeHtml(label) + "</p>\n</div>\n";
    }

    private String renderFlash(String message, String type) {
        String cssClass = switch (type) {
            case "error" -> "flash-error";
            case "success" -> "flash-success";
            default -> "flash-info";
        };
        return "<div class=\"flash-message " + cssClass + "\">" + escapeHtml(message) + "</div>\n";
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#039;");
    }

    // Inner data classes for template data
    private static class MapTaskInfo {
        final int id;
        final String taskName;
        final int durationMinutes;
        final int completedSessions;
        final boolean active;
        MapTaskInfo(int id, String taskName, int durationMinutes, int completedSessions, boolean active) {
            this.id = id; this.taskName = taskName; this.durationMinutes = durationMinutes;
            this.completedSessions = completedSessions; this.active = active;
        }
    }

    private static class MapSessionInfo {
        final int id;
        final String taskName;
        final int durationMinutes;
        final int completedSessions;
        final boolean active;
        final String timestamp;
        final Integer sessionDuration;
        MapSessionInfo(int id, String taskName) {
            this(id, taskName, 0, 0, false, null, null);
        }
        MapSessionInfo(int id, String taskName, int durationMinutes, int completedSessions, boolean active,
                       String timestamp, Integer sessionDuration) {
            this.id = id; this.taskName = taskName; this.durationMinutes = durationMinutes;
            this.completedSessions = completedSessions; this.active = active;
            this.timestamp = timestamp; this.sessionDuration = sessionDuration;
        }
    }
}