package services;

import models.Category;
import models.Priority;
import models.Status;
import models.Task;
import models.User;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgresTaskDAO - PostgreSQL-backed task repository with user isolation.
 * All queries are filtered by user_id to ensure data isolation between clients.
 */
@Service
public class PostgresTaskDAO {
    private final String url;
    private final String user;
    private final String password;

    public PostgresTaskDAO() {
        this(Config.getDbUrl(), Config.getDbUser(), Config.getDbPassword());
    }

    public PostgresTaskDAO(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        loadDriver();
    }

    private void loadDriver() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC driver not found: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public List<Task> getAllTasksByUser(long userId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, title, description, priority, status, created_at, due_date, " +
                     "completed_at, completion_time_minutes, assigned_user_id, category_id, store_sessions, tags " +
                     "FROM tasks WHERE user_id = ? ORDER BY id";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Task task = new Task(
                    (int) rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    Priority.valueOf(rs.getString("priority"))
                );
                task.set_status(Status.valueOf(rs.getString("status")));
                if (rs.getTimestamp("created_at") != null) {
                    task.set_created_at(rs.getTimestamp("created_at").toLocalDateTime());
                }
                if (rs.getTimestamp("due_date") != null) {
                    task.set_due_date(rs.getTimestamp("due_date").toLocalDateTime());
                }
                if (rs.getTimestamp("completed_at") != null) {
                    task.set_completed_at(rs.getTimestamp("completed_at").toLocalDateTime());
                }
                task.set_completion_time_minutes(rs.getInt("completion_time_minutes"));
                task.set_assigned_user_id(rs.getInt("assigned_user_id"));
                task.set_category_id(rs.getInt("category_id"));
                task.set_store_sessions(rs.getBoolean("store_sessions"));
                tasks.add(task);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tasks from PostgreSQL: " + e.getMessage());
        }

        return tasks;
    }

    public Task getTaskById(long id, long userId) {
        String sql = "SELECT * FROM tasks WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapTask(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching task: " + e.getMessage());
        }

        return null;
    }

    public boolean createTask(Task task, long userId) {
        String sql = "INSERT INTO tasks (user_id, title, description, priority, status, created_at, " +
                     "due_date, completed_at, completion_time_minutes, assigned_user_id, category_id, store_sessions, tags) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, task.get_title());
            pstmt.setString(3, task.get_description());
            pstmt.setString(4, task.get_priority().name());
            pstmt.setString(5, task.get_status().name());
            pstmt.setTimestamp(6, Timestamp.valueOf(task.get_created_at()));
            if (task.get_due_date() != null) {
                pstmt.setTimestamp(7, Timestamp.valueOf(task.get_due_date()));
            } else {
                pstmt.setNull(7, Types.TIMESTAMP);
            }
            if (task.get_completed_at() != null) {
                pstmt.setTimestamp(8, Timestamp.valueOf(task.get_completed_at()));
            } else {
                pstmt.setNull(8, Types.TIMESTAMP);
            }
            pstmt.setInt(9, task.get_completion_time_minutes());
            pstmt.setInt(10, task.get_assigned_user_id());
            pstmt.setInt(11, task.get_category_id());
            pstmt.setBoolean(12, task.is_store_sessions());
            pstmt.setArray(13, conn.createArrayOf("text", task.get_tags().toArray()));

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        task.set_id((int) rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating task: " + e.getMessage());
        }

        return false;
    }

    public boolean updateTask(Task task, long userId) {
        String sql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, " +
                     "due_date = ?, completed_at = ?, completion_time_minutes = ?, assigned_user_id = ?, " +
                     "category_id = ?, store_sessions = ?, tags = ? WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, task.get_title());
            pstmt.setString(2, task.get_description());
            pstmt.setString(3, task.get_priority().name());
            pstmt.setString(4, task.get_status().name());
            if (task.get_due_date() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(task.get_due_date()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }
            if (task.get_completed_at() != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(task.get_completed_at()));
            } else {
                pstmt.setNull(6, Types.TIMESTAMP);
            }
            pstmt.setInt(7, task.get_completion_time_minutes());
            pstmt.setInt(8, task.get_assigned_user_id());
            pstmt.setInt(9, task.get_category_id());
            pstmt.setBoolean(10, task.is_store_sessions());
            pstmt.setArray(11, conn.createArrayOf("text", task.get_tags().toArray()));
            pstmt.setInt(12, task.get_id());
            pstmt.setLong(13, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating task: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTask(long id, long userId) {
        String sql = "DELETE FROM tasks WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, (int) id);
            pstmt.setLong(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting task: " + e.getMessage());
            return false;
        }
    }

    public boolean taskExists(long id, long userId) {
        String sql = "SELECT COUNT(*) FROM tasks WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, (int) id);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking task: " + e.getMessage());
        }

        return false;
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Task task = new Task(
            (int) rs.getLong("id"),
            rs.getString("title"),
            rs.getString("description"),
            Priority.valueOf(rs.getString("priority"))
        );
        task.set_status(Status.valueOf(rs.getString("status")));
        if (rs.getTimestamp("created_at") != null) {
            task.set_created_at(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("due_date") != null) {
            task.set_due_date(rs.getTimestamp("due_date").toLocalDateTime());
        }
        if (rs.getTimestamp("completed_at") != null) {
            task.set_completed_at(rs.getTimestamp("completed_at").toLocalDateTime());
        }
        task.set_completion_time_minutes(rs.getInt("completion_time_minutes"));
        task.set_assigned_user_id(rs.getInt("assigned_user_id"));
        task.set_category_id(rs.getInt("category_id"));
        task.set_store_sessions(rs.getBoolean("store_sessions"));
        return task;
    }
}
