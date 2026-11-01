package services;

import models.Role;
import models.Task;
import models.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * UserService - manages all user operations including CRUD and role filtering.
 * Supports both in-memory (JavaFX) and PostgreSQL-backed (web) operation modes.
 */
@Service
public class UserService {
    private final Map<Integer, User> users = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final PostgresUserDAO postgresUserDAO;
    private final boolean usePostgres;

    // No-arg constructor for JavaFX (in-memory only)
    public UserService() {
        this.postgresUserDAO = null;
        this.usePostgres = false;
    }

    // Constructor with PostgreSQL DAO for web app
    @Autowired
    public UserService(PostgresUserDAO postgresUserDAO) {
        this.postgresUserDAO = postgresUserDAO;
        this.usePostgres = true;
        loadFromDatabase();
    }

    private void loadFromDatabase() {
        if (!usePostgres) return;
        try {
            List<User> dbUsers = postgresUserDAO.getAllUsers();
            for (User user : dbUsers) {
                users.put(user.get_id(), user);
                if (user.get_id() >= idGenerator.get()) {
                    idGenerator.set(user.get_id() + 1);
                }
            }
            System.out.println("ℹ Loaded " + dbUsers.size() + " users from PostgreSQL");
        } catch (Exception e) {
            System.err.println("⚠ Could not load users from PostgreSQL: " + e.getMessage());
        }
    }

