package jfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Task;
import models.User;
import models.Role;
import services.*;

import java.util.List;

public class UserView extends StackPane {
    private final UserService userService;
    private final TaskService taskService;
    private final PomodoroService pomodoroService;
    private final JsonStorage storage;
    private final SwipeContainer swipe;
    private final User currentUser;
    private final SettingsService settingsService;
    private final services.PostgresUserDAO postgresUserDAO;
    private final CategoryService categoryService;
    private final TagService tagService;
    private java.util.function.Consumer<Integer> onTabSwitch;

    private final Label ovTotal, ovAdmins, ovDevs, ovMgrs;

    private final Label ctName, ctEmail, ctRole, ctTasks;
    private final Label ctNavName;
    private final Button ctPrev, ctNext;
    private List<User> userList = new java.util.ArrayList<>();
    private int currentIdx;

    private final StackPane centerPane;
    private final VBox detailBox, addBox, deleteBox, assignBox;
    private final TextField tfAddName, tfAddEmail;
    private final ComboBox<String> cbAddRole, cbAssignTask;
    private final Label delConfirm;
    private Button btnSettings;

    public UserView(TaskService ts, UserService us, PomodoroService ps, JsonStorage s, User currentUser, SettingsService settingsService, services.PostgresUserDAO postgresUserDAO, CategoryService categoryService, TagService tagService) {
        this.taskService = ts; this.userService = us; this.pomodoroService = ps; this.storage = s;
        this.currentUser = currentUser; this.settingsService = settingsService; this.postgresUserDAO = postgresUserDAO; this.categoryService = categoryService; this.tagService = tagService;
        setPrefSize(660, 660);

        ovTotal = new Label("0"); ovTotal.getStyleClass().add("stat-value");
        ovAdmins = new Label("0"); ovAdmins.getStyleClass().add("stat-value");
        ovDevs = new Label("0"); ovDevs.getStyleClass().add("stat-value");
        ovMgrs = new Label("0"); ovMgrs.getStyleClass().add("stat-value");

        ctName = new Label(); ctName.setStyle("-fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-padding: 0 0 4 0;");
        ctName.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctEmail = new Label(); ctEmail.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctEmail.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctRole = new Label(); ctRole.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctRole.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctTasks = new Label(); ctTasks.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctTasks.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctNavName = new Label(); ctNavName.setStyle("-fx-text-fill: #cccccc; -fx-font-weight: bold;");
        ctNavName.setMaxWidth(ViewCoordinator.CONTENT_W);

        ctPrev = new Button("\u25C0");
        ctNext = new Button("\u25B6");
        for (Button b : new Button[]{ctPrev, ctNext}) {
            b.setShape(new javafx.scene.shape.Circle(14));
            b.setPrefSize(30, 30);
            b.setMinSize(30, 30);
            b.getStyleClass().add("edge-nav");
        }
        ctPrev.setOnAction(e -> { if (currentIdx > 0) { currentIdx--; showDetail(); } });
        ctNext.setOnAction(e -> { if (currentIdx < userList.size() - 1) { currentIdx++; showDetail(); } });

        btnSettings = new Button("Settings");
        btnSettings.getStyleClass().addAll("btn", "btn-nav");
        Button btnAdd = new Button("+ Add User");
        btnAdd.getStyleClass().addAll("btn", "btn-nav-green");
        Button btnDelete = new Button("\u2715 Delete");
        btnDelete.getStyleClass().addAll("btn", "btn-nav-red");
        Button btnAssign = new Button("Assign Task");
        btnAssign.getStyleClass().addAll("btn", "btn-nav");
        boolean isGuest = currentUser == null;
        if (isGuest) {
            btnAdd.setDisable(true);
            btnAdd.setStyle("-fx-opacity: 0.4;");
            btnDelete.setDisable(true);
            btnDelete.setStyle("-fx-opacity: 0.4;");
        }
        if (currentUser != null && !currentUser.get_role().canAssignTask()) {
            btnAssign.setDisable(true);
            btnAssign.setStyle("-fx-opacity: 0.4;");
        }

        tfAddName = new TextField(); tfAddName.setPromptText("Name");
        tfAddName.setMaxWidth(200);
        tfAddEmail = new TextField(); tfAddEmail.setPromptText("Email");
        tfAddEmail.setMaxWidth(200);
        cbAddRole = new ComboBox<>();
        cbAddRole.getItems().addAll("ADMIN", "DEVELOPER", "MANAGER"); cbAddRole.setValue("DEVELOPER");
        cbAssignTask = new ComboBox<>();
        cbAssignTask.setPromptText("-- select unassigned task --");
        cbAssignTask.setPrefWidth(260);

        detailBox = new VBox(6);
        detailBox.setAlignment(Pos.CENTER);
        Label dtlTitle = new Label("USER DETAILS");
        dtlTitle.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 8 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(dtlTitle, ViewCoordinator.FONT_SECTION);
        VBox detail = new VBox(4); detail.setAlignment(Pos.CENTER);
        detail.getChildren().addAll(ctName, ctEmail, ctRole, ctTasks);
        HBox actRow = new HBox(8);
        actRow.setAlignment(Pos.CENTER);
        actRow.getChildren().addAll(btnAdd, btnAssign, btnDelete, btnSettings);
        btnAdd.setOnAction(e -> showAddForm());
        btnAssign.setOnAction(e -> showAssignForm());
        btnDelete.setOnAction(e -> showDeleteForm());
        btnSettings.setOnAction(e -> openSettingsPopup());
        detailBox.getChildren().addAll(dtlTitle, detail, actRow);

        addBox = new VBox(8);
        addBox.setAlignment(Pos.CENTER);
        Label addLbl = new Label("ADD USER");
        addLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(addLbl, ViewCoordinator.FONT_SECTION);
        VBox addFields = new VBox(6);
        addFields.setAlignment(Pos.CENTER);
        addFields.getChildren().addAll(tfAddName, tfAddEmail, cbAddRole);
        Button btnAddOk = new Button("OK");
        btnAddOk.getStyleClass().addAll("btn", "btn-nav-green");
        Button btnAddCancel = new Button("Cancel");
        btnAddCancel.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox addBtns = new HBox(10); addBtns.setAlignment(Pos.CENTER);
        addBtns.getChildren().addAll(btnAddCancel, btnAddOk);
        addBox.getChildren().addAll(addLbl, addFields, addBtns);
        btnAddOk.setOnAction(e -> {
            if (currentUser == null) return;
            String n = tfAddName.getText().trim();
            String em = tfAddEmail.getText().trim();
            if (!n.isEmpty() && em.contains("@")) {
                models.User created = userService.create_user(n, em, models.Role.valueOf(cbAddRole.getValue()));
                if (postgresUserDAO != null) {
                    models.User pgUser = postgresUserDAO.getUserByEmail(created.get_email());
                    if (pgUser != null) {
                        userService.delete_user(created.get_id());
                        userService.add_user_direct(pgUser);
                    }
                }
                storage.save_user_registry(userService);
                tfAddName.clear(); tfAddEmail.clear(); cbAddRole.setValue("DEVELOPER");
                refresh();
                showDetail();
            }
        });
        btnAddCancel.setOnAction(e -> showDetail());

        delConfirm = new Label();
        ViewCoordinator.applyFont(delConfirm, ViewCoordinator.FONT_DETAIL);
        delConfirm.setStyle("-fx-text-fill: #cccccc;");
        deleteBox = new VBox(8);
        deleteBox.setAlignment(Pos.CENTER);
        Label delLbl = new Label("DELETE USER?");
        delLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #cc4444; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(delLbl, ViewCoordinator.FONT_SECTION);
        Button btnDelYes = new Button("Yes");
        btnDelYes.getStyleClass().addAll("btn", "btn-nav-red");
        Button btnDelNo = new Button("No");
        btnDelNo.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox delBtns = new HBox(10); delBtns.setAlignment(Pos.CENTER);
        delBtns.getChildren().addAll(btnDelNo, btnDelYes);
        deleteBox.getChildren().addAll(delLbl, delConfirm, delBtns);
        btnDelYes.setOnAction(e -> {
            if (currentUser == null) return;
            if (userList.isEmpty()) return;
            int userId = userList.get(currentIdx).get_id();
            userService.delete_user(userId);
            if (postgresUserDAO != null) {
                postgresUserDAO.deleteUser(userId);
            }
            storage.save_user_registry(userService);
            refresh();
            showDetail();
        });
        btnDelNo.setOnAction(e -> showDetail());

        assignBox = new VBox(8);
        assignBox.setAlignment(Pos.CENTER);
        Label assignLbl = new Label("ASSIGN TASK");
        assignLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 8 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(assignLbl, ViewCoordinator.FONT_SECTION);
        Button btnAssignOk = new Button("Assign");
        btnAssignOk.getStyleClass().addAll("btn", "btn-nav-green");
        Button btnAssignCancel = new Button("Cancel");
        btnAssignCancel.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox assignBtns = new HBox(10); assignBtns.setAlignment(Pos.CENTER);
        assignBtns.getChildren().addAll(btnAssignCancel, btnAssignOk);
        assignBox.getChildren().addAll(assignLbl, cbAssignTask, assignBtns);
        btnAssignCancel.setOnAction(e -> showDetail());
        btnAssignOk.setOnAction(e -> {
            String sel = cbAssignTask.getValue();
            if (sel == null || sel.startsWith("--")) return;
            int taskId = Integer.parseInt(sel.substring(0, sel.indexOf(" - ")));
            taskService.try_get_task(taskId).ifPresent(t -> {
                int oldUserId = t.get_assigned_user_id();
                // Unassign from previous user if assigned
                if (oldUserId > 0) {
                    userService.unassign_task_from_user(oldUserId, taskId);
                    storage.save_user_data(oldUserId, taskService, pomodoroService, settingsService, categoryService, tagService);
                }
                // Assign to new user
                t.set_assigned_user_id(userList.get(currentIdx).get_id());
                userService.assign_task_to_user(userList.get(currentIdx).get_id(), taskId);
                storage.save_user_data(userList.get(currentIdx).get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
            });
            refresh();
            showDetail();
        });

        centerPane = new StackPane(detailBox, addBox, deleteBox, assignBox);
        centerPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        showDetail();

        HBox contentRow = new HBox(0);
        contentRow.setAlignment(Pos.CENTER);
        contentRow.setPrefSize(660, 660);
        contentRow.getChildren().addAll(ctPrev, centerPane, ctNext);
        HBox.setHgrow(centerPane, Priority.ALWAYS);

        swipe = new SwipeContainer(buildOverview(), contentRow);
        getChildren().add(swipe);

        refresh();
    }

    public void setOnTabSwitch(java.util.function.Consumer<Integer> onTabSwitch) {
        this.onTabSwitch = onTabSwitch;
    }

    public void showContentPage() { refresh(); swipe.restoreLastPage(); }
    public void refresh() {
        userList = userService.get_all_users();
        currentIdx = Math.min(currentIdx, Math.max(0, userList.size() - 1));
        showCurrent();
        updateOverview();
    }

    private void showDetail() {
        detailBox.setVisible(true); detailBox.setManaged(true);
        addBox.setVisible(false); addBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        assignBox.setVisible(false); assignBox.setManaged(false);
        if (userList.isEmpty()) {
            ctName.setText("No users");
            ctEmail.setText(""); ctRole.setText(""); ctTasks.setText("");
            ctNavName.setText("\u2014");
            ctPrev.setDisable(true); ctNext.setDisable(true);
            return;
        }
        showCurrent();
    }

    private void showAddForm() {
        tfAddName.clear(); tfAddEmail.clear(); cbAddRole.setValue("DEVELOPER");
        detailBox.setVisible(false); detailBox.setManaged(false);
        addBox.setVisible(true); addBox.setManaged(true);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
    }

    private void showDeleteForm() {
        if (userList.isEmpty()) return;
        delConfirm.setText("Delete \"" + userList.get(currentIdx).get_name() + "\"?");
        detailBox.setVisible(false); detailBox.setManaged(false);
        addBox.setVisible(false); addBox.setManaged(false);
        deleteBox.setVisible(true); deleteBox.setManaged(true);
    }

    private void showAssignForm() {
        if (userList.isEmpty()) return;
        if (currentUser == null || !currentUser.get_role().canAssignTask()) return;
        List<Task> allTasks = taskService.get_all_tasks();
        cbAssignTask.getItems().clear();
        for (Task t : allTasks) {
            String assignedTo = t.get_assigned_user_id() > 0
                ? " (assigned to user #" + t.get_assigned_user_id() + ")"
                : " (unassigned)";
            cbAssignTask.getItems().add(t.get_id() + " - " + t.get_title() + assignedTo);
        }
        if (!cbAssignTask.getItems().isEmpty()) cbAssignTask.setValue(cbAssignTask.getItems().get(0));
        detailBox.setVisible(false); detailBox.setManaged(false);
        addBox.setVisible(false); addBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        assignBox.setVisible(true); assignBox.setManaged(true);
    }

    private Node buildOverview() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(660, 660);
        box.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.setPadding(new Insets(0, 10, 0, 10));
        Label title = new Label("Users");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #aaaaaa; -fx-padding: 16 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_PAGE_TITLE);
        HBox row = new HBox(ViewCoordinator.CARD_GAP);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(ViewCoordinator.CONTENT_W);
        row.getChildren().addAll(
            ViewCoordinator.statCard(ovTotal, "Total", 4),
            ViewCoordinator.statCard(ovAdmins, "Admins", 4),
            ViewCoordinator.statCard(ovDevs, "Devs", 4),
            ViewCoordinator.statCard(ovMgrs, "Mgrs", 4));
        box.getChildren().addAll(title, row);
        return box;
    }

