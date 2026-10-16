package jfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import models.AppSettings;
import models.Role;
import models.User;
import services.*;

import java.util.HashMap;
import java.util.Map;

/**
 * SettingsView - role-aware settings configuration panel.
 * Shows only the settings the current user's role is allowed to view/edit.
 */
public class SettingsView extends StackPane {
    private final UserService userService;
    private final TaskService taskService;
    private final PomodoroService pomodoroService;
    private final JsonStorage storage;
    private final User currentUser;
    private final SettingsService settingsService;
    private final CategoryService categoryService;
    private final TagService tagService;

    private final Map<Integer, Control> settingControls = new HashMap<>();

    public SettingsView(TaskService ts, UserService us, PomodoroService ps, JsonStorage s, User currentUser, SettingsService settingsService, CategoryService categoryService, TagService tagService) {
        this.taskService = ts; this.userService = us; this.pomodoroService = ps; this.storage = s;
        this.currentUser = currentUser; this.settingsService = settingsService; this.categoryService = categoryService; this.tagService = tagService;
        setPrefSize(660, 660);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #1a1a1a;");

        VBox box = new VBox(16);
        box.setAlignment(Pos.TOP_CENTER);
        box.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.setPadding(new Insets(20, 10, 20, 10));

        Label title = new Label("SETTINGS");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #aaaaaa; -fx-padding: 0 0 12 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_PAGE_TITLE);

        Label roleLabel = new Label();
        roleLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px; -fx-padding: 0 0 16 0;");
        if (currentUser != null) {
            roleLabel.setText("Role: " + currentUser.get_role() + " — " + getVisibleCount() + " of 10 settings visible");
        } else {
            roleLabel.setText("Guest — 2 of 10 settings visible (read-only)");
        }
        ViewCoordinator.applyFont(roleLabel, ViewCoordinator.FONT_DETAIL);

        ScrollPane sp = new ScrollPane(box);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox root = new VBox(title, roleLabel, sp);
        root.setAlignment(Pos.CENTER);
        getChildren().add(root);

        buildSettingsForm(box);
    }

    private int getVisibleCount() {
        if (currentUser == null) return 2;
        return settingsService.getVisibleCountForRole(currentUser.get_role());
    }

    private void buildSettingsForm(VBox container) {
        Role role = currentUser != null ? currentUser.get_role() : null;
        boolean isGuest = currentUser == null;
        boolean canEdit = !isGuest;

        ComboBox<AppSettings.Theme> themeBox = new ComboBox<>();
        themeBox.getItems().addAll(AppSettings.Theme.DARK, AppSettings.Theme.LIGHT, AppSettings.Theme.AUTO);
        themeBox.setValue(settingsService.getSettings().getTheme());
        themeBox.setDisable(!canEdit || !settingsService.canEdit(1, role));
        settingControls.put(1, themeBox);
        container.getChildren().add(settingRow("Theme", themeBox, 1));

        int durMin = isGuest ? 25 : 1;
        Spinner<Integer> durationSpinner = new Spinner<>(durMin, 180, Math.max(settingsService.getSettings().getDefaultPomodoroDuration(), durMin));
        durationSpinner.setDisable(!canEdit || !settingsService.canEdit(2, role));
        settingControls.put(2, durationSpinner);
        container.getChildren().add(settingRow("Default Pomodoro Duration (min)", durationSpinner, 2));

        if (settingsService.canView(3, role)) {
            CheckBox soundBox = new CheckBox("Notification Sound");
            soundBox.setSelected(settingsService.getSettings().isNotificationSound());
            soundBox.setDisable(!canEdit);
            settingControls.put(3, soundBox);
            container.getChildren().add(settingRow("Notification Sound", soundBox, 3));
        }

        if (settingsService.canView(4, role)) {
            CheckBox autoStartBox = new CheckBox("Auto-start Next Session");
            autoStartBox.setSelected(settingsService.getSettings().isAutoStartNextSession());
            autoStartBox.setDisable(!canEdit);
            settingControls.put(4, autoStartBox);
            container.getChildren().add(settingRow("Auto-start Next Session", autoStartBox, 4));
        }

        if (settingsService.canView(5, role)) {
            Spinner<Integer> saveSpinner = new Spinner<>(5, 120, settingsService.getSettings().getAutoSaveInterval());
            saveSpinner.setDisable(!canEdit);
            settingControls.put(5, saveSpinner);
            container.getChildren().add(settingRow("Auto-save Interval (sec)", saveSpinner, 5));
        }

        if (settingsService.canView(6, role)) {
            CheckBox showAllBox = new CheckBox("Show All Tasks (bypass assignment filter)");
            showAllBox.setSelected(settingsService.getSettings().isShowAllTasks());
            showAllBox.setDisable(!canEdit);
            settingControls.put(6, showAllBox);
            container.getChildren().add(settingRow("Show All Tasks", showAllBox, 6));
        }

        if (settingsService.canView(7, role)) {
            CheckBox teamBox = new CheckBox("Team Report Access");
            teamBox.setSelected(settingsService.getSettings().isTeamReportAccess());
            teamBox.setDisable(!canEdit);
            settingControls.put(7, teamBox);
            container.getChildren().add(settingRow("Team Report Access", teamBox, 7));
        }

        if (settingsService.canView(8, role)) {
            Spinner<Integer> goalSpinner = new Spinner<>(30, 480, settingsService.getSettings().getDailyFocusGoal());
            goalSpinner.setDisable(!canEdit);
            settingControls.put(8, goalSpinner);
            container.getChildren().add(settingRow("Daily Focus Goal (min)", goalSpinner, 8));
        }

        if (settingsService.canView(9, role)) {
            CheckBox userMgmtBox = new CheckBox("Allow User Management");
            userMgmtBox.setSelected(settingsService.getSettings().isUserManagement());
            userMgmtBox.setDisable(!canEdit);
            settingControls.put(9, userMgmtBox);
            container.getChildren().add(settingRow("User Management", userMgmtBox, 9));
        }

        if (settingsService.canView(10, role)) {
            CheckBox roleMgmtBox = new CheckBox("Allow Role Management");
            roleMgmtBox.setSelected(settingsService.getSettings().isRoleManagement());
            roleMgmtBox.setDisable(!canEdit);
            settingControls.put(10, roleMgmtBox);
            container.getChildren().add(settingRow("Role Management", roleMgmtBox, 10));
        }

        Button btnSave = new Button("Save Settings");
        btnSave.getStyleClass().addAll("btn", "btn-nav-green");
        btnSave.setDisable(!canEdit);
        btnSave.setOnAction(e -> {
            saveSettings();
            javafx.stage.Window window = btnSave.getScene().getWindow();
            if (window != null) window.hide();
        });
        container.getChildren().add(btnSave);
    }

