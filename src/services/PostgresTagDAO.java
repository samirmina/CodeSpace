package services;

import models.Tag;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgresTagDAO - PostgreSQL-backed tag repository with user isolation.
 * All queries are filtered by user_id to ensure data isolation between clients.
 */
@Service
public class PostgresTagDAO {
    private final String url;
    private final String user;
    private final String password;

    public PostgresTagDAO() {
        this(Config.getDbUrl(), Config.getDbUser(), Config.getDbPassword());
    }

    public PostgresTagDAO(String url, String user, String password) {
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

    public List<Tag> getAllTagsByUser(long userId) {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT id, name, color FROM tags WHERE user_id = ? ORDER BY id";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Tag tag = new Tag(
                    (int) rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("color")
                );
                tags.add(tag);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tags from PostgreSQL: " + e.getMessage());
        }

        return tags;
    }

    public Optional<Tag> getTagById(long id, long userId) {
        String sql = "SELECT id, name, color FROM tags WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Tag(
                    (int) rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("color")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tag: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean createTag(Tag tag, long userId) {
        String sql = "INSERT INTO tags (user_id, name, color) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, tag.get_name());
            pstmt.setString(3, tag.get_color());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error creating tag: " + e.getMessage());
            return false;
        }
    }

    public boolean updateTag(Tag tag, long userId) {
        String sql = "UPDATE tags SET name = ?, color = ? WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tag.get_name());
            pstmt.setString(2, tag.get_color());
            pstmt.setInt(3, tag.get_id());
            pstmt.setLong(4, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating tag: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteTag(long id, long userId) {
        String sql = "DELETE FROM tags WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, (int) id);
            pstmt.setLong(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting tag: " + e.getMessage());
            return false;
        }
    }

    public boolean tagExists(long id, long userId) {
        String sql = "SELECT COUNT(*) FROM tags WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, (int) id);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking tag: " + e.getMessage());
        }

        return false;
    }
}