    private void showCurrent() {
        if (userList.isEmpty()) {
            ctName.setText("No users");
            ViewCoordinator.fitDetailTitle(ctName, "No users");
            ctEmail.setText(""); ctRole.setText(""); ctTasks.setText("");
            ctNavName.setText("");
            ctPrev.setDisable(true); ctNext.setDisable(true);
            return;
        }
        User u = userList.get(currentIdx);
        ctName.setText(u.get_name());
        ViewCoordinator.fitDetailTitle(ctName, u.get_name());
        ViewCoordinator.fitDetail(ctEmail, u.get_email());
        ViewCoordinator.fitDetail(ctRole, "Role: " + u.get_role());
        ViewCoordinator.fitDetail(ctTasks, "Assigned: " + u.count_assigned_tasks() + " tasks");
        ctNavName.setText(u.get_name());
        ViewCoordinator.fitText(ctNavName, u.get_name(), ViewCoordinator.CONTENT_W, ViewCoordinator.FONT_SECTION, 9);
        ctPrev.setDisable(currentIdx <= 0);
        ctNext.setDisable(currentIdx >= userList.size() - 1);
    }

    private void updateOverview() {
        int total = userService.count_users();
        ovTotal.setText(String.valueOf(total));
        int admins = 0, devs = 0, mgrs = 0;
        for (User u : userService.get_all_users()) {
            switch (u.get_role()) { case ADMIN: admins++; break; case DEVELOPER: devs++; break; case MANAGER: mgrs++; break; }
        }
        ovAdmins.setText(String.valueOf(admins));
        ovDevs.setText(String.valueOf(devs));
        ovMgrs.setText(String.valueOf(mgrs));
        ViewCoordinator.fitStatValue(ovTotal, ovTotal.getText(), 4);
        ViewCoordinator.fitStatValue(ovAdmins, ovAdmins.getText(), 4);
        ViewCoordinator.fitStatValue(ovDevs, ovDevs.getText(), 4);
        ViewCoordinator.fitStatValue(ovMgrs, ovMgrs.getText(), 4);
    }

    private void openSettingsPopup() {
        SettingsView settingsView = new SettingsView(taskService, userService, pomodoroService, storage, currentUser, settingsService, categoryService, tagService);
        javafx.scene.Scene scene = new javafx.scene.Scene(settingsView);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        javafx.stage.Stage stage = new javafx.stage.Stage();
        stage.setTitle("Settings");
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}
