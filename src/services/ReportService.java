package services;

import models.Priority;
import models.Status;
import models.Task;
import models.User;
import utils.FileUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.stereotype.Service;

/**
 * ReportService - generates various reports and statistics
 * Provides methods to create summary, performance, and export reports
 */
@Service
public class ReportService {
    private final TaskService taskService;
    private final UserService userService;
    private final PomodoroService pomodoroService;

    // Create report service without pomodoro - for basic reporting
    public ReportService(TaskService taskService, UserService userService) {
        this.taskService = taskService;
        this.userService = userService;
        this.pomodoroService = null;
    }

    // Create report service with pomodoro - enables full analytics
    public ReportService(TaskService taskService, UserService userService, PomodoroService pomodoroService) {
        this.taskService = taskService;
        this.userService = userService;
        this.pomodoroService = pomodoroService;
    }

    // Generate summary report - creates overview of all tasks, users, and metrics
    public String generate_summary_report() {
        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════\n");
        report.append("     TASK MANAGEMENT SYSTEM REPORT\n");
        report.append("═══════════════════════════════════════════\n");
        report.append("Generated: ")
            .append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("\n\n");

        // Task statistics section
        List<Task> allTasks = taskService.get_all_tasks();
        report.append("📊 TASK STATISTICS\n");
        report.append("───────────────────────────────────────────\n");
        report.append("Total Tasks: ").append(allTasks.size()).append("\n");
        report.append("Completion Rate: ").append(String.format("%.1f%%", taskService.get_completion_rate())).append("\n");
        report.append("Overdue Tasks: ").append(taskService.count_overdue_tasks()).append("\n\n");

        // Count by status
        report.append("By Status:\n");
        Map<String, Integer> statusCount = count_tasks_by_status(allTasks);
        for (Map.Entry<String, Integer> entry : statusCount.entrySet()) {
            report.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Count by priority
        report.append("\nBy Priority:\n");
        Map<String, Integer> priorityCount = count_tasks_by_priority(allTasks);
        for (Map.Entry<String, Integer> entry : priorityCount.entrySet()) {
            report.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // User statistics section
        List<User> allUsers = userService.get_all_users();
        report.append("\n👥 USER STATISTICS\n");
        report.append("───────────────────────────────────────────\n");
        report.append("Total Users: ").append(allUsers.size()).append("\n");
        report.append("Average Completion Rate: ").append(String.format("%.1f%%", userService.get_average_completion_rate())).append("\n\n");

        Map<String, Integer> roleCount = count_users_by_role(allUsers);
        for (Map.Entry<String, Integer> entry : roleCount.entrySet()) {
            report.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }

        // Overdue tasks section
        report.append("\n⚠️  OVERDUE TASKS\n");
        report.append("───────────────────────────────────────────\n");
        List<Task> overdue = taskService.filter_overdue_tasks();
        if (overdue.isEmpty()) {
            report.append("  No overdue tasks! ✓\n");
        } else {
            for (Task task : overdue) {
                report.append("  • ").append(task.get_title())
                    .append(" (Due: ").append(task.get_due_date().toLocalDate()).append(")\n");
            }
        }

        // Urgent tasks section
        report.append("\n🔥 URGENT TASKS\n");
        report.append("───────────────────────────────────────────\n");
        List<Task> urgent = taskService.get_urgent_incomplete_tasks();
        if (urgent.isEmpty()) {
            report.append("  No urgent tasks! ✓\n");
        } else {
            for (Task task : urgent) {
                report.append("  • ").append(task.get_title())
                    .append(" [").append(task.get_priority()).append("]\n");
            }
        }

        // Pomodoro statistics section
        if (pomodoroService != null) {
            report.append("\n🍅 POMODORO STATISTICS\n");
            report.append("───────────────────────────────────────────\n");
            report.append("Total Sessions: ").append(pomodoroService.get_total_sessions()).append("\n");
            report.append("Total Focus Time: ").append(pomodoroService.get_total_focus_time_minutes()).append(" minutes\n");
            report.append("Average Sessions/Task: ")
                .append(String.format("%.1f", pomodoroService.get_average_sessions_per_task())).append("\n");
            
            String mostProductive = pomodoroService.get_most_productive_task();
            if (mostProductive != null) {
                report.append("Most Productive Task: ").append(mostProductive).append("\n");
            }
        }

        // Top performers section
        report.append("\n🏆 TOP PERFORMERS\n");
        report.append("───────────────────────────────────────────\n");
        List<User> topPerformers = userService.get_top_performers(5);
        for (int i = 0; i < topPerformers.size(); i++) {
            User user = topPerformers.get(i);
            report.append("  ").append(i + 1).append(". ").append(user.get_name())
                .append(" - ").append(user.get_tasks_completed_count()).append(" tasks completed\n");
        }

        report.append("\n═══════════════════════════════════════════\n");
        return report.toString();
    }

    // Generate user performance report - shows each user's productivity metrics
    public String generate_user_performance_report() {
        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════\n");
        report.append("     USER PERFORMANCE REPORT\n");
        report.append("═══════════════════════════════════════════\n");
        report.append("Generated: ")
            .append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .append("\n\n");

        List<User> users = userService.get_all_users();
        for (User user : users) {
            report.append("User: ").append(user.get_name()).append(" (").append(user.get_role()).append(")\n");
            report.append("  Email: ").append(user.get_email()).append("\n");
            report.append("  Tasks Assigned: ").append(user.count_assigned_tasks()).append("\n");
            report.append("  Tasks Completed: ").append(user.get_tasks_completed_count()).append("\n");
            report.append("  Completion Rate: ").append(String.format("%.1f%%", user.get_completion_rate())).append("\n");
            report.append("  Member Since: ").append(user.get_created_at().toLocalDate()).append("\n");
            report.append("  Last Active: ").append(user.get_last_active_at().toLocalDate()).append("\n");
            report.append("\n");
        }

        report.append("═══════════════════════════════════════════\n");
        return report.toString();
    }

    // Generate priority breakdown report - shows distribution of task priorities
    public String generate_priority_breakdown_report() {
        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════\n");
        report.append("     TASK PRIORITY BREAKDOWN\n");
        report.append("═══════════════════════════════════════════\n");

        List<Task> allTasks = taskService.get_all_tasks();
        int total = allTasks.size();
        
        if (total == 0) {
            report.append("No tasks to report\n");
            return report.toString();
        }

        Map<Priority, Integer> priorityCounts = count_tasks_by_priority_enum(allTasks);
        for (Priority priority : Priority.values()) {
            int count = priorityCounts.getOrDefault(priority, 0);
            double percent = (count * 100.0) / total;
            report.append(String.format("%-10s: %2d tasks (%5.1f%%)\n", priority, count, percent));
        }

        report.append("═══════════════════════════════════════════\n");
        return report.toString();
    }

    // Generate tag usage report - shows all tags and their frequency
    public String generate_tag_report() {
        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════\n");
        report.append("     TASK TAGS REPORT\n");
        report.append("═══════════════════════════════════════════\n");

        List<String> allTags = taskService.get_all_tags();
        if (allTags.isEmpty()) {
            report.append("No tags used yet\n");
            return report.toString();
        }

        report.append("All Tags:\n");
        for (String tag : allTags) {
            List<Task> taggedTasks = taskService.filter_by_tag(tag);
            report.append("  #").append(tag).append(" - ").append(taggedTasks.size()).append(" tasks\n");
        }

        report.append("═══════════════════════════════════════════\n");
        return report.toString();
    }

    // Print summary report to console - outputs report to stdout
    public void print_report() {
        System.out.println(generate_summary_report());
    }

    // Print user performance report to console - outputs user stats to stdout
    public void print_user_performance() {
        System.out.println(generate_user_performance_report());
    }

    // Save summary report to file - writes report to text file
    public void save_report(String filename) {
        FileUtils.writeToFile(filename, generate_summary_report());
    }

    // Save user performance report to file - writes user stats to file
    public void save_user_performance_report(String filename) {
        FileUtils.writeToFile(filename, generate_user_performance_report());
    }

    // Generate CSV export - creates comma-separated task data for spreadsheets
    public String generate_csv_export() {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Title,Description,Priority,Status,Created At,Due Date,Tags,Assigned User\n");

        for (Task task : taskService.get_all_tasks()) {
            csv.append(task.get_id()).append(",");
            csv.append("\"").append(escape_csv(task.get_title())).append("\",");
            csv.append("\"").append(escape_csv(task.get_description())).append("\",");
            csv.append(task.get_priority()).append(",");
            csv.append(task.get_status()).append(",");
            csv.append(task.get_created_at()).append(",");
            csv.append(task.get_due_date() != null ? task.get_due_date() : "N/A").append(",");
            csv.append("\"").append(String.join(";", task.get_tags())).append("\",");
            csv.append(task.get_assigned_user_id() > 0 ? task.get_assigned_user_id() : "Unassigned");
            csv.append("\n");
        }

        return csv.toString();
    }

    // Export CSV to file - writes task data to CSV file
    public void export_to_csv(String filename) {
        FileUtils.writeToFile(filename, generate_csv_export());
    }

    // Generate daily summary - creates brief summary for today
    public String generate_daily_summary() {
        StringBuilder summary = new StringBuilder();
        summary.append("📅 Daily Summary - ").append(LocalDateTime.now().toLocalDate()).append("\n\n");
        
        summary.append("Tasks: ").append(taskService.count_tasks()).append(" total, ");
        summary.append(taskService.count_completed_tasks()).append(" completed\n");
        summary.append("Users: ").append(userService.count_users()).append(" active\n");
        
        if (pomodoroService != null) {
            summary.append("Pomodoro Sessions: ").append(pomodoroService.get_total_sessions()).append("\n");
        }
        
        summary.append("\nOverdue: ").append(taskService.count_overdue_tasks()).append(" tasks need attention!\n");
        
        return summary.toString();
    }

    // Helper: Count tasks by status - returns map of status names to counts
    private Map<String, Integer> count_tasks_by_status(List<Task> tasks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Task task : tasks) {
            String status = task.get_status().toString();
            counts.put(status, counts.getOrDefault(status, 0) + 1);
        }
        return counts;
    }

    // Helper: Count tasks by priority - returns map of priority names to counts
    private Map<String, Integer> count_tasks_by_priority(List<Task> tasks) {
        Map<String, Integer> counts = new HashMap<>();
        for (Task task : tasks) {
            String priority = task.get_priority().toString();
            counts.put(priority, counts.getOrDefault(priority, 0) + 1);
        }
        return counts;
    }

    // Helper: Count tasks by priority enum - returns map with enum keys
    private Map<Priority, Integer> count_tasks_by_priority_enum(List<Task> tasks) {
        Map<Priority, Integer> counts = new HashMap<>();
        for (Task task : tasks) {
            Priority priority = task.get_priority();
            counts.put(priority, counts.getOrDefault(priority, 0) + 1);
        }
        return counts;
    }

    // Helper: Count users by role - returns map of role names to counts
    private Map<String, Integer> count_users_by_role(List<User> users) {
        Map<String, Integer> counts = new HashMap<>();
        for (User user : users) {
            String role = user.get_role().toString();
            counts.put(role, counts.getOrDefault(role, 0) + 1);
        }
        return counts;
    }

    // Helper: Escape CSV special characters - replaces commas and quotes
    private String escape_csv(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }
}
