package services;

import models.Category;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

/**
 * CategoryService - manages task categories
 */
@Service
public class CategoryService {
    private final Map<Integer, Category> categories = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public Category create_category(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        int id = idGenerator.getAndIncrement();
        Category cat = new Category(id, name.trim(), description != null ? description.trim() : "");
        categories.put(id, cat);
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

    public boolean delete_category(int id) {
        Category removed = categories.remove(id);
        if (removed != null) {
            System.out.println("✓ Category deleted: " + removed);
            return true;
        }
        return false;
    }

    public Category update_category(int id, String name, String description) {
        Category cat = get_category(id);
        cat.set_name(name);
        cat.set_description(description);
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