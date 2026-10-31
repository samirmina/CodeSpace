package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pomodoro model - represents a pomodoro timer session for a task
 * Tracks pomodoro sessions including duration, completed count, and session history
 */
public class Pomodoro {
    private int id;
    private int taskId;
    private String taskName;
    private int durationMinutes;
    private int completedSessions;
    private LocalDateTime lastSessionDate;
    private boolean isActive;
    private LocalDateTime startedAt;
    private List<LocalDateTime> sessionHistory;
    private List<Integer> sessionDurations;

    // Create pomodoro session with task info - initializes with default values
    public Pomodoro(int id, int taskId, String taskName, int durationMinutes) {
        this.id = id;
        this.taskId = taskId;
        this.taskName = taskName;
        this.durationMinutes = durationMinutes;
        this.completedSessions = 0;
        this.lastSessionDate = null;
        this.isActive = false;
        this.startedAt = null;
        this.sessionHistory = new ArrayList<>();
        this.sessionDurations = new ArrayList<>();
    }

    // Get pomodoro ID - returns unique identifier
    public int get_id() { return id; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getId() { return id; }

    // Get task ID - returns associated task identifier
    public int get_task_id() { return taskId; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getTaskId() { return taskId; }

    // Set task ID - updates associated task
    public void set_task_id(int taskId) { this.taskId = taskId; }

    // Get task name - returns name of associated task
    public String get_task_name() { return taskName; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public String getTaskName() { return taskName; }

    // Set task name - updates associated task name
    public void set_task_name(String taskName) { this.taskName = taskName; }

    // Get duration in minutes - returns session length
    public int get_duration_minutes() { return durationMinutes; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getDurationMinutes() { return durationMinutes; }

    // Set duration in minutes - updates session length
    public void set_duration_minutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    // Get completed sessions count - returns number of finished sessions
    public int get_completed_sessions() { return completedSessions; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getCompletedSessions() { return completedSessions; }

    // Set completed sessions count - updates session counter directly
    public void set_completed_sessions(int completedSessions) {
        this.completedSessions = completedSessions;
    }

    // Get last session date - returns timestamp of last completed session
    public LocalDateTime get_last_session_date() { return lastSessionDate; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public LocalDateTime getLastSessionDate() { return lastSessionDate; }

    // Check if session is active - returns true if currently running
    public boolean is_active() { return isActive; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public boolean isActive() { return isActive; }

    // Set active state - marks session as running or stopped
    public void set_active(boolean active) { 
        isActive = active;
        if (active && startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    // Get session start time - returns when session was started
    public LocalDateTime get_started_at() { return startedAt; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public LocalDateTime getStartedAt() { return startedAt; }

    // Get session history - returns list of completed session timestamps
    public List<LocalDateTime> get_session_history() { return sessionHistory; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public List<LocalDateTime> getSessionHistory() { return sessionHistory; }

    // Get session durations - returns list of actual minutes for each completed session
    public List<Integer> get_session_durations() { return sessionDurations; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public List<Integer> getSessionDurations() { return sessionDurations; }

    // Get total focus time in minutes - returns sum of actual completed session durations
    public int get_total_focus_time_minutes() {
        int total = 0;
        int size = Math.min(sessionHistory.size(), sessionDurations.size());
        for (int i = 0; i < size; i++) {
            total += sessionDurations.get(i);
        }
        return total;
    }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getTotalFocusTimeMinutes() {
        return get_total_focus_time_minutes();
    }

    // Get elapsed time in seconds - returns seconds since session started
    public int get_elapsed_time_seconds() {
        if (startedAt == null || !isActive) return 0;
        long seconds = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
        return (int) seconds;
    }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getElapsedTimeSeconds() {
        return get_elapsed_time_seconds();
    }

    // Get remaining time in seconds - returns seconds left in current session
    public int get_remaining_time_seconds() {
        int totalSeconds = durationMinutes * 60;
        int elapsed = get_elapsed_time_seconds();
        return Math.max(0, totalSeconds - elapsed);
    }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getRemainingTimeSeconds() {
        return get_remaining_time_seconds();
    }

    // Check if session is complete - returns true if duration has passed
    public boolean is_session_complete() {
        return get_remaining_time_seconds() <= 0;
    }

    // Standard JavaBean getter for Thymeleaf compatibility
    public boolean isSessionComplete() {
        return is_session_complete();
    }

    // Increment session counter - called when pomodoro session completes
    public void increment_sessions(int durationMinutes) {
        this.completedSessions++;
        this.lastSessionDate = LocalDateTime.now();
        this.sessionHistory.add(this.lastSessionDate);
        this.sessionDurations.add(durationMinutes);
    }

    // Reset session counter - clears completed sessions and history
    public void reset_sessions() {
        this.completedSessions = 0;
        this.lastSessionDate = null;
        this.sessionHistory.clear();
        this.sessionDurations.clear();
    }

    // Validate and repair session history/durations parity
    public void validate_session_parity() {
        int historySize = sessionHistory.size();
        int durationsSize = sessionDurations.size();
        
        if (historySize != durationsSize) {
            // Trim the larger list to match the smaller
            int minSize = Math.min(historySize, durationsSize);
            while (sessionHistory.size() > minSize) {
                sessionHistory.remove(sessionHistory.size() - 1);
            }
            while (sessionDurations.size() > minSize) {
                sessionDurations.remove(sessionDurations.size() - 1);
            }
            completedSessions = minSize;
            if (minSize > 0) {
                lastSessionDate = sessionHistory.get(minSize - 1);
            } else {
                lastSessionDate = null;
            }
        }
    }

    // Get string representation - returns formatted pomodoro summary
    @Override
    public String toString() {
        return String.format("[%d] %s - %d sessions completed", id, taskName, completedSessions);
    }
}
