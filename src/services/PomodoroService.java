package services;

import models.Pomodoro;
import models.Task;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

/**
 * PomodoroService - manages all pomodoro timer sessions
 * Provides methods to create, track, and analyze pomodoro sessions for tasks
 */
@Service
public class PomodoroService {
    private final Map<Integer, Pomodoro> pomodoros = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private int defaultDuration = 25;

    // Create pomodoro with default duration - generates ID and stores session
    public Pomodoro create_pomodoro(int taskId, String taskName) {
        return create_pomodoro(taskId, taskName, defaultDuration);
    }

    // Create pomodoro with custom duration - allows custom session length
    public Pomodoro create_pomodoro(int taskId, String taskName, int durationMinutes) {
        int id = idGenerator.getAndIncrement();
        Pomodoro pomodoro = new Pomodoro(id, taskId, taskName, durationMinutes);
        pomodoros.put(id, pomodoro);
        System.out.println("✓ Pomodoro created: " + pomodoro);
        return pomodoro;
    }

    // Get pomodoro by ID - returns session or throws exception if not found
    public Pomodoro get_pomodoro(int id) {
        Pomodoro pomodoro = pomodoros.get(id);
        if (pomodoro == null) {
            throw new IllegalArgumentException("Pomodoro not found: " + id);
        }
        return pomodoro;
    }

    // Try to get pomodoro safely - returns Optional without throwing exception
    public Optional<Pomodoro> try_get_pomodoro(int id) {
        return Optional.ofNullable(pomodoros.get(id));
    }

    // Get all pomodoros - returns copy of all sessions
    public List<Pomodoro> get_all_pomodoros() {
        return new ArrayList<>(pomodoros.values());
    }

    // Get or create pomodoro for task - returns existing or creates new session for task
    public Pomodoro get_or_create_pomodoro(int taskId, String taskName) {
        Optional<Pomodoro> existing = get_pomodoro_by_task_id(taskId);
        if (existing.isPresent()) {
            return existing.get();
        }
        return create_pomodoro(taskId, taskName);
    }

    // Increment session - marks session as completed and updates timestamp
    public Pomodoro increment_session(int id) {
        Pomodoro p = get_pomodoro(id);
        p.increment_sessions(p.get_duration_minutes());
        System.out.println("✓ Session completed for: " + p.get_task_name());
        return p;
    }

    // Reset sessions - clears completed session count for pomodoro
    public Pomodoro reset_sessions(int id) {
        Pomodoro p = get_pomodoro(id);
        p.reset_sessions();
        System.out.println("✓ Sessions reset for: " + p.get_task_name());
        return p;
    }

    // Start pomodoro session - marks session as active
    public Pomodoro start_session(int id) {
        Pomodoro p = get_pomodoro(id);
        p.set_active(true);
        System.out.println("✓ Session started for: " + p.get_task_name());
        return p;
    }

    // Stop pomodoro session - marks session as inactive
    public Pomodoro stop_session(int id) {
        Pomodoro p = get_pomodoro(id);
        p.set_active(false);
        System.out.println("✓ Session stopped for: " + p.get_task_name());
        return p;
    }

    // Delete pomodoro - removes session from storage by ID
    public boolean delete_pomodoro(int id) {
        Pomodoro removed = pomodoros.remove(id);
        if (removed != null) {
            System.out.println("✓ Pomodoro deleted: " + removed);
            return true;
        }
        return false;
    }

    // Get total sessions - sums all completed sessions across all pomodoros
    public int get_total_sessions() {
        int total = 0;
        for (Pomodoro p : pomodoros.values()) {
            total += p.get_completed_sessions();
        }
        return total;
    }

    // Get total focus time in minutes - sums actual focus time across all sessions
    public int get_total_focus_time_minutes() {
        int total = 0;
        for (Pomodoro p : pomodoros.values()) {
            total += p.get_total_focus_time_minutes();
        }
        return total;
    }

    // Get total sessions for tasks with storeSessions=true
    public int get_total_stored_sessions(TaskService taskService) {
        int total = 0;
        for (Pomodoro p : pomodoros.values()) {
            if (taskService.try_get_task(p.get_task_id())
                    .map(Task::is_store_sessions)
                    .orElse(true)) {
                total += p.get_completed_sessions();
            }
        }
        return total;
    }

