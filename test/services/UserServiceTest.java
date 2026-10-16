package services;

import models.Role;
import models.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class UserServiceTest {

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService();
    }

    @Test
    void test_create_user() {
        User user = service.create_user("Alice", "alice@example.com", Role.DEVELOPER);
        assertNotNull(user);
        assertEquals("Alice", user.get_name());
        assertEquals(1, service.count_users());
    }

    @Test
    void test_create_user_throws_on_empty_name() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create_user("", "a@b.com", Role.DEVELOPER));
    }

    @Test
    void test_create_user_throws_on_null_name() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create_user(null, "a@b.com", Role.DEVELOPER));
    }

    @Test
    void test_create_user_throws_on_invalid_email() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create_user("A", "invalid", Role.DEVELOPER));
    }

    @Test
    void test_create_user_throws_on_null_email() {
        assertThrows(IllegalArgumentException.class, () ->
            service.create_user("A", null, Role.DEVELOPER));
    }

    @Test
    void test_get_user() {
        User created = service.create_user("A", "a@b.com", Role.DEVELOPER);
        User fetched = service.get_user(created.get_id());
        assertEquals(created.get_id(), fetched.get_id());
    }

    @Test
    void test_get_user_throws_when_not_found() {
        assertThrows(IllegalArgumentException.class, () ->
            service.get_user(999));
    }

    @Test
    void test_try_get_user_returns_optional() {
        assertTrue(service.try_get_user(999).isEmpty());
        User u = service.create_user("A", "a@b.com", Role.DEVELOPER);
        assertTrue(service.try_get_user(u.get_id()).isPresent());
    }

    @Test
    void test_get_all_users() {
        service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.create_user("B", "b@b.com", Role.MANAGER);
        assertEquals(2, service.get_all_users().size());
    }

    @Test
    void test_filter_by_role() {
        service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.create_user("B", "b@b.com", Role.ADMIN);
        List<User> admins = service.filter_by_role(Role.ADMIN);
        assertEquals(1, admins.size());
    }

    @Test
    void test_filter_by_email_domain() {
        service.create_user("A", "a@company.com", Role.DEVELOPER);
        service.create_user("B", "b@gmail.com", Role.DEVELOPER);
        List<User> company = service.filter_by_email_domain("company.com");
        assertEquals(1, company.size());
    }

    @Test
    void test_search_by_name() {
        service.create_user("Alice", "a@b.com", Role.DEVELOPER);
        service.create_user("Bob", "b@b.com", Role.DEVELOPER);
        List<User> results = service.search_by_name("ali");
        assertEquals(1, results.size());
    }

    @Test
    void test_delete_user() {
        User u = service.create_user("A", "a@b.com", Role.DEVELOPER);
        assertTrue(service.delete_user(u.get_id()));
        assertEquals(0, service.count_users());
    }

    @Test
    void test_delete_user_returns_false_when_not_found() {
        assertFalse(service.delete_user(999));
    }

    @Test
    void test_update_user_name() {
        User u = service.create_user("Old", "a@b.com", Role.DEVELOPER);
        service.update_user_name(u.get_id(), "New");
        assertEquals("New", service.get_user(u.get_id()).get_name());
    }

    @Test
    void test_update_user_email() {
        User u = service.create_user("A", "old@b.com", Role.DEVELOPER);
        service.update_user_email(u.get_id(), "new@b.com");
        assertEquals("new@b.com", service.get_user(u.get_id()).get_email());
    }

    @Test
    void test_update_user_role() {
        User u = service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.update_user_role(u.get_id(), Role.MANAGER);
        assertEquals(Role.MANAGER, service.get_user(u.get_id()).get_role());
    }

    @Test
    void test_assign_task_to_user() {
        User u = service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.assign_task_to_user(u.get_id(), 42);
        assertTrue(service.get_user(u.get_id()).has_task_assigned(42));
    }

    @Test
    void test_unassign_task_from_user() {
        User u = service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.assign_task_to_user(u.get_id(), 42);
        service.unassign_task_from_user(u.get_id(), 42);
        assertFalse(service.get_user(u.get_id()).has_task_assigned(42));
    }

    @Test
    void test_mark_task_completed_for_user() {
        User u = service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.mark_task_completed_for_user(u.get_id());
        assertEquals(1, service.get_user(u.get_id()).get_tasks_completed_count());
    }

    @Test
    void test_get_user_by_task_id() {
        User u = service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.assign_task_to_user(u.get_id(), 42);
        assertTrue(service.get_user_by_task_id(42).isPresent());
        assertTrue(service.get_user_by_task_id(999).isEmpty());
    }

    @Test
    void test_count_users_by_role() {
        service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.create_user("B", "b@b.com", Role.DEVELOPER);
        service.create_user("C", "c@b.com", Role.ADMIN);
        assertEquals(2, service.count_users_by_role(Role.DEVELOPER));
        assertEquals(1, service.count_users_by_role(Role.ADMIN));
    }

    @Test
    void test_get_top_performers() {
        User a = service.create_user("A", "a@b.com", Role.DEVELOPER);
        User b = service.create_user("B", "b@b.com", Role.DEVELOPER);
        service.mark_task_completed_for_user(a.get_id());
        service.mark_task_completed_for_user(a.get_id());
        service.mark_task_completed_for_user(b.get_id());
        List<User> top = service.get_top_performers(1);
        assertEquals(1, top.size());
        assertEquals(a.get_id(), top.get(0).get_id());
    }

    @Test
    void test_clear_all() {
        service.create_user("A", "a@b.com", Role.DEVELOPER);
        service.create_user("B", "b@b.com", Role.DEVELOPER);
        service.clear_all();
        assertEquals(0, service.count_users());
    }

    @Test
    void test_add_user_direct() {
        User user = new User(100, "Direct", "d@b.com", Role.ADMIN);
        service.add_user_direct(user);
        assertEquals(100, service.get_user(100).get_id());
    }

    @Test
    void test_update_id_generator() {
        service.update_id_generator(50);
        User u = service.create_user("A", "a@b.com", Role.DEVELOPER);
        assertEquals(50, u.get_id());
    }
}
