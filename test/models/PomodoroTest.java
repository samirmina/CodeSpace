package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PomodoroTest {

    @Test
    void test_constructor_initializes_fields() {
        Pomodoro p = new Pomodoro(1, 10, "My Task", 25);
        assertEquals(1, p.get_id());
        assertEquals(10, p.get_task_id());
        assertEquals("My Task", p.get_task_name());
        assertEquals(25, p.get_duration_minutes());
        assertEquals(0, p.get_completed_sessions());
        assertNull(p.get_last_session_date());
        assertFalse(p.is_active());
        assertNull(p.get_started_at());
        assertTrue(p.get_session_history().isEmpty());
    }

    @Test
    void test_set_task_id() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        p.set_task_id(20);
        assertEquals(20, p.get_task_id());
    }

    @Test
    void test_set_task_name() {
        Pomodoro p = new Pomodoro(1, 10, "Old", 25);
        p.set_task_name("New Name");
        assertEquals("New Name", p.get_task_name());
    }

    @Test
    void test_set_duration_minutes() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        p.set_duration_minutes(30);
        assertEquals(30, p.get_duration_minutes());
    }

    @Test
    void test_increment_sessions() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        p.increment_sessions(25);
        assertEquals(1, p.get_completed_sessions());
        assertNotNull(p.get_last_session_date());
        assertEquals(1, p.get_session_history().size());
    }

    @Test
    void test_multiple_increments() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        p.increment_sessions(25);
        p.increment_sessions(25);
        p.increment_sessions(25);
        assertEquals(3, p.get_completed_sessions());
        assertEquals(3, p.get_session_history().size());
    }

    @Test
    void test_reset_sessions() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        p.increment_sessions(25);
        p.increment_sessions(25);
        p.reset_sessions();
        assertEquals(0, p.get_completed_sessions());
        assertNull(p.get_last_session_date());
        assertTrue(p.get_session_history().isEmpty());
    }

    @Test
    void test_set_active_stores_start_time() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        p.set_active(true);
        assertTrue(p.is_active());
        assertNotNull(p.get_started_at());
    }

    @Test
    void test_set_active_false() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        p.set_active(true);
        p.set_active(false);
        assertFalse(p.is_active());
    }

    @Test
    void test_get_elapsed_time_returns_zero_when_not_active() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        assertEquals(0, p.get_elapsed_time_seconds());
    }

    @Test
    void test_get_remaining_time_equals_duration_when_not_started() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        assertEquals(25 * 60, p.get_remaining_time_seconds());
    }

    @Test
    void test_is_session_complete_returns_false_when_not_started() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        assertFalse(p.is_session_complete());
    }

    @Test
    void test_get_total_focus_time_minutes() {
        Pomodoro p = new Pomodoro(1, 10, "T", 25);
        assertEquals(0, p.get_total_focus_time_minutes());
        p.increment_sessions(25);
        assertEquals(25, p.get_total_focus_time_minutes());
        p.increment_sessions(25);
        assertEquals(50, p.get_total_focus_time_minutes());
    }

    @Test
    void test_to_string() {
        Pomodoro p = new Pomodoro(5, 10, "Coding", 25);
        String str = p.toString();
        assertTrue(str.contains("5"));
        assertTrue(str.contains("Coding"));
    }
}
