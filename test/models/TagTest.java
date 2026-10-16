package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TagTest {

    @Test
    void test_constructor_sets_fields() {
        Tag tag = new Tag(1, "bug", "#FF0000");
        assertEquals(1, tag.get_id());
        assertEquals("bug", tag.get_name());
        assertEquals("#FF0000", tag.get_color());
    }

    @Test
    void test_set_name() {
        Tag tag = new Tag(1, "bug", "#FF0000");
        tag.set_name("feature");
        assertEquals("feature", tag.get_name());
    }

    @Test
    void test_set_color() {
        Tag tag = new Tag(1, "bug", "#FF0000");
        tag.set_color("#00FF00");
        assertEquals("#00FF00", tag.get_color());
    }

    @Test
    void test_to_string() {
        Tag tag = new Tag(5, "urgent", "#FF4444");
        String str = tag.toString();
        assertTrue(str.contains("urgent"));
        assertTrue(str.contains("5"));
    }
}
