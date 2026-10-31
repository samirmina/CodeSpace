package services;

import models.Tag;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.paint.Color;

import org.springframework.stereotype.Service;

/**
 * TagService - manages task tags
 */
@Service
public class TagService {
    private final Map<Integer, Tag> tags = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public Tag create_tag(String name, String color) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name cannot be empty");
        }
        int id = idGenerator.getAndIncrement();
        String tagColor = (color != null && !color.isEmpty()) ? color : "#3498DB";
        Tag tag = new Tag(id, name.trim(), tagColor);
        tags.put(id, tag);
        System.out.println("✓ Tag created: " + tag);
        return tag;
    }

    public Tag create_tag(String name) {
        return create_tag(name, "#3498DB");
    }

    public Tag get_tag(int id) {
        Tag tag = tags.get(id);
        if (tag == null) {
            throw new IllegalArgumentException("Tag not found: " + id);
        }
        return tag;
    }

    public Optional<Tag> try_get_tag(int id) {
        return Optional.ofNullable(tags.get(id));
    }

    public List<Tag> get_all_tags() {
        return new ArrayList<>(tags.values());
    }

    public boolean delete_tag(int id) {
        Tag removed = tags.remove(id);
        if (removed != null) {
            System.out.println("✓ Tag deleted: " + removed);
            return true;
        }
        return false;
    }

    public Tag update_tag(int id, String name, String color) {
        Tag tag = get_tag(id);
        tag.set_name(name);
        tag.set_color(color);
        System.out.println("✓ Tag updated: " + tag);
        return tag;
    }

    public int count_tags() {
        return tags.size();
    }

    public void clear_all() {
        tags.clear();
        System.out.println("✓ All tags cleared");
    }

    public void add_tag_direct(Tag tag) {
        tags.put(tag.get_id(), tag);
        if (tag.get_id() >= idGenerator.get()) {
            idGenerator.set(tag.get_id() + 1);
        }
    }

    public void update_id_generator(int startFrom) {
        idGenerator.set(startFrom);
    }

    public int get_current_id() {
        return idGenerator.get();
    }
}