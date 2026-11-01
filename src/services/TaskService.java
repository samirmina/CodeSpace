package services;

import models.Priority;
import models.Status;
import models.Task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskService - manages all task operations including CRUD, filtering, and statistics.
 * Supports both in-memory (JavaFX) and PostgreSQL-backed (web) operation modes.
 */
@Service
public class TaskService {
    private final Map<Integer, Task> tasks = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final PostgresTaskDAO postgresTaskDAO;
    private final boolean usePostgres;

    // No-arg constructor for JavaFX (in-memory only)
    public TaskService() {
        this.postgresTaskDAO = null;
        this.usePostgres = false;
    }

    // Constructor with PostgreSQL DAO for web app
    @Autowired
    public TaskService(PostgresTaskDAO postgresTaskDAO) {
        this.postgresTaskDAO = postgresTaskDAO;
        this.usePostgres = true;
        loadFromDatabase();
    }

    private void loadFromDatabase() {
        if (!usePostgres) return;
        try {
            // Load all tasks - in multi-user mode, this loads all tasks
            // User-specific filtering is done at the query level
            // For now, we load all and filter in memory
            // Note: PostgresTaskDAO.getAllTasksByUser requires a userId
            // We'll load tasks lazily per user instead
            System.out.println("ℹ TaskService initialized with PostgreSQL DAO");
        } catch (Exception e) {
            System.err.println("⚠ Could not initialize TaskService with PostgreSQL: " + e.getMessage());
        }
    }

    // Create new task - generates unique ID and stores task in memory and PostgreSQL
    public Task create_task(String title, String description, Priority priority) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        int id = idGenerator.getAndIncrement();
        Task task = new Task(id, title.trim(), description, priority);
        tasks.put(id, task);

        if (usePostgres) {
            // Note: Task creation for web app requires userId - use create_task with userId instead
            System.out.println("⚠ Use create_task(title, description, priority, userId) for PostgreSQL mode");
        }

