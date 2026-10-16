package services;

import models.AppSettings;
import models.Category;
import models.Pomodoro;
import models.Priority;
import models.Role;
import models.Status;
import models.Tag;
import models.Task;
import models.User;
import utils.FileUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * JsonStorage - handles all data persistence without external libraries.
 * Per-user data isolation: each user has their own data file.
 * Shared user registry stored separately.
 */
public class JsonStorage {
    private static final String DATA_FILE = "data.json";
    private static final String USER_REGISTRY_FILE = "data_users.json";
    private List<Category> categories = new ArrayList<>();
    private List<Tag> tags = new ArrayList<>();
    private AppSettings settings = new AppSettings();

    // Get user-specific data file path
    public String get_user_data_file(int userId) {
        return "data_" + userId + ".json";
    }

    // Check if user data file exists
    public boolean has_user_data(int userId) {
        return FileUtils.fileExists(get_user_data_file(userId));
    }

    // Check if user registry exists
    public boolean has_user_registry() {
        return FileUtils.fileExists(USER_REGISTRY_FILE);
    }

    // Check if old unified data file exists (for migration)
    public boolean has_legacy_data() {
        return FileUtils.fileExists(DATA_FILE);
    }

    // Migrate old data.json to per-user format
    public void migrate_legacy_data_if_needed(TaskService taskService, UserService userService, PomodoroService pomodoroService, services.SettingsService settingsService) {
        if (has_user_registry() || !has_legacy_data()) return;

        System.out.println("=== Migrating legacy data to per-user format ===");
        String content = FileUtils.readFromFile(DATA_FILE);
        if (content.isEmpty()) return;

// Load everything from old format
            try {
                load_data(content, taskService, userService, pomodoroService, settingsService, new CategoryService(), new TagService());
                System.out.println("  → Loaded legacy data: " + userService.count_users() + " users, " + taskService.get_all_tasks().size() + " tasks");

                // Save user registry
                save_user_registry(userService);

                // Save per-user data
                CategoryService catService = new CategoryService();
                TagService tagService = new TagService();
                for (User u : userService.get_all_users()) {
                    save_user_data(u.get_id(), taskService, pomodoroService, settingsService, catService, tagService);
                }

            // Archive old file
            FileUtils.moveFile(DATA_FILE, DATA_FILE + ".bak");
            System.out.println("  → Migration complete. Old data backed up to " + DATA_FILE + ".bak");
        } catch (Exception e) {
            System.err.println("✗ Migration failed: " + e.getMessage());
        }
    }

