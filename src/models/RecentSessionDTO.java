package models;

import java.time.LocalDateTime;

/**
 * DTO for recent pomodoro sessions (web-compatible, no JavaFX dependencies)
 */
public class RecentSessionDTO {
    private String taskName;
    private LocalDateTime timestamp;
    private Integer durationMinutes;

    public RecentSessionDTO() {}

    public RecentSessionDTO(String taskName, LocalDateTime timestamp, Integer durationMinutes) {
        this.taskName = taskName;
        this.timestamp = timestamp;
        this.durationMinutes = durationMinutes;
    }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
}