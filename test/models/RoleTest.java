package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @Test
    void test_enum_values() {
        Role[] values = Role.values();
        assertEquals(3, values.length);
    }

    @Test
    void test_contains_admin() {
        assertNotNull(Role.valueOf("ADMIN"));
    }

    @Test
    void test_contains_manager() {
        assertNotNull(Role.valueOf("MANAGER"));
    }

    @Test
    void test_contains_developer() {
        assertNotNull(Role.valueOf("DEVELOPER"));
    }
}
