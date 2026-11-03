package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    public int get_id() { return id; }

    public void set_id(int id) { this.id = id; }

    public int get_task_id() { return taskId; }

    public void set_task_id(int taskId) { this.taskId = taskId; }

    public String get_task_name() { return taskName; }

    public void set_task_name(String taskName) { this.taskName = taskName; }

    public int get_duration_minutes() { return durationMinutes; }

    public void set_duration_minutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int get_completed_sessions() { return completedSessions; }

    public void set_completed_sessions(int completedSessions) {
        this.completedSessions = completedSessions;
    }

    public LocalDateTime get_last_session_date() { return lastSessionDate; }

    public void set_last_session_date(LocalDateTime lastSessionDate) {
        this.lastSessionDate = lastSessionDate;
    }

    public boolean is_active() { return isActive; }

    public void set_active(boolean active) { 
        isActive = active;
        if (active && startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime get_started_at() { return startedAt; }

    public void set_started_at(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public List<LocalDateTime> get_session_history() { return sessionHistory; }

    public List<Integer> get_session_durations() { return sessionDurations; }

    public int get_total_focus_time_minutes() {
        int total = 0;
        int size = Math.min(sessionHistory.size(), sessionDurations.size());
        for (int i = 0; i < size; i++) {
            total += sessionDurations.get(i);
        }
        return total;
    }

    public int get_elapsed_time_seconds() {
        if (startedAt == null || !isActive) return 0;
        long seconds = java.time.Duration.between(startedAt, LocalDateTime.now()).getSeconds();
        return (int) seconds;
    }

    public int get_remaining_time_seconds() {
        int totalSeconds = durationMinutes * 60;
        int elapsed = get_elapsed_time_seconds();
        return Math.max(0, totalSeconds - elapsed);
    }

    public boolean is_session_complete() {
        return get_remaining_time_seconds() <= 0;
    }

    public void increment_sessions(int durationMinutes) {
        this.completedSessions++;
        this.lastSessionDate = LocalDateTime.now();
        this.sessionHistory.add(this.lastSessionDate);
        this.sessionDurations.add(durationMinutes);
    }

    public void reset_sessions() {
        this.completedSessions = 0;
        this.lastSessionDate = null;
        this.sessionHistory.clear();
        this.sessionDurations.clear();
    }

    public void validate_session_parity() {
        int historySize = sessionHistory.size();
        int durationsSize = sessionDurations.size();
        
        if (historySize != durationsSize) {
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

    @Override
    public String toString() {
        return String.format("[%d] %s - %d sessions completed", id, taskName, completedSessions);
    }
}