    private Node settingRow(String labelText, Control control, int settingNumber) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ViewCoordinator.applyFont(label, ViewCoordinator.FONT_DETAIL);
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(ViewCoordinator.CONTENT_W);
        HBox.setHgrow(control, Priority.ALWAYS);
        control.setMaxWidth(Double.MAX_VALUE);
        row.getChildren().addAll(label, control);
        return row;
    }

    private void saveSettings() {
        AppSettings s = settingsService.getSettings();
        Control c1 = settingControls.get(1);
        if (c1 instanceof ComboBox) s.setTheme(((ComboBox<AppSettings.Theme>) c1).getValue());
        Control c2 = settingControls.get(2);
        if (c2 instanceof Spinner) s.setDefaultPomodoroDuration(((Spinner<Integer>) c2).getValue());
        Control c3 = settingControls.get(3);
        if (c3 instanceof CheckBox) s.setNotificationSound(((CheckBox) c3).isSelected());
        Control c4 = settingControls.get(4);
        if (c4 instanceof CheckBox) s.setAutoStartNextSession(((CheckBox) c4).isSelected());
        Control c5 = settingControls.get(5);
        if (c5 instanceof Spinner) s.setAutoSaveInterval(((Spinner<Integer>) c5).getValue());
        Control c6 = settingControls.get(6);
        if (c6 instanceof CheckBox) s.setShowAllTasks(((CheckBox) c6).isSelected());
        Control c7 = settingControls.get(7);
        if (c7 instanceof CheckBox) s.setTeamReportAccess(((CheckBox) c7).isSelected());
        Control c8 = settingControls.get(8);
        if (c8 instanceof Spinner) s.setDailyFocusGoal(((Spinner<Integer>) c8).getValue());
        Control c9 = settingControls.get(9);
        if (c9 instanceof CheckBox) s.setUserManagement(((CheckBox) c9).isSelected());
        Control c10 = settingControls.get(10);
        if (c10 instanceof CheckBox) s.setRoleManagement(((CheckBox) c10).isSelected());

        if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Settings saved successfully.", ButtonType.OK);
        a.showAndWait();
    }

    public void showContentPage() { refresh(); }
    public void refresh() {
        AppSettings s = settingsService.getSettings();
        Control c1 = settingControls.get(1);
        if (c1 instanceof ComboBox) ((ComboBox<AppSettings.Theme>) c1).setValue(s.getTheme());
        Control c2 = settingControls.get(2);
        if (c2 instanceof Spinner) ((Spinner<Integer>) c2).getValueFactory().setValue(s.getDefaultPomodoroDuration());
        Control c3 = settingControls.get(3);
        if (c3 instanceof CheckBox) ((CheckBox) c3).setSelected(s.isNotificationSound());
        Control c4 = settingControls.get(4);
        if (c4 instanceof CheckBox) ((CheckBox) c4).setSelected(s.isAutoStartNextSession());
        Control c5 = settingControls.get(5);
        if (c5 instanceof Spinner) ((Spinner<Integer>) c5).getValueFactory().setValue(s.getAutoSaveInterval());
        Control c6 = settingControls.get(6);
        if (c6 instanceof CheckBox) ((CheckBox) c6).setSelected(s.isShowAllTasks());
        Control c7 = settingControls.get(7);
        if (c7 instanceof CheckBox) ((CheckBox) c7).setSelected(s.isTeamReportAccess());
        Control c8 = settingControls.get(8);
        if (c8 instanceof Spinner) ((Spinner<Integer>) c8).getValueFactory().setValue(s.getDailyFocusGoal());
        Control c9 = settingControls.get(9);
        if (c9 instanceof CheckBox) ((CheckBox) c9).setSelected(s.isUserManagement());
        Control c10 = settingControls.get(10);
        if (c10 instanceof CheckBox) ((CheckBox) c10).setSelected(s.isRoleManagement());
    }
}