    // Save user registry (shared across all users)
    public void save_user_registry(UserService userService) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        append_users_to_json(json, userService);
        json.append("\n}");
        FileUtils.writeToFile(USER_REGISTRY_FILE, json.toString());
    }

    // Load user registry
    public void load_user_registry(UserService userService) {
        String content = FileUtils.readFromFile(USER_REGISTRY_FILE);
        if (content.isEmpty()) return;

        try {
            int maxUserId = load_users_from_json(content, userService);
            userService.update_id_generator(maxUserId + 1);
        } catch (Exception e) {
            System.err.println("✗ Error loading user registry: " + e.getMessage());
        }
    }

    // Save per-user data
    public void save_user_data(int userId, TaskService taskService, PomodoroService pomodoroService, services.SettingsService settingsService, CategoryService categoryService, TagService tagService) {
        String filename = get_user_data_file(userId);
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        append_settings_to_json(json, settingsService);
        json.append(",\n");

        java.util.List<Task> userTasks = new java.util.ArrayList<>();
        for (Task t : taskService.get_all_tasks()) {
            if (t.get_assigned_user_id() == userId) userTasks.add(t);
        }
        append_tasks_to_json(json, userTasks);
        json.append(",\n");

        java.util.Set<Integer> userTaskIds = new java.util.HashSet<>();
        for (Task t : userTasks) userTaskIds.add(t.get_id());

        java.util.List<Pomodoro> userPomos = new java.util.ArrayList<>();
        for (Pomodoro p : pomodoroService.get_all_pomodoros()) {
            if (userTaskIds.contains(p.get_task_id())) {
                Task task = taskService.try_get_task(p.get_task_id()).orElse(null);
                if (task != null && task.is_store_sessions()) {
                    userPomos.add(p);
                }
            }
        }
        append_pomodoros_to_json(json, userPomos);
        json.append(",\n");

        // Save categories and tags for this user
        append_categories_to_json(json, categoryService.get_all_categories());
        json.append(",\n");
        append_tags_to_json(json, tagService.get_all_tags());
        json.append("\n}");

        FileUtils.writeToFile(filename, json.toString());
    }

    // Load per-user data
    public void load_user_data(int userId, TaskService taskService, PomodoroService pomodoroService, services.SettingsService settingsService, CategoryService categoryService, TagService tagService) {
        String filename = get_user_data_file(userId);
        String content = FileUtils.readFromFile(filename);
        if (content.isEmpty()) {
            System.out.println("ℹ No saved data for user #" + userId);
            return;
        }

        try {
            load_data(content, taskService, null, pomodoroService, settingsService, categoryService, tagService);
        } catch (Exception e) {
            System.err.println("✗ Error loading user data: " + e.getMessage());
        }
    }

    // Save all data (legacy - kept for backward compatibility)
    public void save_data(TaskService taskService, UserService userService, PomodoroService pomodoroService, services.SettingsService settingsService) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        append_settings_to_json(json, settingsService);
        json.append(",\n");

        append_tasks_to_json(json, taskService.get_all_tasks());
        json.append(",\n");

        append_pomodoros_to_json(json, pomodoroService.get_all_pomodoros());
        json.append(",\n");
        append_categories_to_json(json, new ArrayList<>());
        json.append("\n}");

        FileUtils.writeToFile(DATA_FILE, json.toString());
    }

    // Save data without pomodoros
    public void save_data(TaskService taskService, UserService userService) {
        save_data(taskService, userService, new PomodoroService(), new SettingsService());
    }

    // Save data with services
    public void save_data(TaskService taskService, UserService userService, PomodoroService pomodoroService) {
        save_data(taskService, userService, pomodoroService, new SettingsService());
    }

    // Load all data (legacy)
    public void load_data(TaskService taskService, UserService userService, PomodoroService pomodoroService, services.SettingsService settingsService) {
        String content = FileUtils.readFromFile(DATA_FILE);
        if (content.isEmpty()) {
            System.out.println("ℹ No saved data found");
            return;
        }

        try {
            load_data(content, taskService, userService, pomodoroService, settingsService, new CategoryService(), new TagService());
        } catch (Exception e) {
            System.err.println("✗ Error loading data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Internal load from string content
    private void load_data(String content, TaskService taskService, UserService userService, PomodoroService pomodoroService, services.SettingsService settingsService, CategoryService categoryService, TagService tagService) {
        load_settings_from_json(content, settingsService);
        int maxTaskId = load_tasks_from_json(content, taskService);
        int maxUserId = load_users_from_json(content, userService);
        int maxPomodoroId = load_pomodoros_from_json(content, pomodoroService);
        load_categories_from_json(content, categoryService);
        load_tags_from_json(content, tagService);

        taskService.update_id_generator(maxTaskId + 1);
        if (userService != null) userService.update_id_generator(maxUserId + 1);
        pomodoroService.update_id_generator(maxPomodoroId + 1);
    }

    // Load data without pomodoros
    public void load_data(TaskService taskService, UserService userService) {
        load_data(taskService, userService, new PomodoroService(), new SettingsService(), new CategoryService(), new TagService());
    }

    // Load data with services
    public void load_data(TaskService taskService, UserService userService, PomodoroService pomodoroService) {
        load_data(taskService, userService, pomodoroService, new SettingsService(), new CategoryService(), new TagService());
    }

    // Load data with all services
    public void load_data(TaskService taskService, UserService userService, PomodoroService pomodoroService, services.SettingsService settingsService, CategoryService categoryService, TagService tagService) {
        String content = FileUtils.readFromFile(DATA_FILE);
        if (content.isEmpty()) {
            System.out.println("ℹ No saved data found");
            return;
        }
        try {
            load_data(content, taskService, userService, pomodoroService, settingsService, categoryService, tagService);
        } catch (Exception e) {
            System.err.println("✗ Error loading data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Append tasks section
    private void append_tasks_to_json(StringBuilder json, List<Task> tasks) {
        json.append("  \"tasks\": [\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(t.get_id()).append(",\n");
            json.append("      \"title\": \"").append(escape_json(t.get_title())).append("\",\n");
            json.append("      \"description\": \"").append(escape_json(t.get_description())).append("\",\n");
            json.append("      \"priority\": \"").append(t.get_priority()).append("\",\n");
            json.append("      \"status\": \"").append(t.get_status()).append("\",\n");

            if (t.get_due_date() != null) {
                json.append("      \"dueDate\": \"").append(t.get_due_date()).append("\",\n");
            }

            json.append("      \"completionTimeMinutes\": ").append(t.get_completion_time_minutes()).append(",\n");

            if (t.get_completed_at() != null) {
                json.append("      \"completedAt\": \"").append(t.get_completed_at()).append("\",\n");
            }

            json.append("      \"tags\": [");
            List<String> tags = t.get_tags();
            for (int j = 0; j < tags.size(); j++) {
                json.append("\"").append(escape_json(tags.get(j))).append("\"");
                if (j < tags.size() - 1) json.append(", ");
            }
            json.append("],\n");

            json.append("      \"assignedUserId\": ").append(t.get_assigned_user_id()).append(",\n");
            json.append("      \"categoryId\": ").append(t.get_category_id()).append(",\n");
            json.append("      \"storeSessions\": ").append(t.is_store_sessions()).append("\n");

            json.append("    }");
            if (i < tasks.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]");
    }

    // Append users section
    private void append_users_to_json(StringBuilder json, UserService userService) {
        json.append("  \"users\": [\n");
        List<User> users = userService.get_all_users();
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(u.get_id()).append(",\n");
            json.append("      \"name\": \"").append(escape_json(u.get_name())).append("\",\n");
            json.append("      \"email\": \"").append(escape_json(u.get_email())).append("\",\n");
            json.append("      \"role\": \"").append(u.get_role()).append("\",\n");
            json.append("      \"tasksCompletedCount\": ").append(u.get_tasks_completed_count()).append("\n");
            json.append("    }");
            if (i < users.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]");
    }

    // Append pomodoros section
    private void append_pomodoros_to_json(StringBuilder json, List<Pomodoro> pomodoros) {
        json.append("  \"pomodoros\": [\n");
        for (int i = 0; i < pomodoros.size(); i++) {
            Pomodoro p = pomodoros.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(p.get_id()).append(",\n");
            json.append("      \"taskId\": ").append(p.get_task_id()).append(",\n");
            json.append("      \"taskName\": \"").append(escape_json(p.get_task_name())).append("\",\n");
            json.append("      \"durationMinutes\": ").append(p.get_duration_minutes()).append(",\n");
            json.append("      \"completedSessions\": ").append(p.get_completed_sessions()).append(",\n");
            List<Integer> durations = p.get_session_durations();
            json.append("      \"sessionDurations\": [\n");
            for (int j = 0; j < durations.size(); j++) {
                json.append("        ").append(durations.get(j));
                if (j < durations.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("      ],\n");
            List<LocalDateTime> history = p.get_session_history();
            json.append("      \"sessionHistory\": [\n");
            for (int j = 0; j < history.size(); j++) {
                json.append("        \"").append(history.get(j).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\"");
                if (j < history.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("      ]\n");
            json.append("    }");
            if (i < pomodoros.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]");
    }

    // Append categories section
    private void append_categories_to_json(StringBuilder json, List<Category> categories) {
        json.append("  \"categories\": [\n");
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(c.get_id()).append(",\n");
            json.append("      \"name\": \"").append(escape_json(c.get_name())).append("\",\n");
            json.append("      \"description\": \"").append(escape_json(c.get_description())).append("\"\n");
            json.append("    }");
            if (i < categories.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]");
    }

    // Append tags section
    private void append_tags_to_json(StringBuilder json, List<Tag> tags) {
        json.append("  \"tags\": [\n");
        for (int i = 0; i < tags.size(); i++) {
            Tag t = tags.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(t.get_id()).append(",\n");
            json.append("      \"name\": \"").append(escape_json(t.get_name())).append("\",\n");
            json.append("      \"color\": \"").append(escape_json(t.get_color())).append("\"\n");
            json.append("    }");
            if (i < tags.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]");
    }

    // Get stored categories
    public List<Category> get_categories() { return categories; }

    // Set stored categories
    public void set_categories(List<Category> categories) { this.categories = categories; }

    // Get stored tags
    public List<Tag> get_tags() { return tags; }

    // Set stored tags
    public void set_tags(List<Tag> tags) { this.tags = tags; }

    // Get settings
    public AppSettings get_settings() { return settings; }

    // Set settings
    public void set_settings(AppSettings settings) { this.settings = settings; }

    // Load settings from JSON
    private void load_settings_from_json(String content, services.SettingsService settingsService) {
        String settingsSection = extract_section(content, "settings");
        if (settingsSection.isEmpty()) return;

        models.AppSettings s = settingsService.getSettings();
        String themeStr = extract_string(settingsSection, "\"theme\":");
        if (!themeStr.isEmpty()) {
            try { s.setTheme(models.AppSettings.Theme.valueOf(themeStr)); } catch (Exception e) { }
        }
        int defaultDuration = extract_int(settingsSection, "\"defaultPomodoroDuration\":");
        if (defaultDuration > 0) s.setDefaultPomodoroDuration(defaultDuration);
        s.setNotificationSound(extract_bool(settingsSection, "\"notificationSound\":", s.isNotificationSound()));
        s.setAutoStartNextSession(extract_bool(settingsSection, "\"autoStartNextSession\":", s.isAutoStartNextSession()));
        int autoSave = extract_int(settingsSection, "\"autoSaveInterval\":");
        if (autoSave > 0) s.setAutoSaveInterval(autoSave);
        s.setShowAllTasks(extract_bool(settingsSection, "\"showAllTasks\":", s.isShowAllTasks()));
        s.setTeamReportAccess(extract_bool(settingsSection, "\"teamReportAccess\":", s.isTeamReportAccess()));
        int focusGoal = extract_int(settingsSection, "\"dailyFocusGoal\":");
        if (focusGoal > 0) s.setDailyFocusGoal(focusGoal);
        s.setUserManagement(extract_bool(settingsSection, "\"userManagement\":", s.isUserManagement()));
        s.setRoleManagement(extract_bool(settingsSection, "\"roleManagement\":", s.isRoleManagement()));
    }

    // Append settings section
    private void append_settings_to_json(StringBuilder json, services.SettingsService settingsService) {
        models.AppSettings s = settingsService.getSettings();
        json.append("  \"settings\": {\n");
        json.append("    \"theme\": \"").append(s.getTheme()).append("\",\n");
        json.append("    \"defaultPomodoroDuration\": ").append(s.getDefaultPomodoroDuration()).append(",\n");
        json.append("    \"notificationSound\": ").append(s.isNotificationSound()).append(",\n");
        json.append("    \"autoStartNextSession\": ").append(s.isAutoStartNextSession()).append(",\n");
        json.append("    \"autoSaveInterval\": ").append(s.getAutoSaveInterval()).append(",\n");
        json.append("    \"showAllTasks\": ").append(s.isShowAllTasks()).append(",\n");
        json.append("    \"teamReportAccess\": ").append(s.isTeamReportAccess()).append(",\n");
        json.append("    \"dailyFocusGoal\": ").append(s.getDailyFocusGoal()).append(",\n");
        json.append("    \"userManagement\": ").append(s.isUserManagement()).append(",\n");
        json.append("    \"roleManagement\": ").append(s.isRoleManagement()).append("\n");
        json.append("  }");
    }

    // Load tasks from JSON
    private int load_tasks_from_json(String content, TaskService taskService) {
        String tasksSection = extract_section(content, "tasks");
        int maxTaskId = 0;

        if (!tasksSection.isEmpty()) {
            List<String> taskObjects = extract_objects(tasksSection);

            for (String taskObj : taskObjects) {
                int id = extract_int(taskObj, "\"id\":");
                String title = extract_string(taskObj, "\"title\":");
                String description = extract_string(taskObj, "\"description\":");
                Priority priority = Priority.valueOf(extract_string(taskObj, "\"priority\":"));
                Status status = Status.valueOf(extract_string(taskObj, "\"status\":"));

                Task task = new Task(id, title, description, priority);
                task.set_status(status);

                String dueDateStr = extract_string(taskObj, "\"dueDate\":");
                if (!dueDateStr.isEmpty() && !dueDateStr.equals("null")) {
                    try {
                        LocalDateTime dueDate = LocalDateTime.parse(dueDateStr);
                        task.set_due_date(dueDate);
                    } catch (Exception e) { }
                }

                int completionTime = extract_int(taskObj, "\"completionTimeMinutes\":");
                task.set_completion_time_minutes(completionTime);

                String completedAtStr = extract_string(taskObj, "\"completedAt\":");
                if (!completedAtStr.isEmpty() && !completedAtStr.equals("null")) {
                    try {
                        LocalDateTime completedAt = LocalDateTime.parse(completedAtStr);
                        task.set_completed_at(completedAt);
                    } catch (Exception e) { }
                }

                List<String> tags = extract_string_list(taskObj, "\"tags\":");
                for (String tag : tags) {
                    task.add_tag(tag);
                }

                int assignedUserId = extract_int(taskObj, "\"assignedUserId\":");
                if (assignedUserId > 0) {
                    task.set_assigned_user_id(assignedUserId);
                }

                int categoryId = extract_int(taskObj, "\"categoryId\":");
                if (categoryId > 0) {
                    task.set_category_id(categoryId);
                }

                boolean storeSessions = extract_bool(taskObj, "\"storeSessions\":", true);
                task.set_store_sessions(storeSessions);

                taskService.add_task_direct(task);

                if (id > maxTaskId) maxTaskId = id;
            }
        }

        return maxTaskId;
    }

    // Load users from JSON
    private int load_users_from_json(String content, UserService userService) {
        String usersSection = extract_section(content, "users");
        int maxUserId = 0;

        if (userService == null) {
            // If userService is null, we still need to find max id for generator update
            if (!usersSection.isEmpty()) {
                List<String> userObjects = extract_objects(usersSection);
                for (String userObj : userObjects) {
                    int id = extract_int(userObj, "\"id\":");
                    if (id > maxUserId) maxUserId = id;
                }
            }
            return maxUserId;
        }

        if (!usersSection.isEmpty()) {
            List<String> userObjects = extract_objects(usersSection);

            for (String userObj : userObjects) {
                int id = extract_int(userObj, "\"id\":");
                String name = extract_string(userObj, "\"name\":");
                String email = extract_string(userObj, "\"email\":");
                Role role = Role.valueOf(extract_string(userObj, "\"role\":"));

                User user = new User(id, name, email, role);

                int completedCount = extract_int(userObj, "\"tasksCompletedCount\":");
                for (int i = 0; i < completedCount; i++) {
                    user.increment_completed_count();
                }

                userService.add_user_direct(user);

                if (id > maxUserId) maxUserId = id;
            }
        }

        return maxUserId;
    }

    // Load pomodoros from JSON
    private int load_pomodoros_from_json(String content, PomodoroService pomodoroService) {
        String pomodorosSection = extract_section(content, "pomodoros");
        int maxPomodoroId = 0;

        if (!pomodorosSection.isEmpty()) {
            List<String> pomodoroObjects = extract_objects(pomodorosSection);

            for (String pomoObj : pomodoroObjects) {
                int id = extract_int(pomoObj, "\"id\":");
                int taskId = extract_int(pomoObj, "\"taskId\":");
                String taskName = extract_string(pomoObj, "\"taskName\":");
                int durationMinutes = extract_int(pomoObj, "\"durationMinutes\":");
                int completedSessions = extract_int(pomoObj, "\"completedSessions\":");

                Pomodoro pomodoro = new Pomodoro(id, taskId, taskName, durationMinutes);
                pomodoro.set_completed_sessions(completedSessions);

                List<String> historyStrs = extract_string_list(pomoObj, "\"sessionHistory\":");
                for (String ts : historyStrs) {
                    try {
                        pomodoro.get_session_history().add(LocalDateTime.parse(ts));
                    } catch (Exception e) { }
                }
                if (!historyStrs.isEmpty()) {
                    pomodoro.get_session_history().sort(null);
                }

                List<String> durationStrs = extract_string_list(pomoObj, "\"sessionDurations\":");
                if (!durationStrs.isEmpty()) {
                    for (String ds : durationStrs) {
                        try {
                            pomodoro.get_session_durations().add(Integer.parseInt(ds.trim()));
                        } catch (Exception e) { }
                    }
                }

                if (pomodoro.get_session_durations().isEmpty() && completedSessions > 0) {
                    for (int k = 0; k < completedSessions; k++) {
                        pomodoro.get_session_durations().add(durationMinutes);
                    }
                }

                // Validate session parity after loading
                pomodoro.validate_session_parity();

                pomodoroService.add_pomodoro_direct(pomodoro);

                if (id > maxPomodoroId) maxPomodoroId = id;
            }
        }

        return maxPomodoroId;
    }

    // Load categories from JSON
    private void load_categories_from_json(String content) {
        String categoriesSection = extract_section(content, "categories");
        if (categoriesSection.isEmpty()) return;

        categories.clear();
        List<String> categoryObjects = extract_objects(categoriesSection);
        for (String catObj : categoryObjects) {
            int id = extract_int(catObj, "\"id\":");
            String name = extract_string(catObj, "\"name\":");
            String description = extract_string(catObj, "\"description\":");
            categories.add(new Category(id, name, description));
        }
    }

    // Load categories from JSON into service
    private void load_categories_from_json(String content, CategoryService categoryService) {
        String categoriesSection = extract_section(content, "categories");
        if (categoriesSection.isEmpty()) return;

        List<String> categoryObjects = extract_objects(categoriesSection);
        for (String catObj : categoryObjects) {
            int id = extract_int(catObj, "\"id\":");
            String name = extract_string(catObj, "\"name\":");
            String description = extract_string(catObj, "\"description\":");
            categoryService.add_category_direct(new Category(id, name, description));
        }
    }

    // Load tags from JSON into service
    private void load_tags_from_json(String content, TagService tagService) {
        String tagsSection = extract_section(content, "tags");
        if (tagsSection.isEmpty()) return;

        List<String> tagObjects = extract_objects(tagsSection);
        for (String tagObj : tagObjects) {
            int id = extract_int(tagObj, "\"id\":");
            String name = extract_string(tagObj, "\"name\":");
            String color = extract_string(tagObj, "\"color\":");
            tagService.add_tag_direct(new Tag(id, name, color != null && !color.isEmpty() ? color : "#3498DB"));
        }
    }

    // Extract section from JSON
    private String extract_section(String json, String sectionName) {
        String key = "\"" + sectionName + "\":";
        int start = json.indexOf(key);
        if (start == -1) return "";
        start = json.indexOf("[", start);
        if (start == -1) return "";
        int depth = 1;
        int end = start + 1;
        while (end < json.length() && depth > 0) {
            char c = json.charAt(end);
            if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') depth--;
            end++;
        }
        if (depth != 0) return "";
        return json.substring(start + 1, end - 1).trim();
    }

    // Extract objects from section
    private List<String> extract_objects(String section) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;

        for (int i = 0; i < section.length(); i++) {
            char c = section.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    objects.add(section.substring(start, i + 1));
                }
            }
        }
        return objects;
    }

    // Extract integer value
    private int extract_int(String obj, String key) {
        int start = obj.indexOf(key);
        if (start == -1) return 0;
        start += key.length();
        while (start < obj.length() && !Character.isDigit(obj.charAt(start))) start++;
        if (start >= obj.length()) return 0;
        int end = start;
        while (end < obj.length() && Character.isDigit(obj.charAt(end))) end++;
        if (end == start) return 0;
        return Integer.parseInt(obj.substring(start, end));
    }

    // Extract string value
    private String extract_string(String obj, String key) {
        int start = obj.indexOf(key);
        if (start == -1) return "";
        start = obj.indexOf("\"", start + key.length());
        if (start == -1) return "";
        int end = start + 1;
        while (end < obj.length()) {
            if (obj.charAt(end) == '"' && obj.charAt(end - 1) != '\\') break;
            end++;
        }
        String value = obj.substring(start + 1, end);
        return unescape_json(value);
    }

    // Extract string list
    private List<String> extract_string_list(String obj, String key) {
        List<String> list = new ArrayList<>();
        int start = obj.indexOf(key);
        if (start == -1) return list;

        int arrayStart = obj.indexOf("[", start);
        if (arrayStart == -1) return list;

        int arrayEnd = obj.indexOf("]", arrayStart);
        if (arrayEnd == -1) return list;

        String arrayContent = obj.substring(arrayStart + 1, arrayEnd).trim();
        if (arrayContent.isEmpty()) return list;

        int pos = 0;
        while (pos < arrayContent.length()) {
            int quoteStart = arrayContent.indexOf("\"", pos);
            if (quoteStart == -1) break;
            int quoteEnd = quoteStart + 1;
            while (quoteEnd < arrayContent.length()) {
                if (arrayContent.charAt(quoteEnd) == '"' && arrayContent.charAt(quoteEnd - 1) != '\\') break;
                quoteEnd++;
            }
            if (quoteEnd > quoteStart) {
                list.add(unescape_json(arrayContent.substring(quoteStart + 1, quoteEnd)));
            }
            pos = quoteEnd + 1;
        }

        return list;
    }

    // Extract boolean value
    private boolean extract_bool(String obj, String key, boolean defaultValue) {
        int start = obj.indexOf(key);
        if (start == -1) return defaultValue;
        start += key.length();
        while (start < obj.length() && (obj.charAt(start) == ' ' || obj.charAt(start) == ':')) start++;
        if (start >= obj.length()) return defaultValue;
        String token = obj.substring(start).trim();
        if (token.startsWith("true")) return true;
        if (token.startsWith("false")) return false;
        return defaultValue;
    }

    // Escape JSON special characters
    private String escape_json(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    // Unescape JSON special characters
    private String unescape_json(String text) {
        return text.replace("\\n", "\n")
                   .replace("\\r", "\r")
                   .replace("\\t", "\t")
                   .replace("\\\"", "\"")
                   .replace("\\\\", "\\");
    }

    // Check if saved data exists (legacy)
    public boolean has_saved_data() {
        return FileUtils.fileExists(DATA_FILE);
    }

    // Clear saved data
    public void clear_saved_data() {
        FileUtils.deleteFile(DATA_FILE);
    }
}