    // Create new user - validates input, stores in PostgreSQL and memory
    public User create_user(String name, String email, Role role) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }

        int id = idGenerator.getAndIncrement();
        User user = new User(id, name.trim(), email.trim(), role);
        users.put(id, user);

        // Persist to PostgreSQL if enabled
        if (usePostgres) {
            try {
                postgresUserDAO.createUser(user);
            } catch (Exception e) {
                System.err.println("⚠ Failed to persist user to PostgreSQL: " + e.getMessage());
                users.remove(id);
                idGenerator.decrementAndGet();
                throw new RuntimeException("Failed to create user in database");
            }
        }

        System.out.println("✓ User created: " + user);
        return user;
    }

    // Get user by ID - returns user or throws exception if not found
    public User get_user(int id) {
        User user = users.get(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        return user;
    }

    // Try to get user safely - returns Optional without throwing exception
    public Optional<User> try_get_user(int id) {
        return Optional.ofNullable(users.get(id));
    }

    // Get all users - returns copy of all stored users
    public List<User> get_all_users() {
        return new ArrayList<>(users.values());
    }

    // Filter users by role - returns list of users with matching role
    public List<User> filter_by_role(Role role) {
        List<User> filtered = new ArrayList<>();
        for (User user : users.values()) {
            if (user.get_role() == role) {
                filtered.add(user);
            }
        }
        return filtered;
    }

    // Filter users by email domain - returns users with matching email domain
    public List<User> filter_by_email_domain(String domain) {
        List<User> filtered = new ArrayList<>();
        for (User user : users.values()) {
            if (user.get_email().endsWith("@" + domain)) {
                filtered.add(user);
            }
        }
        return filtered;
    }

    // Search users by name - returns users with name containing keyword
    public List<User> search_by_name(String keyword) {
        List<User> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (User user : users.values()) {
            if (user.get_name().toLowerCase().contains(lowerKeyword)) {
                results.add(user);
            }
        }
        return results;
    }

    // Delete user - removes user from PostgreSQL and memory
    public boolean delete_user(int id) {
        User removed = users.remove(id);
        if (removed != null) {
            if (usePostgres) {
                try {
                    postgresUserDAO.deleteUser(id);
                } catch (Exception e) {
                    System.err.println("⚠ Failed to delete user from PostgreSQL: " + e.getMessage());
                    users.put(id, removed);
                    return false;
                }
            }
            System.out.println("✓ User deleted: " + removed);
            return true;
        }
        return false;
    }

    // Update user name - finds user and updates their name in PostgreSQL and memory
    public User update_user_name(int id, String name) {
        User user = get_user(id);
        user.set_name(name);
        if (usePostgres) {
            try {
                postgresUserDAO.updateUser(user);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update user in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ User #" + id + " name updated to " + name);
        return user;
    }

    // Update user email - finds user and updates their email in PostgreSQL and memory
    public User update_user_email(int id, String email) {
        User user = get_user(id);
        user.set_email(email);
        if (usePostgres) {
            try {
                postgresUserDAO.updateUser(user);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update user in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ User #" + id + " email updated to " + email);
        return user;
    }

    // Update user role - finds user and updates their role in PostgreSQL and memory
    public User update_user_role(int id, Role role) {
        User user = get_user(id);
        user.set_role(role);
        if (usePostgres) {
            try {
                postgresUserDAO.updateUser(user);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update user in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ User #" + id + " role updated to " + role);
        return user;
    }

    // Assign task to user - adds task ID to user's assignments
    public User assign_task_to_user(int userId, int taskId) {
        User user = get_user(userId);
        user.assign_task(taskId);
        if (usePostgres) {
            try {
                postgresUserDAO.updateUser(user);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update user in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ Task #" + taskId + " assigned to " + user.get_name());
        return user;
    }

    // Unassign task from user - removes task from user's assignments
    public User unassign_task_from_user(int userId, int taskId) {
        User user = get_user(userId);
        user.unassign_task(taskId);
        if (usePostgres) {
            try {
                postgresUserDAO.updateUser(user);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update user in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ Task #" + taskId + " unassigned from " + user.get_name());
        return user;
    }

    // Mark task as completed by user - increments user's completion counter
    public void mark_task_completed_for_user(int userId) {
        User user = get_user(userId);
        user.increment_completed_count();
        if (usePostgres) {
            try {
                postgresUserDAO.updateUser(user);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update user in PostgreSQL: " + e.getMessage());
            }
        }
    }

    // Get user by task ID - finds user assigned to specific task
    public Optional<User> get_user_by_task_id(int taskId) {
        for (User user : users.values()) {
            if (user.has_task_assigned(taskId)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    // Count users - returns total number of stored users
    public int count_users() {
        return users.size();
    }

    // Count users by role - returns number of users with specific role
    public int count_users_by_role(Role role) {
        int count = 0;
        for (User user : users.values()) {
            if (user.get_role() == role) {
                count++;
            }
        }
        return count;
    }

    // Get users with overdue tasks - returns users who have overdue tasks assigned
    public List<User> get_users_with_overdue_tasks(TaskService taskService) {
        List<User> usersWithOverdue = new ArrayList<>();
        for (User user : users.values()) {
            for (int taskId : user.get_assigned_task_ids()) {
                Optional<Task> taskOpt = taskService.try_get_task(taskId);
                if (taskOpt.isPresent() && taskOpt.get().is_overdue()) {
                    usersWithOverdue.add(user);
                    break;
                }
            }
        }
        return usersWithOverdue;
    }

    // Get top performers - returns users sorted by completed tasks count
    public List<User> get_top_performers(int limit) {
        List<User> sorted = new ArrayList<>(users.values());
        sorted.sort((u1, u2) -> Integer.compare(u2.get_tasks_completed_count(), u1.get_tasks_completed_count()));

        List<User> top = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, sorted.size()); i++) {
            top.add(sorted.get(i));
        }
        return top;
    }

    // Get average completion rate - returns average completion rate across all users
    public double get_average_completion_rate() {
        int total = users.size();
        if (total == 0) return 0.0;

        double sum = 0;
        for (User user : users.values()) {
            sum += user.get_completion_rate();
        }
        return sum / total;
    }

    // Clear all users - removes all stored users from memory
    public void clear_all() {
        users.clear();
        System.out.println("✓ All users cleared");
    }

    // Add user directly - used for loading from storage without validation
    public void add_user_direct(User user) {
        users.put(user.get_id(), user);
        if (user.get_id() >= idGenerator.get()) {
            idGenerator.set(user.get_id() + 1);
        }
    }

    // Update ID generator - sets next ID to generate
    public void update_id_generator(int startFrom) {
        idGenerator.set(startFrom);
        System.out.println("ℹ User ID generator set to start from: " + startFrom);
    }

    // Get current ID generator value - returns next ID to be assigned
    public int get_current_id() {
        return idGenerator.get();
    }
}
