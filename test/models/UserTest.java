package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void test_constructor_initializes_fields() {
        User user = new User(1, "Alice", "alice@example.com", Role.DEVELOPER);
        assertEquals(1, user.get_id());
        assertEquals("Alice", user.get_name());
        assertEquals("alice@example.com", user.get_email());
        assertEquals(Role.DEVELOPER, user.get_role());
        assertTrue(user.get_assigned_task_ids().isEmpty());
        assertEquals(0, user.get_tasks_completed_count());
        assertNotNull(user.get_created_at());
        assertNotNull(user.get_last_active_at());
    }

    @Test
    void test_set_name() {
        User user = new User(1, "Old", "a@b.com", Role.ADMIN);
        user.set_name("New Name");
        assertEquals("New Name", user.get_name());
    }

    @Test
    void test_set_email() {
        User user = new User(1, "A", "old@b.com", Role.ADMIN);
        user.set_email("new@b.com");
        assertEquals("new@b.com", user.get_email());
    }

    @Test
    void test_set_role() {
        User user = new User(1, "A", "a@b.com", Role.DEVELOPER);
        user.set_role(Role.MANAGER);
        assertEquals(Role.MANAGER, user.get_role());
    }

    @Test
    void test_assign_task() {
        User user = new User(1, "A", "a@b.com", Role.DEVELOPER);
        user.assign_task(100);
        assertTrue(user.has_task_assigned(100));
        assertEquals(1, user.count_assigned_tasks());
    }

    @Test
    void test_assign_duplicate_task_ignored() {
        User user = new User(1, "A", "a@b.com", Role.DEVELOPER);
        user.assign_task(100);
        user.assign_task(100);
        assertEquals(1, user.count_assigned_tasks());
    }

    @Test
    void test_unassign_task() {
        User user = new User(1, "A", "a@b.com", Role.DEVELOPER);
        user.assign_task(100);
        user.unassign_task(100);
        assertFalse(user.has_task_assigned(100));
        assertEquals(0, user.count_assigned_tasks());
    }

    @Test
    void test_increment_completed_count() {
        User user = new User(1, "A", "a@b.com", Role.DEVELOPER);
        assertEquals(0, user.get_tasks_completed_count());
        user.increment_completed_count();
        assertEquals(1, user.get_tasks_completed_count());
        user.increment_completed_count();
        assertEquals(2, user.get_tasks_completed_count());
    }

    @Test
    void test_completion_rate_zero_when_no_tasks() {
        User user = new User(1, "A", "a@b.com", Role.DEVELOPER);
        assertEquals(0.0, user.get_completion_rate(), 0.001);
    }

    @Test
    void test_completion_rate() {
        User user = new User(1, "A", "a@b.com", Role.DEVELOPER);
        user.assign_task(1);
        user.assign_task(2);
        user.increment_completed_count();
        assertEquals(50.0, user.get_completion_rate(), 0.001);
    }

    @Test
    void test_is_admin() {
        User admin = new User(1, "A", "a@b.com", Role.ADMIN);
        User dev = new User(2, "B", "b@b.com", Role.DEVELOPER);
        assertTrue(admin.is_admin());
        assertFalse(dev.is_admin());
    }

    @Test
    void test_is_manager() {
        User mgr = new User(1, "A", "a@b.com", Role.MANAGER);
        User dev = new User(2, "B", "b@b.com", Role.DEVELOPER);
        assertTrue(mgr.is_manager());
        assertFalse(dev.is_manager());
    }

    @Test
    void test_update_last_active() {
        User user = new User(1, "A", "a@b.com", Role.DEVELOPER);
        java.time.LocalDateTime before = user.get_last_active_at();
        sleep_one_millis();
        user.update_last_active();
        assertTrue(user.get_last_active_at().isAfter(before));
    }

    @Test
    void test_equals_by_id() {
        User u1 = new User(3, "A", "a@b.com", Role.DEVELOPER);
        User u2 = new User(3, "B", "b@b.com", Role.ADMIN);
        assertEquals(u1, u2);
    }

    @Test
    void test_hash_code_consistent_with_id() {
        User u1 = new User(7, "A", "a@b.com", Role.DEVELOPER);
        User u2 = new User(7, "B", "b@b.com", Role.ADMIN);
        assertEquals(u1.hashCode(), u2.hashCode());
    }

    private void sleep_one_millis() {
        try { Thread.sleep(1); } catch (InterruptedException ignored) {}
    }
}
