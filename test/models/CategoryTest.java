package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    void test_constructor_sets_fields() {
        Category cat = new Category(1, "Work", "Work related tasks");
        assertEquals(1, cat.get_id());
        assertEquals("Work", cat.get_name());
        assertEquals("Work related tasks", cat.get_description());
    }

    @Test
    void test_set_name() {
        Category cat = new Category(1, "Work", "");
        cat.set_name("Personal");
        assertEquals("Personal", cat.get_name());
    }

    @Test
    void test_set_description() {
        Category cat = new Category(1, "Work", "Old desc");
        cat.set_description("New desc");
        assertEquals("New desc", cat.get_description());
    }

    @Test
    void test_to_string() {
        Category cat = new Category(2, "Personal", "Personal tasks");
        String str = cat.toString();
        assertTrue(str.contains("Personal"));
        assertTrue(str.contains("2"));
    }
}
