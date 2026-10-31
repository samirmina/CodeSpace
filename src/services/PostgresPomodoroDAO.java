package services;

import models.Pomodoro;
import models.Task;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgresPomodoroDAO - PostgreSQL-backed pomodoro repository with user isolation.
 * All queries are filtered by user_id to ensure data isolation between clients.
 */
@Service
public class PostgresPomodoroDAO {
    private final String url;
    private final String user;
    private final String password;

    public PostgresPomodoroDAO() {
        this(Config.getDbUrl(), Config.getDbUser(), Config.getDbPassword());
    }

    public PostgresPomodoroDAO(String url, String user, String password) {
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

    public List<Pomodoro> getAllPomodorosByUser(long userId) {
        List<Pomodoro> pomodoros = new ArrayList<>();
        String sql = "SELECT id, task_id, task_name, duration_minutes, completed_sessions, " +
                     "last_session_date, is_active, started_at FROM pomodoros WHERE user_id = ? ORDER BY id";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Pomodoro p = new Pomodoro(
                    (int) rs.getLong("id"),
                    (int) rs.getLong("task_id"),
                    rs.getString("task_name"),
                    rs.getInt("duration_minutes")
                );
                p.set_completed_sessions(rs.getInt("completed_sessions"));
                if (rs.getTimestamp("last_session_date") != null) {
                    // Use reflection or direct field access via setter
                    p.set_last_session_date(rs.getTimestamp("last_session_date").toLocalDateTime());
                }
                p.set_active(rs.getBoolean("is_active"));
                if (rs.getTimestamp("started_at") != null) {
                    p.set_started_at(rs.getTimestamp("started_at").toLocalDateTime());
                }
                pomodoros.add(p);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pomodoros from PostgreSQL: " + e.getMessage());
        }

        return pomodoros;
    }

    public Optional<Pomodoro> getPomodoroById(long id, long userId) {
        String sql = "SELECT * FROM pomodoros WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapPomodoro(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pomodoro: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean createPomodoro(Pomodoro pomodoro, long userId) {
        String sql = "INSERT INTO pomodoros (user_id, task_id, task_name, duration_minutes, " +
                     "completed_sessions, last_session_date, is_active, started_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, userId);
            pstmt.setInt(2, pomodoro.get_task_id());
            pstmt.setString(3, pomodoro.get_task_name());
            pstmt.setInt(4, pomodoro.get_duration_minutes());
            pstmt.setInt(5, pomodoro.get_completed_sessions());
            if (pomodoro.get_last_session_date() != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(pomodoro.get_last_session_date()));
            } else {
                pstmt.setNull(6, Types.TIMESTAMP);
            }
            pstmt.setBoolean(7, pomodoro.is_active());
            if (pomodoro.get_started_at() != null) {
                pstmt.setTimestamp(8, Timestamp.valueOf(pomodoro.get_started_at()));
            } else {
                pstmt.setNull(8, Types.TIMESTAMP);
            }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        pomodoro.set_id((int) rs.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating pomodoro: " + e.getMessage());
        }

        return false;
    }

    public boolean updatePomodoro(Pomodoro pomodoro, long userId) {
        String sql = "UPDATE pomodoros SET task_id = ?, task_name = ?, duration_minutes = ?, " +
                     "completed_sessions = ?, last_session_date = ?, is_active = ?, started_at = ? " +
                     "WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, pomodoro.get_task_id());
            pstmt.setString(2, pomodoro.get_task_name());
            pstmt.setInt(3, pomodoro.get_duration_minutes());
            pstmt.setInt(4, pomodoro.get_completed_sessions());
            if (pomodoro.get_last_session_date() != null) {
                pstmt.setTimestamp(5, Timestamp.valueOf(pomodoro.get_last_session_date()));
            } else {
                pstmt.setNull(5, Types.TIMESTAMP);
            }
            pstmt.setBoolean(6, pomodoro.is_active());
            if (pomodoro.get_started_at() != null) {
                pstmt.setTimestamp(7, Timestamp.valueOf(pomodoro.get_started_at()));
            } else {
                pstmt.setNull(7, Types.TIMESTAMP);
            }
            pstmt.setInt(8, pomodoro.get_id());
            pstmt.setLong(9, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating pomodoro: " + e.getMessage());
            return false;
        }
    }

    public boolean deletePomodoro(long id, long userId) {
        String sql = "DELETE FROM pomodoros WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, (int) id);
            pstmt.setLong(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting pomodoro: " + e.getMessage());
            return false;
        }
    }

    public boolean deletePomodorosForTask(long taskId, long userId) {
        String sql = "DELETE FROM pomodoros WHERE task_id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, (int) taskId);
            pstmt.setLong(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting pomodoros for task: " + e.getMessage());
            return false;
        }
    }

    private Pomodoro mapPomodoro(ResultSet rs) throws SQLException {
        Pomodoro p = new Pomodoro(
            (int) rs.getLong("id"),
            (int) rs.getLong("task_id"),
            rs.getString("task_name"),
            rs.getInt("duration_minutes")
        );
        p.set_completed_sessions(rs.getInt("completed_sessions"));
        if (rs.getTimestamp("last_session_date") != null) {
            p.set_last_session_date(rs.getTimestamp("last_session_date").toLocalDateTime());
        }
        p.set_active(rs.getBoolean("is_active"));
        if (rs.getTimestamp("started_at") != null) {
            p.set_started_at(rs.getTimestamp("started_at").toLocalDateTime());
        }
        return p;
    }
}