        System.out.println("✓ Task created: " + task);
        return task;
    }

    // Create new task with user isolation - for web app multi-user support
    public Task create_task(String title, String description, Priority priority, long userId) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        int id = idGenerator.getAndIncrement();
        Task task = new Task(id, title.trim(), description, priority);
        tasks.put(id, task);

        if (usePostgres && postgresTaskDAO != null) {
            try {
                postgresTaskDAO.createTask(task, userId);
            } catch (Exception e) {
                System.err.println("⚠ Failed to persist task to PostgreSQL: " + e.getMessage());
                tasks.remove(id);
                idGenerator.decrementAndGet();
                throw new RuntimeException("Failed to create task in database");
            }
        }

        System.out.println("✓ Task created: " + task);
        return task;
    }

    // Get task by ID - returns task or throws exception if not found
    public Task get_task(int id) {
        Task task = tasks.get(id);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + id);
        }
        return task;
    }

    // Get task by ID with user isolation - for web app
    public Task get_task(int id, long userId) {
        if (usePostgres && postgresTaskDAO != null) {
            Task task = postgresTaskDAO.getTaskById(id, userId);
            if (task != null) {
                tasks.put(id, task);
                return task;
            }
            throw new IllegalArgumentException("Task not found: " + id + " for user " + userId);
        }
        return get_task(id);
    }

    // Try to get task safely - returns Optional without throwing exception
    public Optional<Task> try_get_task(int id) {
        return Optional.ofNullable(tasks.get(id));
    }

    // Try to get task safely with user isolation - for web app
    public Optional<Task> try_get_task(int id, long userId) {
        if (usePostgres && postgresTaskDAO != null) {
            return Optional.ofNullable(postgresTaskDAO.getTaskById(id, userId));
        }
        return try_get_task(id);
    }

    // Get all tasks - returns copy of all stored tasks (in-memory mode)
    public List<Task> get_all_tasks() {
        return new ArrayList<>(tasks.values());
    }

    // Get all tasks for a specific user - for web app multi-user support
    public List<Task> get_all_tasks(long userId) {
        if (usePostgres && postgresTaskDAO != null) {
            return postgresTaskDAO.getAllTasksByUser(userId);
        }
        return get_all_tasks().stream()
                .filter(t -> t.get_assigned_user_id() == (int) userId || t.get_assigned_user_id() == -1)
                .toList();
    }

    // Update task status - finds task and changes its status
    public Task update_task_status(int id, Status status) {
        Task task = get_task(id);
        Status oldStatus = task.get_status();
        task.set_status(status);
        if (usePostgres && postgresTaskDAO != null) {
            try {
                postgresTaskDAO.updateTask(task, -1); // userId not tracked in legacy mode
            } catch (Exception e) {
                System.err.println("⚠ Failed to update task in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ Task #" + id + " status updated from " + oldStatus + " to " + status);
        return task;
    }

    // Update task status with user isolation - for web app
    public Task update_task_status(int id, Status status, long userId) {
        if (usePostgres && postgresTaskDAO != null) {
            Task task = postgresTaskDAO.getTaskById(id, userId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + id + " for user " + userId);
            }
            Status oldStatus = task.get_status();
            task.set_status(status);
            try {
                postgresTaskDAO.updateTask(task, userId);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update task in PostgreSQL: " + e.getMessage());
            }
            tasks.put(id, task);
            System.out.println("✓ Task #" + id + " status updated from " + oldStatus + " to " + status);
            return task;
        }
        return update_task_status(id, status);
    }

    // Update task priority - finds task and changes its priority level
    public Task update_task_priority(int id, Priority priority) {
        Task task = get_task(id);
        task.set_priority(priority);
        if (usePostgres && postgresTaskDAO != null) {
            try {
                postgresTaskDAO.updateTask(task, -1);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update task in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ Task #" + id + " priority updated to " + priority);
        return task;
    }

    // Update task description - finds task and updates its description
    public Task update_task_description(int id, String description) {
        Task task = get_task(id);
        task.set_description(description);
        if (usePostgres && postgresTaskDAO != null) {
            try {
                postgresTaskDAO.updateTask(task, -1);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update task in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ Task #" + id + " description updated");
        return task;
    }

    // Update task title - finds task and updates its title
    public Task update_task_title(int id, String title) {
        Task task = get_task(id);
        task.set_title(title);
        if (usePostgres && postgresTaskDAO != null) {
            try {
                postgresTaskDAO.updateTask(task, -1);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update task in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ Task #" + id + " title updated");
        return task;
    }

    // Update task due date - finds task and sets its deadline
    public Task update_task_due_date(int id, LocalDateTime dueDate) {
        Task task = get_task(id);
        task.set_due_date(dueDate);
        if (usePostgres && postgresTaskDAO != null) {
            try {
                postgresTaskDAO.updateTask(task, -1);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update task in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ Task #" + id + " due date updated to " + dueDate);
        return task;
    }

    // Delete task - removes task from storage by ID
    public boolean delete_task(int id) {
        Task removed = tasks.remove(id);
        if (removed != null) {
            if (usePostgres && postgresTaskDAO != null) {
                try {
                    postgresTaskDAO.deleteTask(id, -1);
                } catch (Exception e) {
                    System.err.println("⚠ Failed to delete task from PostgreSQL: " + e.getMessage());
                    tasks.put(id, removed);
                    return false;
                }
            }
            System.out.println("✓ Task deleted: " + removed);
            return true;
        }
        return false;
    }

    // Delete task with user isolation - for web app
    public boolean delete_task(int id, long userId) {
        if (usePostgres && postgresTaskDAO != null) {
            boolean deleted = postgresTaskDAO.deleteTask(id, userId);
            tasks.remove(id);
            if (deleted) {
                System.out.println("✓ Task deleted: #" + id);
                return true;
            }
            return false;
        }
        return delete_task(id);
    }

    // Filter tasks by status - returns list of tasks matching status
    public List<Task> filter_by_status(Status status) {
        List<Task> filtered = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.get_status() == status) {
                filtered.add(task);
            }
        }
        return filtered;
    }

    // Filter tasks by priority - returns list of tasks matching priority level
    public List<Task> filter_by_priority(Priority priority) {
        List<Task> filtered = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.get_priority() == priority) {
                filtered.add(task);
            }
        }
        return filtered;
    }

    // Filter tasks by title keyword - returns tasks with title containing keyword
    public List<Task> search_by_title(String keyword) {
        List<Task> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (Task task : tasks.values()) {
            if (task.get_title().toLowerCase().contains(lowerKeyword)) {
                results.add(task);
            }
        }
        return results;
    }

    // Filter overdue tasks - returns incomplete tasks past their due date
    public List<Task> filter_overdue_tasks() {
        List<Task> overdue = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.is_overdue()) {
                overdue.add(task);
            }
        }
        return overdue;
    }

    // Filter tasks by tag - returns tasks containing specified tag
    public List<Task> filter_by_tag(String tag) {
        List<Task> tagged = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.has_tag(tag)) {
                tagged.add(task);
            }
        }
        return tagged;
    }

    // Filter tasks assigned to user - returns tasks assigned to specific user
    public List<Task> filter_by_user(int userId) {
        List<Task> assigned = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task.get_assigned_user_id() == userId) {
                assigned.add(task);
            }
        }
        return assigned;
    }

    // Filter incomplete tasks - returns tasks not in COMPLETED or CANCELLED status
    public List<Task> filter_incomplete_tasks() {
        List<Task> incomplete = new ArrayList<>();
        for (Task task : tasks.values()) {
            Status s = task.get_status();
            if (s != Status.COMPLETED && s != Status.CANCELLED) {
                incomplete.add(task);
            }
        }
        return incomplete;
    }

    // Filter urgent incomplete tasks - returns HIGH or URGENT priority incomplete tasks
    public List<Task> get_urgent_incomplete_tasks() {
        List<Task> urgent = new ArrayList<>();
        for (Task task : tasks.values()) {
            Priority p = task.get_priority();
            Status s = task.get_status();
            if ((p == Priority.URGENT || p == Priority.HIGH) && s != Status.COMPLETED) {
                urgent.add(task);
            }
        }
        return urgent;
    }

    // Sort tasks by priority - returns list ordered by urgency (URGENT first)
    public List<Task> sort_by_priority() {
        List<Task> sorted = new ArrayList<>(tasks.values());
        sorted.sort((t1, t2) -> t1.get_priority().compareTo(t2.get_priority()));
        return sorted;
    }

    // Sort tasks by creation date - returns list ordered by age (oldest first)
    public List<Task> sort_by_creation_date() {
        List<Task> sorted = new ArrayList<>(tasks.values());
        sorted.sort((t1, t2) -> t1.get_created_at().compareTo(t2.get_created_at()));
        return sorted;
    }

    // Sort tasks by due date - returns list ordered by deadline (earliest first)
    public List<Task> sort_by_due_date() {
        List<Task> sorted = new ArrayList<>(tasks.values());
        sorted.sort((t1, t2) -> {
            if (t1.get_due_date() == null) return 1;
            if (t2.get_due_date() == null) return -1;
            return t1.get_due_date().compareTo(t2.get_due_date());
        });
        return sorted;
    }

    // Get task statistics - returns count of tasks grouped by status
    public Map<String, Long> get_statistics() {
        Map<String, Long> stats = new HashMap<>();
        for (Task task : tasks.values()) {
            String status = task.get_status().toString();
            stats.put(status, stats.getOrDefault(status, 0L) + 1);
        }
        return stats;
    }

    // Get completion rate - returns percentage of completed tasks
    public double get_completion_rate() {
        int total = tasks.size();
        if (total == 0) return 0.0;
        long completed = 0;
        for (Task task : tasks.values()) {
            if (task.get_status() == Status.COMPLETED) {
                completed++;
            }
        }
        return (completed * 100.0) / total;
    }

    // Count tasks - returns total number of stored tasks
    public int count_tasks() {
        return tasks.size();
    }

    // Count completed tasks - returns number of tasks with COMPLETED status
    public int count_completed_tasks() {
        int count = 0;
        for (Task task : tasks.values()) {
            if (task.get_status() == Status.COMPLETED) {
                count++;
            }
        }
        return count;
    }

    // Count overdue tasks - returns number of tasks past their due date
    public int count_overdue_tasks() {
        int count = 0;
        for (Task task : tasks.values()) {
            if (task.is_overdue()) {
                count++;
            }
        }
        return count;
    }

    // Get average completion time - returns average minutes to complete tasks
    public double get_average_completion_time() {
        int total = 0;
        int count = 0;
        for (Task task : tasks.values()) {
            if (task.get_status() == Status.COMPLETED && task.get_completion_time_minutes() > 0) {
                total += task.get_completion_time_minutes();
                count++;
            }
        }
        return count > 0 ? (double) total / count : 0.0;
    }

    // Get all tags - returns list of all unique tags used across tasks
    public List<String> get_all_tags() {
        List<String> allTags = new ArrayList<>();
        for (Task task : tasks.values()) {
            for (String tag : task.get_tags()) {
                if (!allTags.contains(tag)) {
                    allTags.add(tag);
                }
            }
        }
        return allTags;
    }

    // Clear all tasks - removes all stored tasks from memory
    public void clear_all() {
        tasks.clear();
        System.out.println("✓ All tasks cleared");
    }

    // Add task directly - used for loading from storage without validation
    public void add_task_direct(Task task) {
        tasks.put(task.get_id(), task);
    }

    // Update ID generator - sets next ID to generate
    public void update_id_generator(int startFrom) {
        idGenerator.set(startFrom);
        System.out.println("ℹ Task ID generator set to start from: " + startFrom);
    }

    // Get current ID generator value - returns next ID to be assigned
    public int get_current_id() {
        return idGenerator.get();
    }
}
