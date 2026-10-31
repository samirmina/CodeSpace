package web.controller;

import services.UserService;
import services.TaskService;
import services.PomodoroService;
import services.CategoryService;
import services.TagService;
import models.User;
import models.Task;
import models.Role;
import models.Priority;
import models.Status;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller
@RequestMapping("/")
public class WebController {

    private final UserService userService;
    private final TaskService taskService;
    private final PomodoroService pomodoroService;
    private final CategoryService categoryService;
    private final TagService tagService;

    public WebController(UserService userService, TaskService taskService,
                         PomodoroService pomodoroService, CategoryService categoryService,
                         TagService tagService) {
        this.userService = userService;
        this.taskService = taskService;
        this.pomodoroService = pomodoroService;
        this.categoryService = categoryService;
        this.tagService = tagService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("userCount", userService.count_users());
        model.addAttribute("taskCount", taskService.get_all_tasks().size());
        model.addAttribute("pomodoroCount", pomodoroService.get_all_pomodoros().size());
        model.addAttribute("categoryCount", categoryService.get_all_categories().size());
        model.addAttribute("tagCount", tagService.get_all_tags().size());
        model.addAttribute("topPerformers", userService.get_top_performers(5));
        model.addAttribute("recentTasks", taskService.get_all_tasks().stream()
                .limit(10).toList());
        return "dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.get_all_users());
        model.addAttribute("roles", Role.values());
        return "users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User(0, "", "", Role.ADMIN));
        model.addAttribute("roles", Role.values());
        return "user-form";
    }

    @PostMapping("/users")
    public RedirectView createUser(@ModelAttribute User user) {
        userService.create_user(user.get_name(), user.get_email(), user.get_role());
        return new RedirectView("/users");
    }

    @GetMapping("/users/{id}/edit")
    public String editUser(@PathVariable int id, Model model) {
        User user = userService.try_get_user(id).orElse(null);
        if (user == null) return "redirect:/users";
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "user-form";
    }

    @PostMapping("/users/{id}")
    public RedirectView updateUser(@PathVariable int id, @ModelAttribute User user) {
        userService.update_user_name(id, user.get_name());
        userService.update_user_email(id, user.get_email());
        userService.update_user_role(id, user.get_role());
        return new RedirectView("/users");
    }

    @PostMapping("/users/{id}/delete")
    public RedirectView deleteUser(@PathVariable int id) {
        userService.delete_user(id);
        return new RedirectView("/users");
    }

    @GetMapping("/tasks")
    public String tasks(Model model) {
        model.addAttribute("tasks", taskService.get_all_tasks());
        model.addAttribute("users", userService.get_all_users());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());
        return "tasks";
    }

    @GetMapping("/tasks/new")
    public String newTaskForm(Model model) {
        model.addAttribute("task", new Task(0, "", "", Priority.LOW));
        model.addAttribute("users", userService.get_all_users());
        model.addAttribute("priorities", Priority.values());
        return "task-form";
    }

    @PostMapping("/tasks")
    public RedirectView createTask(@ModelAttribute Task task) {
        taskService.create_task(task.get_title(), task.get_description(), task.get_priority());
        return new RedirectView("/tasks");
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTask(@PathVariable int id, Model model) {
        Task task = taskService.try_get_task(id).orElse(null);
        if (task == null) return "redirect:/tasks";
        model.addAttribute("task", task);
        model.addAttribute("users", userService.get_all_users());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());
        return "task-form";
    }

    @PostMapping("/tasks/{id}")
    public RedirectView updateTask(@PathVariable int id, @ModelAttribute Task task) {
        Task existing = taskService.try_get_task(id).orElse(null);
        if (existing != null) {
            existing.set_title(task.get_title());
            existing.set_description(task.get_description());
            existing.set_priority(task.get_priority());
            existing.set_status(task.get_status());
        }
        return new RedirectView("/tasks");
    }

    @PostMapping("/tasks/{id}/delete")
    public RedirectView deleteTask(@PathVariable int id) {
        // TaskService doesn't have delete, mark as cancelled instead
        Task task = taskService.try_get_task(id).orElse(null);
        if (task != null) {
            task.set_status(Status.CANCELLED);
        }
        return new RedirectView("/tasks");
    }

    @GetMapping("/launch")
    public String launch() {
        return "launch";
    }
}
