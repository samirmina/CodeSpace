package services;

import models.Category;

import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PostgresCategoryDAO - PostgreSQL-backed category repository with user isolation.
 * All queries are filtered by user_id to ensure data isolation between clients.
 */
@Service
public class PostgresCategoryDAO {
    private final String url;
    private final String user;
    private final String password;

    public PostgresCategoryDAO() {
        this(Config.getDbUrl(), Config.getDbUser(), Config.getDbPassword());
    }

    public PostgresCategoryDAO(String url, String user, String password) {
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

    public List<Category> getAllCategoriesByUser(long userId) {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name, description FROM categories WHERE user_id = ? ORDER BY id";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Category category = new Category(
                    (int) rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description")
                );
                categories.add(category);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching categories from PostgreSQL: " + e.getMessage());
        }

        return categories;
    }

    public Optional<Category> getCategoryById(long id, long userId) {
        String sql = "SELECT id, name, description FROM categories WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new Category(
                    (int) rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching category: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean createCategory(Category category, long userId) {
        String sql = "INSERT INTO categories (user_id, name, description) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setLong(1, userId);
            pstmt.setString(2, category.get_name());
            pstmt.setString(3, category.get_description());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        // Category doesn't have a set_id, but we can note it
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating category: " + e.getMessage());
        }

        return false;
    }

    public boolean updateCategory(Category category, long userId) {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.get_name());
            pstmt.setString(2, category.get_description());
            pstmt.setInt(3, category.get_id());
            pstmt.setLong(4, userId);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCategory(long id, long userId) {
        String sql = "DELETE FROM categories WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, (int) id);
            pstmt.setLong(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
            return false;
        }
    }

    public boolean categoryExists(long id, long userId) {
        String sql = "SELECT COUNT(*) FROM categories WHERE id = ? AND user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, (int) id);
            pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking category: " + e.getMessage());
        }

        return false;
    }
}
