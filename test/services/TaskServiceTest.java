package services;

import models.Priority;
import models.Status;
import models.Task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

class TaskServiceTest {

    private TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService();
    }

    @Test
    void test_create_task() {
        Task task = service.create_task("Test", "Desc", Priority.MEDIUM);
        assertNotNull(task);
        assertEquals("Test", task.get_title());
        assertEquals(1, service.count_tasks());
    }

    @Test
    void test_create_task_throws_on_empty_title() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create_task("", "Desc", Priority.LOW));
    }

    @Test
    void test_create_task_throws_on_null_title() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create_task(null, "Desc", Priority.LOW));
    }

    @Test
    void test_get_task() {
        Task created = service.create_task("T", "D", Priority.HIGH);
        Task fetched = service.get_task(created.get_id());
        assertEquals(created.get_id(), fetched.get_id());
    }

    @Test
    void test_get_task_throws_when_not_found() {
        assertThrows(IllegalArgumentException.class, () ->
            service.get_task(999));
    }

    @Test
    void test_try_get_task_returns_optional() {
        assertTrue(service.try_get_task(999).isEmpty());
        Task t = service.create_task("T", "D", Priority.LOW);
        assertTrue(service.try_get_task(t.get_id()).isPresent());
    }

    @Test
    void test_get_all_tasks() {
        service.create_task("A", "", Priority.LOW);
        service.create_task("B", "", Priority.MEDIUM);
        assertEquals(2, service.get_all_tasks().size());
    }

    @Test
    void test_update_task_status() {
        Task t = service.create_task("T", "", Priority.LOW);
        service.update_task_status(t.get_id(), Status.IN_PROGRESS);
        assertEquals(Status.IN_PROGRESS, service.get_task(t.get_id()).get_status());
    }

    @Test
    void test_update_task_priority() {
        Task t = service.create_task("T", "", Priority.LOW);
        service.update_task_priority(t.get_id(), Priority.URGENT);
        assertEquals(Priority.URGENT, service.get_task(t.get_id()).get_priority());
    }

    @Test
    void test_update_task_description() {
        Task t = service.create_task("T", "Old", Priority.LOW);
        service.update_task_description(t.get_id(), "New Desc");
        assertEquals("New Desc", service.get_task(t.get_id()).get_description());
    }

    @Test
    void test_update_task_title() {
        Task t = service.create_task("Old", "", Priority.LOW);
        service.update_task_title(t.get_id(), "New Title");
        assertEquals("New Title", service.get_task(t.get_id()).get_title());
    }

    @Test
    void test_update_task_due_date() {
        Task t = service.create_task("T", "", Priority.LOW);
        LocalDateTime due = LocalDateTime.now().plusDays(3);
        service.update_task_due_date(t.get_id(), due);
        assertEquals(due, service.get_task(t.get_id()).get_due_date());
    }

    @Test
    void test_delete_task() {
        Task t = service.create_task("T", "", Priority.LOW);
        assertTrue(service.delete_task(t.get_id()));
        assertEquals(0, service.count_tasks());
    }

    @Test
    void test_delete_task_returns_false_when_not_found() {
        assertFalse(service.delete_task(999));
    }

    @Test
    void test_filter_by_status() {
        Task t1 = service.create_task("A", "", Priority.LOW);
        Task t2 = service.create_task("B", "", Priority.LOW);
        service.update_task_status(t1.get_id(), Status.COMPLETED);
        List<Task> completed = service.filter_by_status(Status.COMPLETED);
        assertEquals(1, completed.size());
        assertEquals(t1.get_id(), completed.get(0).get_id());
    }

    @Test
    void test_filter_by_priority() {
        service.create_task("A", "", Priority.HIGH);
        service.create_task("B", "", Priority.LOW);
        List<Task> high = service.filter_by_priority(Priority.HIGH);
        assertEquals(1, high.size());
    }

    @Test
    void test_search_by_title() {
        service.create_task("Alpha", "", Priority.LOW);
        service.create_task("Beta", "", Priority.LOW);
        service.create_task("Gamma", "", Priority.LOW);
        List<Task> results = service.search_by_title("al");
        assertEquals(1, results.size());
    }

    @Test
    void test_filter_incomplete_tasks() {
        Task t1 = service.create_task("A", "", Priority.LOW);
        Task t2 = service.create_task("B", "", Priority.LOW);
        service.update_task_status(t1.get_id(), Status.COMPLETED);
        List<Task> incomplete = service.filter_incomplete_tasks();
        assertEquals(1, incomplete.size());
        assertEquals(t2.get_id(), incomplete.get(0).get_id());
    }

    @Test
    void test_get_urgent_incomplete_tasks() {
        Task t1 = service.create_task("A", "", Priority.URGENT);
        Task t2 = service.create_task("B", "", Priority.URGENT);
        service.update_task_status(t2.get_id(), Status.COMPLETED);
        List<Task> urgent = service.get_urgent_incomplete_tasks();
        assertEquals(1, urgent.size());
        assertEquals(t1.get_id(), urgent.get(0).get_id());
    }

    @Test
    void test_sort_by_priority() {
        service.create_task("A", "", Priority.LOW);
        service.create_task("B", "", Priority.URGENT);
        service.create_task("C", "", Priority.HIGH);
        List<Task> sorted = service.sort_by_priority();
        assertEquals(Priority.LOW, sorted.get(0).get_priority());
        assertEquals(Priority.URGENT, sorted.get(sorted.size() - 1).get_priority());
    }

    @Test
    void test_sort_by_creation_date() {
        Task a = service.create_task("A", "", Priority.LOW);
        Task b = service.create_task("B", "", Priority.LOW);
        List<Task> sorted = service.sort_by_creation_date();
        assertTrue(sorted.get(0).get_created_at().compareTo(sorted.get(1).get_created_at()) <= 0);
    }

    @Test
    void test_get_statistics() {
        Task t1 = service.create_task("A", "", Priority.LOW);
        Task t2 = service.create_task("B", "", Priority.LOW);
        service.update_task_status(t1.get_id(), Status.COMPLETED);
        var stats = service.get_statistics();
        assertEquals(1, stats.get("COMPLETED").longValue());
        assertEquals(1, stats.get("PENDING").longValue());
    }

    @Test
    void test_get_completion_rate() {
        assertEquals(0.0, service.get_completion_rate(), 0.001);
        service.create_task("A", "", Priority.LOW);
        assertEquals(0.0, service.get_completion_rate(), 0.001);
        Task t = service.create_task("B", "", Priority.LOW);
        service.update_task_status(t.get_id(), Status.COMPLETED);
        assertEquals(50.0, service.get_completion_rate(), 0.001);
    }

    @Test
    void test_count_completed_tasks() {
        Task t = service.create_task("A", "", Priority.LOW);
        assertEquals(0, service.count_completed_tasks());
        service.update_task_status(t.get_id(), Status.COMPLETED);
        assertEquals(1, service.count_completed_tasks());
    }

    @Test
    void test_filter_by_tag() {
        Task t = service.create_task("A", "", Priority.LOW);
        t.add_tag("bug");
        List<Task> tagged = service.filter_by_tag("bug");
        assertEquals(1, tagged.size());
    }

    @Test
    void test_filter_by_user() {
        Task t = service.create_task("A", "", Priority.LOW);
        t.set_assigned_user_id(5);
        List<Task> assigned = service.filter_by_user(5);
        assertEquals(1, assigned.size());
    }

    @Test
    void test_get_all_tags() {
        Task t1 = service.create_task("A", "", Priority.LOW);
        Task t2 = service.create_task("B", "", Priority.LOW);
        t1.add_tag("bug");
        t1.add_tag("ui");
        t2.add_tag("bug");
        List<String> allTags = service.get_all_tags();
        assertEquals(2, allTags.size());
        assertTrue(allTags.contains("bug"));
        assertTrue(allTags.contains("ui"));
    }

    @Test
    void test_clear_all() {
        service.create_task("A", "", Priority.LOW);
        service.create_task("B", "", Priority.LOW);
        service.clear_all();
        assertEquals(0, service.count_tasks());
    }

    @Test
    void test_add_task_direct() {
        Task task = new Task(100, "Direct", "", Priority.MEDIUM);
        service.add_task_direct(task);
        assertEquals(100, service.get_task(100).get_id());
    }

    @Test
    void test_update_id_generator() {
        service.update_id_generator(50);
        Task t = service.create_task("A", "", Priority.LOW);
        assertEquals(50, t.get_id());
    }
}