    // Get total focus time for tasks with storeSessions=true
    public int get_total_stored_focus_time_minutes(TaskService taskService) {
        int total = 0;
        for (Pomodoro p : pomodoros.values()) {
            if (taskService.try_get_task(p.get_task_id())
                    .map(Task::is_store_sessions)
                    .orElse(true)) {
                total += p.get_total_focus_time_minutes();
            }
        }
        return total;
    }

    // Get sessions by task name for stored sessions only
    public Map<String, Integer> get_stored_sessions_by_task_name(TaskService taskService) {
        Map<String, Integer> result = new HashMap<>();
        for (Pomodoro p : pomodoros.values()) {
            if (taskService.try_get_task(p.get_task_id())
                    .map(Task::is_store_sessions)
                    .orElse(true)) {
                String name = p.get_task_name();
                result.put(name, result.getOrDefault(name, 0) + p.get_total_focus_time_minutes());
            }
        }
        return result;
    }

    // Get sessions by task ID for stored sessions only
    public Map<Integer, Integer> get_stored_sessions_by_task_id(TaskService taskService) {
        Map<Integer, Integer> result = new HashMap<>();
        for (Pomodoro p : pomodoros.values()) {
            if (taskService.try_get_task(p.get_task_id())
                    .map(Task::is_store_sessions)
                    .orElse(true)) {
                int taskId = p.get_task_id();
                int focus = p.get_total_focus_time_minutes();
                result.put(taskId, result.getOrDefault(taskId, 0) + focus);
            }
        }
        return result;
    }

    // Get most productive task among stored sessions only
    public String get_most_productive_stored_task(TaskService taskService) {
        Map<String, Integer> focusByTask = get_stored_sessions_by_task_name(taskService);
        String mostProductive = null;
        int maxFocus = 0;
        
        for (Map.Entry<String, Integer> entry : focusByTask.entrySet()) {
            if (entry.getValue() > maxFocus) {
                maxFocus = entry.getValue();
                mostProductive = entry.getKey();
            }
        }
        return mostProductive;
    }

    // Get sessions by task name - maps task names to their total focus minutes
    public Map<String, Integer> get_sessions_by_task_name() {
        Map<String, Integer> result = new HashMap<>();
        for (Pomodoro p : pomodoros.values()) {
            String name = p.get_task_name();
            result.put(name, result.getOrDefault(name, 0) + p.get_total_focus_time_minutes());
        }
        return result;
    }

    // Get sessions by task ID - aggregates focus minutes for each task ID
    public Map<Integer, Integer> get_sessions_by_task_id() {
        Map<Integer, Integer> result = new HashMap<>();
        for (Pomodoro p : pomodoros.values()) {
            int taskId = p.get_task_id();
            int focus = p.get_total_focus_time_minutes();
            result.put(taskId, result.getOrDefault(taskId, 0) + focus);
        }
        return result;
    }

    // Get sessions for task - returns total focus minutes for specific task ID
    public int get_sessions_for_task(int taskId) {
        int total = 0;
        for (Pomodoro p : pomodoros.values()) {
            if (p.get_task_id() == taskId) {
                total += p.get_total_focus_time_minutes();
            }
        }
        return total;
    }

    // Get task session details - returns list of (timestamp, durationMinutes) for a task
    public List<javafx.util.Pair<LocalDateTime, Integer>> get_task_sessions(int taskId) {
        java.util.List<javafx.util.Pair<LocalDateTime, Integer>> result = new java.util.ArrayList<>();
        for (Pomodoro p : pomodoros.values()) {
            if (p.get_task_id() == taskId) {
                List<LocalDateTime> history = p.get_session_history();
                List<Integer> durations = p.get_session_durations();
                int size = Math.min(history.size(), durations.size());
                for (int i = 0; i < size; i++) {
                    result.add(new javafx.util.Pair<>(history.get(i), durations.get(i)));
                }
            }
        }
        result.sort((a, b) -> a.getKey().compareTo(b.getKey()));
        return result;
    }

