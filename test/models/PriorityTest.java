package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriorityTest {

    @Test
    void test_enum_values() {
        Priority[] values = Priority.values();
        assertEquals(4, values.length);
    }

    @Test
    void test_enum_contains_low() {
        assertNotNull(Priority.valueOf("LOW"));
    }

    @Test
    void test_enum_contains_medium() {
        assertNotNull(Priority.valueOf("MEDIUM"));
    }

    @Test
    void test_enum_contains_high() {
        assertNotNull(Priority.valueOf("HIGH"));
    }

    @Test
    void test_enum_contains_urgent() {
        assertNotNull(Priority.valueOf("URGENT"));
    }

    @Test
    void test_priority_ordering_lowest_first() {
        assertTrue(Priority.LOW.compareTo(Priority.MEDIUM) < 0);
    }

    @Test
    void test_priority_ordering_highest_last() {
        assertTrue(Priority.URGENT.compareTo(Priority.HIGH) > 0);
    }
}
