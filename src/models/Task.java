package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Task {
    private int id;
    private String title;
    private String description;
    private Priority priority;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private LocalDateTime completedAt;
    private int completionTimeMinutes;
    private List<String> tags;
    private int assignedUserId;
    private int categoryId;
    private boolean storeSessions = true;

    public Task(int id, String title, String description, Priority priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = Status.PENDING;
        this.createdAt = LocalDateTime.now();
        this.completionTimeMinutes = 0;
        this.tags = new ArrayList<>();
        this.assignedUserId = -1;
        this.categoryId = -1;
    }

    public int get_id() { return id; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getId() { return id; }

    public String get_title() { return title; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public String getTitle() { return title; }

    public void set_title(String title) { this.title = title; }

    public String get_description() { return description; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public String getDescription() { return description; }

    public void set_description(String description) { this.description = description; }

    public Priority get_priority() { return priority; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public Priority getPriority() { return priority; }

    public void set_priority(Priority priority) { this.priority = priority; }

    public Status get_status() { return status; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public Status getStatus() { return status; }

    public void set_status(Status status) {
        Status oldStatus = this.status;
        this.status = status;
        if (status == Status.COMPLETED) {
            if (this.completedAt == null) {
                this.completedAt = LocalDateTime.now();
            }
            // Auto-calculate completion time if not already set
            if (this.completionTimeMinutes == 0 && this.createdAt != null) {
                long minutes = java.time.Duration.between(this.createdAt, this.completedAt).toMinutes();
                this.completionTimeMinutes = (int) Math.max(1, minutes);
            }
        } else if (oldStatus == Status.COMPLETED) {
            // Task was completed but now changed to another status
            // Keep completedAt and completionTimeMinutes for history
        }
    }

    public LocalDateTime get_created_at() { return createdAt; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime get_due_date() { return dueDate; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public LocalDateTime getDueDate() { return dueDate; }

    public void set_due_date(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public LocalDateTime get_completed_at() { return completedAt; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void set_completed_at(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public int get_completion_time_minutes() { return completionTimeMinutes; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getCompletionTimeMinutes() { return completionTimeMinutes; }

    public void set_completion_time_minutes(int minutes) { this.completionTimeMinutes = minutes; }

    public List<String> get_tags() { return tags; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public List<String> getTags() { return tags; }

    public void add_tag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !tags.contains(tag.trim())) {
            tags.add(tag.trim());
        }
    }

    public void remove_tag(String tag) {
        tags.remove(tag);
    }

    public boolean has_tag(String tag) {
        return tags.contains(tag);
    }

    public int get_assigned_user_id() { return assignedUserId; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getAssignedUserId() { return assignedUserId; }

    public void set_assigned_user_id(int userId) { this.assignedUserId = userId; }

    public int get_category_id() { return categoryId; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getCategoryId() { return categoryId; }

    public void set_category_id(int categoryId) { this.categoryId = categoryId; }

    public boolean is_store_sessions() { return storeSessions; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public boolean isStoreSessions() { return storeSessions; }

    public void set_store_sessions(boolean storeSessions) { this.storeSessions = storeSessions; }

    public boolean is_overdue() {
        if (dueDate == null || status == Status.COMPLETED) return false;
        return LocalDateTime.now().isAfter(dueDate);
    }

    public boolean is_completed() {
        return status == Status.COMPLETED;
    }

    public boolean is_in_progress() {
        return status == Status.IN_PROGRESS;
    }

    public int get_days_until_due() {
        if (dueDate == null) return -1;
        long days = java.time.Duration.between(LocalDateTime.now(), dueDate).toDays();
        return (int) days;
    }

    public int get_age_in_days() {
        long days = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        return (int) days;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s", id, title, priority, status);
    }
}