    // Get pomodoro by task ID - finds first pomodoro for task ID
    public Optional<Pomodoro> get_pomodoro_by_task_id(int taskId) {
        for (Pomodoro p : pomodoros.values()) {
            if (p.get_task_id() == taskId) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    // Get active pomodoros - returns list of currently running sessions
    public List<Pomodoro> get_active_pomodoros() {
        List<Pomodoro> active = new ArrayList<>();
        for (Pomodoro p : pomodoros.values()) {
            if (p.is_active()) {
                active.add(p);
            }
        }
        return active;
    }

    // Get distinct session dates across all pomodoros
    public java.util.Set<java.time.LocalDate> get_session_dates() {
        java.util.Set<java.time.LocalDate> dates = new java.util.HashSet<>();
        for (Pomodoro p : pomodoros.values()) {
            for (java.time.LocalDateTime dt : p.get_session_history()) {
                dates.add(dt.toLocalDate());
            }
        }
        return dates;
    }

    // Count distinct active days (days with at least one completed session)
    public int get_active_days_count() {
        return get_session_dates().size();
    }

    // Get session counts by task name - maps task names to completed session counts
    public Map<String, Integer> get_session_counts_by_task_name() {
        Map<String, Integer> result = new HashMap<>();
        for (Pomodoro p : pomodoros.values()) {
            String name = p.get_task_name();
            result.put(name, result.getOrDefault(name, 0) + p.get_completed_sessions());
        }
        return result;
    }

    // Get pomodoros with sessions - returns sessions that have at least one completion
    public List<Pomodoro> get_pomodoros_with_sessions() {
        List<Pomodoro> withSessions = new ArrayList<>();
        for (Pomodoro p : pomodoros.values()) {
            if (p.get_completed_sessions() > 0) {
                withSessions.add(p);
            }
        }
        return withSessions;
    }

    // Get average sessions per task - returns mean sessions across all pomodoros
    public double get_average_sessions_per_task() {
        int count = pomodoros.size();
        if (count == 0) return 0.0;
        return (double) get_total_sessions() / count;
    }

    // Get most productive task - returns task name with most focus time
    public String get_most_productive_task() {
        Map<String, Integer> focusByTask = get_sessions_by_task_name();
        String mostProductive = null;
        int maxFocus = 0;
        
        for (Map.Entry<String, Integer> entry : focusByTask.entrySet()) {
            if (entry.getValue() > maxFocus) {
                maxFocus = entry.getValue();
                mostProductive = entry.getKey();
            }
        }
        return mostProductive;
    }

    // Get last N sessions - returns list of (taskName, timestamp, durationMinutes) sorted by time descending
    public List<javafx.util.Pair<String, javafx.util.Pair<LocalDateTime, Integer>>> get_last_sessions(int n) {
        java.util.List<javafx.util.Pair<String, javafx.util.Pair<LocalDateTime, Integer>>> all = new java.util.ArrayList<>();
        for (Pomodoro p : pomodoros.values()) {
            List<LocalDateTime> history = p.get_session_history();
            List<Integer> durations = p.get_session_durations();
            int size = Math.min(history.size(), durations.size());
            for (int i = 0; i < size; i++) {
                all.add(new javafx.util.Pair<>(p.get_task_name(), new javafx.util.Pair<>(history.get(i), durations.get(i))));
            }
        }
        all.sort((a, b) -> b.getValue().getKey().compareTo(a.getValue().getKey()));
        return all.subList(0, Math.min(n, all.size()));
    }

    // Count pomodoros - returns total number of stored pomodoro sessions
    public int count_pomodoros() {
        return pomodoros.size();
    }

    // Set default duration - updates default pomodoro length in minutes
    public void set_default_duration(int minutes) {
        this.defaultDuration = minutes;
    }

    // Get default duration - returns default pomodoro length
    public int get_default_duration() {
        return defaultDuration;
    }

    // Clear all pomodoros - removes all sessions from memory
    public void clear_all() {
        pomodoros.clear();
        System.out.println("✓ All Pomodoro sessions cleared");
    }

    // Add pomodoro directly - used for loading from storage without validation
    public void add_pomodoro_direct(Pomodoro pomodoro) {
        pomodoros.put(pomodoro.get_id(), pomodoro);
        if (pomodoro.get_id() >= idGenerator.get()) {
            idGenerator.set(pomodoro.get_id() + 1);
        }
    }

    // Update ID generator - sets next ID to generate
    public void update_id_generator(int startFrom) {
        idGenerator.set(startFrom);
    }

    // Delete all pomodoros for a specific task
    public void delete_pomodoros_for_task(int taskId) {
        pomodoros.entrySet().removeIf(entry -> entry.getValue().get_task_id() == taskId);
    }
}
