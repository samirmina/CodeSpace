package models;

import java.util.Objects;

public class Category {
    private int id;
    private String name;
    private String description;

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
    }

    public Category(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int get_id() { return id; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public int getId() { return id; }

    public String get_name() { return name; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public String getName() { return name; }

    public void set_name(String name) { this.name = name; }

    public String get_description() { return description; }

    // Standard JavaBean getter for Thymeleaf compatibility
    public String getDescription() { return description; }

    public void set_description(String description) { this.description = description; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%d] %s", id, name);
    }
}
