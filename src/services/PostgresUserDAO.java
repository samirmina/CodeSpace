package services;

import models.Role;
import models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgresUserDAO - PostgreSQL-backed user repository.
 * Provides CRUD operations directly against the chronos database.
 * Reads connection parameters from Config (config.properties).
 */
public class PostgresUserDAO {
    private final String url;
    private final String user;
    private final String password;

    public PostgresUserDAO() {
        this(Config.getDbUrl(), Config.getDbUser(), Config.getDbPassword());
    }

    public PostgresUserDAO(String url, String user, String password) {
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

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email, role, created_at, updated_at FROM users ORDER BY id";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                User user = new User(
                    (int) rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    models.Role.valueOf(rs.getString("role"))
                );
                user.set_created_at(rs.getTimestamp("created_at").toLocalDateTime());
                user.set_last_active_at(rs.getTimestamp("updated_at").toLocalDateTime());
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching users from PostgreSQL: " + e.getMessage());
        }

        return users;
    }

    public User getUserById(long id) {
        String sql = "SELECT id, name, email, role, created_at, updated_at FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                    (int) rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    models.Role.valueOf(rs.getString("role"))
                );
                user.set_created_at(rs.getTimestamp("created_at").toLocalDateTime());
                user.set_last_active_at(rs.getTimestamp("updated_at").toLocalDateTime());
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }

        return null;
    }

    public User getUserByEmail(String email) {
        String sql = "SELECT id, name, email, role, created_at, updated_at FROM users WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User(
                    (int) rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    models.Role.valueOf(rs.getString("role"))
                );
                user.set_created_at(rs.getTimestamp("created_at").toLocalDateTime());
                user.set_last_active_at(rs.getTimestamp("updated_at").toLocalDateTime());
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user: " + e.getMessage());
        }

        return null;
    }

    public boolean createUser(User user) {
        String sql = "INSERT INTO users (name, email, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.get_name());
            pstmt.setString(2, user.get_email());
            pstmt.setString(3, user.get_role().name());
            pstmt.setTimestamp(4, Timestamp.valueOf(user.get_created_at()));
            pstmt.setTimestamp(5, Timestamp.valueOf(user.get_last_active_at()));

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, role = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.get_name());
            pstmt.setString(2, user.get_email());
            pstmt.setString(3, user.get_role().name());
            pstmt.setTimestamp(4, Timestamp.valueOf(java.time.LocalDateTime.now()));
            pstmt.setLong(5, user.get_id());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    public boolean userExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking user: " + e.getMessage());
        }

        return false;
    }
}
