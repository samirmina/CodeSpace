package services;

import models.Pomodoro;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PomodoroServiceTest {

    private PomodoroService service;

    @BeforeEach
    void setUp() {
        service = new PomodoroService();
    }

    @Test
    void test_create_pomodoro_with_default_duration() {
        Pomodoro p = service.create_pomodoro(1, "Task A");
        assertNotNull(p);
        assertEquals(1, p.get_task_id());
        assertEquals("Task A", p.get_task_name());
        assertEquals(25, p.get_duration_minutes());
        assertEquals(1, service.count_pomodoros());
    }

    @Test
    void test_create_pomodoro_with_custom_duration() {
        Pomodoro p = service.create_pomodoro(1, "Task A", 45);
        assertEquals(45, p.get_duration_minutes());
    }

    @Test
    void test_get_pomodoro() {
        Pomodoro created = service.create_pomodoro(1, "T");
        Pomodoro fetched = service.get_pomodoro(created.get_id());
        assertEquals(created.get_id(), fetched.get_id());
    }

    @Test
    void test_get_pomodoro_throws_when_not_found() {
        assertThrows(IllegalArgumentException.class, () ->
            service.get_pomodoro(999));
    }

    @Test
    void test_try_get_pomodoro_returns_optional() {
        assertTrue(service.try_get_pomodoro(999).isEmpty());
        Pomodoro p = service.create_pomodoro(1, "T");
        assertTrue(service.try_get_pomodoro(p.get_id()).isPresent());
    }

    @Test
    void test_get_all_pomodoros() {
        service.create_pomodoro(1, "A");
        service.create_pomodoro(2, "B");
        assertEquals(2, service.get_all_pomodoros().size());
    }

    @Test
    void test_get_or_create_pomodoro_creates_new() {
        Pomodoro p = service.get_or_create_pomodoro(1, "New Task");
        assertNotNull(p);
        assertEquals(1, p.get_task_id());
        assertEquals(1, service.count_pomodoros());
    }

    @Test
    void test_get_or_create_pomodoro_returns_existing() {
        service.create_pomodoro(1, "Existing");
        Pomodoro p = service.get_or_create_pomodoro(1, "Existing");
        assertEquals(1, service.count_pomodoros());
    }

    @Test
    void test_increment_session() {
        Pomodoro p = service.create_pomodoro(1, "T");
        service.increment_session(p.get_id());
        assertEquals(1, service.get_pomodoro(p.get_id()).get_completed_sessions());
    }

    @Test
    void test_reset_sessions() {
        Pomodoro p = service.create_pomodoro(1, "T");
        service.increment_session(p.get_id());
        service.increment_session(p.get_id());
        service.reset_sessions(p.get_id());
        assertEquals(0, service.get_pomodoro(p.get_id()).get_completed_sessions());
    }

    @Test
    void test_start_session() {
        Pomodoro p = service.create_pomodoro(1, "T");
        service.start_session(p.get_id());
        assertTrue(service.get_pomodoro(p.get_id()).is_active());
    }

    @Test
    void test_stop_session() {
        Pomodoro p = service.create_pomodoro(1, "T");
        service.start_session(p.get_id());
        service.stop_session(p.get_id());
        assertFalse(service.get_pomodoro(p.get_id()).is_active());
    }

    @Test
    void test_delete_pomodoro() {
        Pomodoro p = service.create_pomodoro(1, "T");
        assertTrue(service.delete_pomodoro(p.get_id()));
        assertEquals(0, service.count_pomodoros());
    }

    @Test
    void test_delete_pomodoro_returns_false_when_not_found() {
        assertFalse(service.delete_pomodoro(999));
    }

    @Test
    void test_get_total_sessions() {
        Pomodoro a = service.create_pomodoro(1, "A");
        Pomodoro b = service.create_pomodoro(2, "B");
        service.increment_session(a.get_id());
        service.increment_session(a.get_id());
        service.increment_session(b.get_id());
        assertEquals(3, service.get_total_sessions());
    }

    @Test
    void test_get_total_focus_time_minutes() {
        Pomodoro a = service.create_pomodoro(1, "A", 25);
        Pomodoro b = service.create_pomodoro(2, "B", 15);
        service.increment_session(a.get_id());
        service.increment_session(a.get_id());
        service.increment_session(b.get_id());
        assertEquals(65, service.get_total_focus_time_minutes());
    }

    @Test
    void test_get_sessions_for_task() {
        Pomodoro a = service.create_pomodoro(1, "A", 25);
        service.increment_session(a.get_id());
        assertEquals(25, service.get_sessions_for_task(1));
        assertEquals(0, service.get_sessions_for_task(999));
    }

    @Test
    void test_get_pomodoro_by_task_id() {
        service.create_pomodoro(1, "A");
        assertTrue(service.get_pomodoro_by_task_id(1).isPresent());
        assertTrue(service.get_pomodoro_by_task_id(999).isEmpty());
    }

    @Test
    void test_get_active_pomodoros() {
        Pomodoro a = service.create_pomodoro(1, "A");
        Pomodoro b = service.create_pomodoro(2, "B");
        service.start_session(a.get_id());
        assertEquals(1, service.get_active_pomodoros().size());
    }

    @Test
    void test_get_pomodoros_with_sessions() {
        Pomodoro a = service.create_pomodoro(1, "A");
        Pomodoro b = service.create_pomodoro(2, "B");
        service.increment_session(a.get_id());
        assertEquals(1, service.get_pomodoros_with_sessions().size());
    }

    @Test
    void test_get_average_sessions_per_task() {
        assertEquals(0.0, service.get_average_sessions_per_task(), 0.001);
        Pomodoro a = service.create_pomodoro(1, "A");
        service.increment_session(a.get_id());
        service.increment_session(a.get_id());
        assertEquals(2.0, service.get_average_sessions_per_task(), 0.001);
    }

    @Test
    void test_get_most_productive_task() {
        assertNull(service.get_most_productive_task());
        Pomodoro a = service.create_pomodoro(1, "Task A");
        Pomodoro b = service.create_pomodoro(2, "Task B");
        service.increment_session(a.get_id());
        service.increment_session(a.get_id());
        service.increment_session(b.get_id());
        assertEquals("Task A", service.get_most_productive_task());
    }

    @Test
    void test_set_default_duration() {
        service.set_default_duration(30);
        assertEquals(30, service.get_default_duration());
        Pomodoro p = service.create_pomodoro(1, "T");
        assertEquals(30, p.get_duration_minutes());
    }

    @Test
    void test_clear_all() {
        service.create_pomodoro(1, "A");
        service.create_pomodoro(2, "B");
        service.clear_all();
        assertEquals(0, service.count_pomodoros());
    }

    @Test
    void test_add_pomodoro_direct() {
        Pomodoro p = new Pomodoro(100, 1, "Direct", 25);
        service.add_pomodoro_direct(p);
        assertEquals(100, service.get_pomodoro(100).get_id());
    }

    @Test
    void test_update_id_generator() {
        service.update_id_generator(50);
        Pomodoro p = service.create_pomodoro(1, "T");
        assertEquals(50, p.get_id());
    }
}
