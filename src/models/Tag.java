package models;

import java.util.Objects;

public class Tag {
    private int id;
    private String name;
    private String color;

    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
        this.color = "#3498DB";
    }

    public Tag(int id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public int get_id() { return id; }

    public String get_name() { return name; }

    public void set_name(String name) { this.name = name; }

    public String get_color() { return color; }

    public void set_color(String color) { this.color = color; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return id == tag.id;
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
