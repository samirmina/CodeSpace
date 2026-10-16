package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    void test_constructor_initializes_fields() {
        Task task = new Task(1, "Test Task", "Description", Priority.HIGH);
        assertEquals(1, task.get_id());
        assertEquals("Test Task", task.get_title());
        assertEquals("Description", task.get_description());
        assertEquals(Priority.HIGH, task.get_priority());
        assertEquals(Status.PENDING, task.get_status());
        assertNotNull(task.get_created_at());
        assertEquals(-1, task.get_assigned_user_id());
        assertEquals(-1, task.get_category_id());
        assertTrue(task.get_tags().isEmpty());
    }

    @Test
    void test_set_title() {
        Task task = new Task(1, "Old", "", Priority.LOW);
        task.set_title("New Title");
        assertEquals("New Title", task.get_title());
    }

    @Test
    void test_set_description() {
        Task task = new Task(1, "T", "Old", Priority.LOW);
        task.set_description("New Desc");
        assertEquals("New Desc", task.get_description());
    }

    @Test
    void test_set_priority() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.set_priority(Priority.URGENT);
        assertEquals(Priority.URGENT, task.get_priority());
    }

    @Test
    void test_set_status_to_completed_sets_completed_at() {
        Task task = new Task(1, "T", "", Priority.LOW);
        assertNull(task.get_completed_at());
        task.set_status(Status.COMPLETED);
        assertEquals(Status.COMPLETED, task.get_status());
        assertNotNull(task.get_completed_at());
    }

    @Test
    void test_set_status_non_completed_does_not_set_completed_at() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.set_status(Status.IN_PROGRESS);
        assertNull(task.get_completed_at());
    }

    @Test
    void test_add_tag() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.add_tag("bug");
        assertTrue(task.has_tag("bug"));
        assertEquals(1, task.get_tags().size());
    }

    @Test
    void test_add_tag_trimmed() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.add_tag("  bug  ");
        assertTrue(task.has_tag("bug"));
    }

    @Test
    void test_add_duplicate_tag_ignored() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.add_tag("bug");
        task.add_tag("bug");
        assertEquals(1, task.get_tags().size());
    }

    @Test
    void test_remove_tag() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.add_tag("bug");
        task.remove_tag("bug");
        assertFalse(task.has_tag("bug"));
    }

    @Test
    void test_set_assigned_user_id() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.set_assigned_user_id(42);
        assertEquals(42, task.get_assigned_user_id());
    }

    @Test
    void test_set_category_id() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.set_category_id(7);
        assertEquals(7, task.get_category_id());
    }

    @Test
    void test_is_overdue_with_no_due_date() {
        Task task = new Task(1, "T", "", Priority.LOW);
        assertFalse(task.is_overdue());
    }

    @Test
    void test_is_overdue_with_future_due_date() {
        Task task = new Task(1, "T", "", Priority.LOW);
        task.set_due_date(java.time.LocalDateTime.now().plusDays(5));
        assertFalse(task.is_overdue());
    }

    @Test
    void test_is_completed() {
        Task task = new Task(1, "T", "", Priority.LOW);
        assertFalse(task.is_completed());
        task.set_status(Status.COMPLETED);
        assertTrue(task.is_completed());
    }

    @Test
    void test_is_in_progress() {
        Task task = new Task(1, "T", "", Priority.LOW);
        assertFalse(task.is_in_progress());
        task.set_status(Status.IN_PROGRESS);
        assertTrue(task.is_in_progress());
    }

    @Test
    void test_get_days_until_due_returns_negative_one_when_null() {
        Task task = new Task(1, "T", "", Priority.LOW);
        assertEquals(-1, task.get_days_until_due());
    }

    @Test
    void test_equals_by_id() {
        Task t1 = new Task(5, "A", "", Priority.LOW);
        Task t2 = new Task(5, "B", "", Priority.HIGH);
        assertEquals(t1, t2);
    }

    @Test
    void test_hash_code_consistent_with_id() {
        Task t1 = new Task(10, "X", "", Priority.LOW);
        Task t2 = new Task(10, "Y", "", Priority.HIGH);
        assertEquals(t1.hashCode(), t2.hashCode());
    }

    @Test
    void test_to_string_contains_id_and_title() {
        Task task = new Task(3, "MyTask", "", Priority.MEDIUM);
        String str = task.toString();
        assertTrue(str.contains("3"));
        assertTrue(str.contains("MyTask"));
    }
}
