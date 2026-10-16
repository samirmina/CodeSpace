package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * User model - represents a system user with role and task assignments
 * Stores user information including name, email, role, and assigned tasks
 */
public class User {
    private int id;
    private String name;
    private String email;
    private Role role;
    private List<Integer> assignedTaskIds;
    private int tasksCompletedCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;

    // Create user with required fields - initializes with default values
    public User(int id, String name, String email, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.assignedTaskIds = new ArrayList<>();
        this.tasksCompletedCount = 0;
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
    }

    // Get user ID - returns unique identifier
    public int get_id() { return id; }

    // Get user name - returns full name
    public String get_name() { return name; }

    // Set user name - updates full name
    public void set_name(String name) { this.name = name; }

    // Get user email - returns email address
    public String get_email() { return email; }

    // Set user email - updates email address
    public void set_email(String email) { this.email = email; }

    // Get user role - returns permission level
    public Role get_role() { return role; }

    // Set user role - updates permission level
    public void set_role(Role role) { this.role = role; }

    // Get assigned task IDs - returns list of assigned task identifiers
    public List<Integer> get_assigned_task_ids() { return assignedTaskIds; }

    // Get creation timestamp - returns when user was created
    public LocalDateTime get_created_at() { return createdAt; }

    // Get last active timestamp - returns last activity time
    public LocalDateTime get_last_active_at() { return lastActiveAt; }

    // Set creation timestamp - sets when user was created
    public void set_created_at(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Set last active timestamp - sets last activity time
    public void set_last_active_at(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    // Update last active time - marks user as currently active
    public void update_last_active() {
        this.lastActiveAt = LocalDateTime.now();
    }

    // Assign task to user - adds task ID to assignments
    public void assign_task(int taskId) {
        if (!assignedTaskIds.contains(taskId)) {
            assignedTaskIds.add(taskId);
            update_last_active();
        }
    }

    // Unassign task from user - removes task ID from assignments
    public void unassign_task(int taskId) {
        assignedTaskIds.remove(Integer.valueOf(taskId));
        update_last_active();
    }

    // Check if user has task assigned - returns true if task is assigned
    public boolean has_task_assigned(int taskId) {
        return assignedTaskIds.contains(taskId);
    }

    // Count assigned tasks - returns number of tasks assigned to user
    public int count_assigned_tasks() {
        return assignedTaskIds.size();
    }

    // Increment completed tasks counter - increases count when task completes
    public void increment_completed_count() {
        this.tasksCompletedCount++;
        update_last_active();
    }

    // Get tasks completed count - returns number of completed tasks
    public int get_tasks_completed_count() {
        return tasksCompletedCount;
    }

    // Get completion rate - returns percentage of completed vs assigned tasks
    public double get_completion_rate() {
        int total = assignedTaskIds.size();
        if (total == 0) return 0.0;
        return (tasksCompletedCount * 100.0) / total;
    }

    // Check if user is admin - returns true if role is ADMIN
    public boolean is_admin() {
        return role == Role.ADMIN;
    }

    // Check if user is manager - returns true if role is MANAGER
    public boolean is_manager() {
        return role == Role.MANAGER;
    }

    // Check if user is developer - returns true if role is DEVELOPER
    public boolean is_developer() {
        return role == Role.DEVELOPER;
    }

    // Check if user can edit a setting by number
    public boolean can_edit_setting(int settingNumber) {
        return role.canEditSetting(settingNumber);
    }

    // Check if user can view a setting by number
    public boolean can_view_setting(int settingNumber) {
        return role.canViewSetting(settingNumber);
    }

    // Get user age in days - returns days since user was created
    public int get_age_in_days() {
        long days = java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        return (int) days;
    }

    // Check equality - compares users by ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id;
    }

    // Get hash code - generates hash based on ID
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // Get string representation - returns formatted user summary
    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - %s | Tasks: %d | Completed: %d",
            id, name, email, role, count_assigned_tasks(), tasksCompletedCount);
    }
}
