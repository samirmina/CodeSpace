package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StatusTest {

    @Test
    void test_enum_values() {
        Status[] values = Status.values();
        assertEquals(5, values.length);
    }

    @Test
    void test_contains_pending() {
        assertNotNull(Status.valueOf("PENDING"));
    }

    @Test
    void test_contains_in_progress() {
        assertNotNull(Status.valueOf("IN_PROGRESS"));
    }

    @Test
    void test_contains_completed() {
        assertNotNull(Status.valueOf("COMPLETED"));
    }

    @Test
    void test_contains_cancelled() {
        assertNotNull(Status.valueOf("CANCELLED"));
    }

    @Test
    void test_contains_dummy() {
        assertNotNull(Status.valueOf("DUMMY"));
    }
}
