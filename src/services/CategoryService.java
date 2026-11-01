package services;

import models.Category;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CategoryService - manages task categories.
 * Supports both in-memory (JavaFX) and PostgreSQL-backed (web) operation modes.
 */
@Service
public class CategoryService {
    private final Map<Integer, Category> categories = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final PostgresCategoryDAO postgresCategoryDAO;
    private final boolean usePostgres;

    // No-arg constructor for JavaFX (in-memory only)
    public CategoryService() {
        this.postgresCategoryDAO = null;
        this.usePostgres = false;
    }

    // Constructor with PostgreSQL DAO for web app
    @Autowired
    public CategoryService(PostgresCategoryDAO postgresCategoryDAO) {
        this.postgresCategoryDAO = postgresCategoryDAO;
        this.usePostgres = true;
        System.out.println("ℹ CategoryService initialized with PostgreSQL DAO");
    }

    public Category create_category(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        int id = idGenerator.getAndIncrement();
        Category cat = new Category(id, name.trim(), description != null ? description.trim() : "");
        categories.put(id, cat);

        if (usePostgres && postgresCategoryDAO != null) {
            System.out.println("⚠ Use create_category(name, description, userId) for PostgreSQL mode");
        }

        System.out.println("✓ Category created: " + cat);
        return cat;
    }

    public Category create_category(String name, String description, long userId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        int id = idGenerator.getAndIncrement();
        Category cat = new Category(id, name.trim(), description != null ? description.trim() : "");
        categories.put(id, cat);

        if (usePostgres && postgresCategoryDAO != null) {
            try {
                postgresCategoryDAO.createCategory(cat, userId);
            } catch (Exception e) {
                System.err.println("⚠ Failed to persist category to PostgreSQL: " + e.getMessage());
                categories.remove(id);
                idGenerator.decrementAndGet();
                throw new RuntimeException("Failed to create category in database");
            }
        }

        System.out.println("✓ Category created: " + cat);
        return cat;
    }

    public Category get_category(int id) {
        Category cat = categories.get(id);
        if (cat == null) {
            throw new IllegalArgumentException("Category not found: " + id);
        }
        return cat;
    }

    public Optional<Category> try_get_category(int id) {
        return Optional.ofNullable(categories.get(id));
    }

    public List<Category> get_all_categories() {
        return new ArrayList<>(categories.values());
    }

    public List<Category> get_all_categories(long userId) {
        if (usePostgres && postgresCategoryDAO != null) {
            return postgresCategoryDAO.getAllCategoriesByUser(userId);
        }
        return get_all_categories();
    }

    public boolean delete_category(int id) {
        Category removed = categories.remove(id);
        if (removed != null) {
            if (usePostgres && postgresCategoryDAO != null) {
                try {
                    postgresCategoryDAO.deleteCategory(id, -1);
                } catch (Exception e) {
                    System.err.println("⚠ Failed to delete category from PostgreSQL: " + e.getMessage());
                    categories.put(id, removed);
                    return false;
                }
            }
            System.out.println("✓ Category deleted: " + removed);
            return true;
        }
        return false;
    }

    public Category update_category(int id, String name, String description) {
        Category cat = get_category(id);
        cat.set_name(name);
        cat.set_description(description);
        if (usePostgres && postgresCategoryDAO != null) {
            try {
                postgresCategoryDAO.updateCategory(cat, -1);
            } catch (Exception e) {
                System.err.println("⚠ Failed to update category in PostgreSQL: " + e.getMessage());
            }
        }
        System.out.println("✓ Category updated: " + cat);
        return cat;
    }

    public int count_categories() {
        return categories.size();
    }

    public void clear_all() {
        categories.clear();
        System.out.println("✓ All categories cleared");
    }

    public void add_category_direct(Category category) {
        categories.put(category.get_id(), category);
        if (category.get_id() >= idGenerator.get()) {
            idGenerator.set(category.get_id() + 1);
        }
    }

    public void update_id_generator(int startFrom) {
        idGenerator.set(startFrom);
    }

    public int get_current_id() {
        return idGenerator.get();
    }
}
