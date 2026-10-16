package jfx;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import models.Priority;
import models.Status;
import models.Task;
import models.User;
import models.Category;
import models.Tag;
import services.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class TaskView extends StackPane {
    private final TaskService taskService;
    private final UserService userService;
    private final PomodoroService pomodoroService;
    private final JsonStorage storage;
    private final SwipeContainer swipe;
    private final User currentUser;
    private final SettingsService settingsService;

    private final Label ovTotal, ovDone, ovUrgent, ovOverdue;
    private final Label ovUrgentLbl, ovHighLbl, ovMedLbl, ovLowLbl;

    private final Label ctTitle, ctStatus, ctPriority, ctDesc, ctStoreFlag;
    private final Label ctNavName;
    private final Button ctPrev, ctNext;
    private List<Task> taskList = new java.util.ArrayList<>();
    private int currentIdx;

    private final StackPane centerPane;
    private final VBox detailBox, addBox, editBox, deleteBox, completedBox;
    private final TextField tfAddTitle;
    private final TextArea taAddDesc, taEditDesc;
    private final ComboBox<String> cbAddPriority, cbEditStatus, cbEditPriority;
    private final CheckBox cbStoreSessions;
    private final CheckBox cbDeleteSessions;
    private final Label delConfirm;
    private final Label ctComTitle, ctComStatus, ctComPriority, ctComDate, ctComCounter;
    private final Button ctComPrev, ctComNext;
    private final Button btnCompleted;
    private final Button btnCatTag;
    private final ComboBox<String> cbAddCategory, cbAddTag, cbEditCategory, cbEditTag;
    private List<Task> completedTaskList = new java.util.ArrayList<>();
    private int completedIdx;

    // --- Category/Tag management ---
    private final VBox categoryTagBox;
    private final ListView<Category> categoryListView;
    private final ListView<Tag> tagListView;
    private final TextField tfCatName, tfCatDesc;
    private final TextField tfTagName;
    private final ColorPicker cpTagColor;
    private final List<Category> categoryList = new ArrayList<>();
    private final List<Tag> tagList = new ArrayList<>();

    private final CategoryService categoryService;
    private final TagService tagService;

    public TaskView(TaskService ts, UserService us, PomodoroService ps, JsonStorage s, User currentUser, SettingsService settingsService, CategoryService categoryService, TagService tagService) {
        this.taskService = ts; this.userService = us; this.pomodoroService = ps; this.storage = s; this.currentUser = currentUser; this.settingsService = settingsService; this.categoryService = categoryService; this.tagService = tagService;
        setPrefSize(660, 660);

        ovTotal = new Label("0"); ovTotal.getStyleClass().add("stat-value");
        ovDone = new Label("0%"); ovDone.getStyleClass().add("stat-value");
        ovUrgent = new Label("0"); ovUrgent.getStyleClass().add("stat-value");
        ovOverdue = new Label("0"); ovOverdue.getStyleClass().add("stat-value");
        ovUrgentLbl = new Label(); ovHighLbl = new Label(); ovMedLbl = new Label(); ovLowLbl = new Label();
        for (Label l : new Label[]{ovUrgentLbl, ovHighLbl, ovMedLbl, ovLowLbl}) l.setMaxWidth(ViewCoordinator.CONTENT_W);

        ctTitle = new Label(); ctTitle.setStyle("-fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-padding: 0 0 4 0;");
        ctTitle.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctStatus = new Label(); ctStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctStatus.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctPriority = new Label(); ctPriority.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctPriority.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctDesc = new Label(); ctDesc.setStyle("-fx-text-fill: #aaaaaa; -fx-padding: 2 0;");
        ctDesc.setWrapText(true);
        ctDesc.setAlignment(Pos.CENTER);
        ctDesc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        ctDesc.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctStoreFlag = new Label();
        ctStoreFlag.setStyle("-fx-font-weight: bold; -fx-text-fill: #cc4444; -fx-padding: 2 0;");
        ctStoreFlag.setMaxWidth(ViewCoordinator.CONTENT_W);
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
        ctNext.setOnAction(e -> { if (currentIdx < taskList.size() - 1) { currentIdx++; showDetail(); } });

        Button btnAdd = new Button("+ Add Task");
        btnAdd.getStyleClass().addAll("btn", "btn-nav-green");
        Button btnEdit = new Button("\u270E Edit");
        btnEdit.getStyleClass().addAll("btn", "btn-nav");
        Button btnDelete = new Button("\u2715 Delete");
        btnDelete.getStyleClass().addAll("btn", "btn-nav-red");
        btnCompleted = new Button("Completed");
        btnCompleted.getStyleClass().addAll("btn", "btn-nav-green");
        btnCatTag = new Button("Categories/Tags");
        btnCatTag.getStyleClass().addAll("btn", "btn-nav");

        // --- Inline edit form fields ---
        tfAddTitle = new TextField(); tfAddTitle.setPromptText("Title");
        tfAddTitle.setMaxWidth(280);
        taAddDesc = new TextArea(); taAddDesc.setPromptText("Description"); taAddDesc.setPrefRowCount(2);
        taAddDesc.setMaxWidth(280);
        taEditDesc = new TextArea(); taEditDesc.setPromptText("Description"); taEditDesc.setPrefRowCount(2);
        taEditDesc.setMaxWidth(280);
        cbAddPriority = new ComboBox<>();
        cbAddPriority.getItems().addAll("LOW", "MEDIUM", "HIGH", "URGENT"); cbAddPriority.setValue("MEDIUM");
        cbEditStatus = new ComboBox<>();
        cbEditStatus.getItems().addAll("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED", "DUMMY");
        cbEditPriority = new ComboBox<>();
        cbEditPriority.getItems().addAll("LOW", "MEDIUM", "HIGH", "URGENT");
        cbStoreSessions = new CheckBox("Do not store sessions");
        cbStoreSessions.setSelected(false);
        cbStoreSessions.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");

        // Category and Tag combo boxes for add/edit
        cbAddCategory = new ComboBox<>();
        cbAddCategory.setPromptText("-- No Category --");
        cbAddCategory.setMaxWidth(280);
        cbAddTag = new ComboBox<>();
        cbAddTag.setPromptText("-- No Tag --");
        cbAddTag.setMaxWidth(280);
        cbEditCategory = new ComboBox<>();
        cbEditCategory.setPromptText("-- No Category --");
        cbEditCategory.setMaxWidth(280);
        cbEditTag = new ComboBox<>();
        cbEditTag.setPromptText("-- No Tag --");
        cbEditTag.setMaxWidth(280);

        // --- Detail mode ---
        detailBox = new VBox(6);
        detailBox.setAlignment(Pos.CENTER);
        Label dtlTitle = new Label("TASK DETAILS");
        dtlTitle.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 8 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(dtlTitle, ViewCoordinator.FONT_SECTION);
        VBox detail = new VBox(4); detail.setAlignment(Pos.CENTER);
        detail.getChildren().addAll(ctTitle, ctStatus, ctPriority, ctStoreFlag, ctDesc);
        HBox actRow = new HBox(12);
        actRow.setAlignment(Pos.CENTER);
        actRow.getChildren().addAll(btnAdd, btnEdit, btnDelete, btnCompleted, btnCatTag);
        btnAdd.setOnAction(e -> showAddForm());
        btnEdit.setOnAction(e -> showEditForm());
        btnDelete.setOnAction(e -> showDeleteForm());
        btnCompleted.setOnAction(e -> showCompleted());
        btnCatTag.setOnAction(e -> showCategoryTag());
        detailBox.getChildren().addAll(dtlTitle, detail, actRow);

        // --- Add mode ---
        addBox = new VBox(12);
        addBox.setAlignment(Pos.CENTER);
        Label addLbl = new Label("ADD TASK");
        addLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 8 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(addLbl, ViewCoordinator.FONT_SECTION);
        VBox addFields = new VBox(8);
        addFields.setAlignment(Pos.CENTER);
        addFields.getChildren().addAll(tfAddTitle, taAddDesc, cbAddPriority, cbAddCategory, cbAddTag, cbStoreSessions);
        Button btnAddOk = new Button("OK");
        btnAddOk.getStyleClass().addAll("btn", "btn-nav-green");
        Button btnAddCancel = new Button("Cancel");
        btnAddCancel.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox addBtns = new HBox(10); addBtns.setAlignment(Pos.CENTER);
        addBtns.getChildren().addAll(btnAddCancel, btnAddOk);
        addBox.getChildren().addAll(addLbl, addFields, addBtns);
        btnAddOk.setOnAction(e -> {
            String t = tfAddTitle.getText().trim();
            if (!t.isEmpty()) {
                Task newTask = taskService.create_task(t, taAddDesc.getText().trim(), Priority.valueOf(cbAddPriority.getValue()));
                newTask.set_store_sessions(!cbStoreSessions.isSelected());
                // Set category if selected
                if (cbAddCategory.getValue() != null) {
                    String selected = cbAddCategory.getValue();
                    int catId = Integer.parseInt(selected.split(" - ")[0]);
                    newTask.set_category_id(catId);
                }
                // Set tag if selected
                if (cbAddTag.getValue() != null) {
                    String selected = cbAddTag.getValue();
                    int tagId = Integer.parseInt(selected.split(" - ")[0]);
                    Tag tag = tagService.try_get_tag(tagId).orElse(null);
                    if (tag != null) newTask.add_tag(tag.get_name());
                }
                if (currentUser != null) {
                    newTask.set_assigned_user_id(currentUser.get_id());
                    userService.assign_task_to_user(currentUser.get_id(), newTask.get_id());
                    storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
                }
                tfAddTitle.clear(); taAddDesc.clear(); cbAddPriority.setValue("MEDIUM");
                cbAddCategory.setValue(null); cbAddTag.setValue(null);
                refresh();
                showDetail();
            }
        });
        btnAddCancel.setOnAction(e -> showDetail());

        // --- Edit mode ---
        editBox = new VBox(8);
        editBox.setAlignment(Pos.CENTER);
        Label editLbl = new Label("EDIT TASK");
        editLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(editLbl, ViewCoordinator.FONT_SECTION);
        VBox editFields = new VBox(6);
        editFields.setAlignment(Pos.CENTER);
        editFields.getChildren().addAll(new Label("Status:"), cbEditStatus, new Label("Description:"), taEditDesc, new Label("Priority:"), cbEditPriority, new Label("Category:"), cbEditCategory, new Label("Tag:"), cbEditTag);
        Button btnEditOk = new Button("OK");
        btnEditOk.getStyleClass().addAll("btn", "btn-nav-green");
        Button btnEditCancel = new Button("Cancel");
        btnEditCancel.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox editBtns = new HBox(10); editBtns.setAlignment(Pos.CENTER);
        editBtns.getChildren().addAll(btnEditCancel, btnEditOk);
        editBox.getChildren().addAll(editLbl, editFields, editBtns);
        btnEditOk.setOnAction(e -> {
            if (taskList.isEmpty()) return;
            Task t = taskList.get(currentIdx);
            taskService.update_task_status(t.get_id(), Status.valueOf(cbEditStatus.getValue()));
            taskService.update_task_priority(t.get_id(), Priority.valueOf(cbEditPriority.getValue()));
            taskService.update_task_description(t.get_id(), taEditDesc.getText().trim());
            // Update category
            if (cbEditCategory.getValue() != null) {
                String selected = cbEditCategory.getValue();
                int catId = Integer.parseInt(selected.split(" - ")[0]);
                t.set_category_id(catId);
            } else {
                t.set_category_id(-1);
            }
            // Update tag
            if (cbEditTag.getValue() != null) {
                String selected = cbEditTag.getValue();
                int tagId = Integer.parseInt(selected.split(" - ")[0]);
                Tag tag = tagService.try_get_tag(tagId).orElse(null);
                if (tag != null) {
                    // Remove all existing tags and add the new one
                    t.get_tags().clear();
                    t.add_tag(tag.get_name());
                }
            } else {
                t.get_tags().clear();
            }
            if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
            refresh();
            showDetail();
        });
        btnEditCancel.setOnAction(e -> showDetail());

        // --- Delete mode ---
        delConfirm = new Label();
        ViewCoordinator.applyFont(delConfirm, ViewCoordinator.FONT_DETAIL);
        delConfirm.setStyle("-fx-text-fill: #cccccc;");
        cbDeleteSessions = new CheckBox("Delete sessions as well");
        cbDeleteSessions.setSelected(false);
        cbDeleteSessions.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        deleteBox = new VBox(8);
        deleteBox.setAlignment(Pos.CENTER);
        Label delLbl = new Label("DELETE TASK?");
        delLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #cc4444; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(delLbl, ViewCoordinator.FONT_SECTION);
        Button btnDelYes = new Button("Yes");
        btnDelYes.getStyleClass().addAll("btn", "btn-nav-red");
        Button btnDelNo = new Button("No");
        btnDelNo.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox delBtns = new HBox(10); delBtns.setAlignment(Pos.CENTER);
        delBtns.getChildren().addAll(btnDelNo, btnDelYes);
        deleteBox.getChildren().addAll(delLbl, delConfirm, cbDeleteSessions, delBtns);
        btnDelYes.setOnAction(e -> {
            if (taskList.isEmpty()) return;
            int taskId = taskList.get(currentIdx).get_id();
            taskService.delete_task(taskId);
            if (cbDeleteSessions.isSelected()) {
                pomodoroService.delete_pomodoros_for_task(taskId);
            }
            if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
            refresh();
            showDetail();
        });
        btnDelNo.setOnAction(e -> showDetail());

        // --- Completed mode ---
        ctComTitle = new Label(); ctComTitle.setStyle("-fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-padding: 0 0 4 0;");
        ctComTitle.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctComStatus = new Label(); ctComStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctComStatus.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctComPriority = new Label(); ctComPriority.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctComPriority.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctComDate = new Label(); ctComDate.setStyle("-fx-text-fill: #aaaaaa; -fx-padding: 2 0;");
        ctComDate.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctComCounter = new Label(); ctComCounter.setStyle("-fx-font-weight: bold; -fx-text-fill: #888888; -fx-padding: 4 0 0 0;");
        ctComCounter.setMaxWidth(ViewCoordinator.CONTENT_W);

        ctComPrev = new Button("\u25C0");
        ctComNext = new Button("\u25B6");
        for (Button b : new Button[]{ctComPrev, ctComNext}) {
            b.setShape(new javafx.scene.shape.Circle(14));
            b.setPrefSize(30, 30);
            b.setMinSize(30, 30);
            b.setStyle("-fx-background-color: #444444; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 0; -fx-background-radius: 15; -fx-cursor: hand; -fx-border-width: 0;");
        }
        ctComPrev.setOnAction(e -> navigateCompleted(-1));
        ctComNext.setOnAction(e -> navigateCompleted(1));

        completedBox = new VBox(8);
        completedBox.setAlignment(Pos.CENTER);
        Label comLbl = new Label("COMPLETED TASKS");
        comLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #4caf7a; -fx-padding: 0 0 8 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(comLbl, ViewCoordinator.FONT_SECTION);
        VBox comDetail = new VBox(4); comDetail.setAlignment(Pos.CENTER);
        comDetail.getChildren().addAll(ctComTitle, ctComStatus, ctComPriority, ctComDate, ctComCounter);
        HBox comNav = new HBox(10); comNav.setAlignment(Pos.CENTER);
        comNav.getChildren().addAll(ctComPrev, ctComNext);
        Button btnComBack = new Button("Back");
        btnComBack.getStyleClass().addAll("btn", "btn-nav-grey");
        btnComBack.setOnAction(e -> showDetail());
        completedBox.getChildren().addAll(comLbl, comDetail, comNav, btnComBack);

        // --- Category/Tag management mode ---
        categoryListView = new ListView<>();
        categoryListView.setPrefWidth(280);
        categoryListView.setPrefHeight(200);
        categoryListView.setCellFactory(lv -> new ListCell<Category>() {
            @Override
            protected void updateItem(Category cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) setText(null);
                else setText(cat.get_name() + (cat.get_description().isEmpty() ? "" : " - " + cat.get_description()));
            }
        });

        tagListView = new ListView<>();
        tagListView.setPrefWidth(280);
        tagListView.setPrefHeight(200);
        tagListView.setCellFactory(lv -> new ListCell<Tag>() {
            @Override
            protected void updateItem(Tag tag, boolean empty) {
                super.updateItem(tag, empty);
                if (empty || tag == null) setText(null);
                else {
                    setText(tag.get_name());
                    setStyle("-fx-text-fill: " + tag.get_color() + "; -fx-font-weight: bold;");
                }
            }
        });

        tfCatName = new TextField(); tfCatName.setPromptText("Category name"); tfCatName.setMaxWidth(280);
        tfCatDesc = new TextField(); tfCatDesc.setPromptText("Description (optional)"); tfCatDesc.setMaxWidth(280);
        tfTagName = new TextField(); tfTagName.setPromptText("Tag name"); tfTagName.setMaxWidth(280);
        cpTagColor = new ColorPicker(); cpTagColor.setValue(Color.web("#3498DB"));

        Button btnAddCat = new Button("Add Category");
        btnAddCat.getStyleClass().addAll("btn", "btn-nav-green");
        btnAddCat.setOnAction(e -> addCategory());
        Button btnDelCat = new Button("Delete Category");
        btnDelCat.getStyleClass().addAll("btn", "btn-nav-red");
        btnDelCat.setOnAction(e -> deleteCategory());
        Button btnAddTag = new Button("Add Tag");
        btnAddTag.getStyleClass().addAll("btn", "btn-nav-green");
        btnAddTag.setOnAction(e -> addTag());
        Button btnDelTag = new Button("Delete Tag");
        btnDelTag.getStyleClass().addAll("btn", "btn-nav-red");
        btnDelTag.setOnAction(e -> deleteTag());

        VBox catForm = new VBox(6);
        catForm.setAlignment(Pos.CENTER);
        catForm.getChildren().addAll(
            new Label("CATEGORIES"),
            categoryListView,
            tfCatName, tfCatDesc,
            new HBox(10, btnAddCat, btnDelCat)
        );
        ((HBox) catForm.getChildren().get(catForm.getChildren().size() - 1)).setAlignment(Pos.CENTER);

        VBox tagForm = new VBox(6);
        tagForm.setAlignment(Pos.CENTER);
        tagForm.getChildren().addAll(
            new Label("TAGS"),
            tagListView,
            tfTagName, cpTagColor,
            new HBox(10, btnAddTag, btnDelTag)
        );
        ((HBox) tagForm.getChildren().get(tagForm.getChildren().size() - 1)).setAlignment(Pos.CENTER);

        HBox catTagForm = new HBox(20);
        catTagForm.setAlignment(Pos.CENTER);
        catTagForm.getChildren().addAll(catForm, tagForm);

        Button btnCatTagBack = new Button("Back");
        btnCatTagBack.getStyleClass().addAll("btn", "btn-nav-grey");
        btnCatTagBack.setOnAction(e -> showDetail());

        categoryTagBox = new VBox(12);
        categoryTagBox.setAlignment(Pos.CENTER);
        Label ctLbl = new Label("CATEGORIES & TAGS");
        ctLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 8 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(ctLbl, ViewCoordinator.FONT_SECTION);
        categoryTagBox.getChildren().addAll(ctLbl, catTagForm, btnCatTagBack);

        // --- Center pane swaps modes ---
        centerPane = new StackPane(detailBox, addBox, editBox, deleteBox, completedBox, categoryTagBox);
        centerPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        showDetail();

        HBox contentRow = new HBox(0);
        contentRow.setAlignment(Pos.CENTER);
        contentRow.setPrefSize(660, 660);
        contentRow.getChildren().addAll(ctPrev, centerPane, ctNext);
        HBox.setHgrow(centerPane, javafx.scene.layout.Priority.ALWAYS);

        swipe = new SwipeContainer(buildOverview(), contentRow);
        getChildren().add(swipe);

        refresh();
    }

    private void showDetail() {
        detailBox.setVisible(true); detailBox.setManaged(true);
        addBox.setVisible(false); addBox.setManaged(false);
        editBox.setVisible(false); editBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        completedBox.setVisible(false); completedBox.setManaged(false);
        categoryTagBox.setVisible(false); categoryTagBox.setManaged(false);
        if (taskList.isEmpty()) {
            ctTitle.setText("No tasks");
            ctStatus.setText(""); ctPriority.setText(""); ctDesc.setText("");
            ctNavName.setText("\u2014");
            ctPrev.setDisable(true); ctNext.setDisable(true);
            return;
        }
        showCurrent();
    }

    private void showAddForm() {
        tfAddTitle.clear(); taAddDesc.clear(); cbAddPriority.setValue("MEDIUM"); cbStoreSessions.setSelected(false);
        // Refresh category and tag combo boxes
        refreshCategoryTagComboBoxes();
        cbAddCategory.setValue(null);
        cbAddTag.setValue(null);
        detailBox.setVisible(false); detailBox.setManaged(false);
        addBox.setVisible(true); addBox.setManaged(true);
        editBox.setVisible(false); editBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        completedBox.setVisible(false); completedBox.setManaged(false);
        categoryTagBox.setVisible(false); categoryTagBox.setManaged(false);
    }

    private void showEditForm() {
        if (taskList.isEmpty()) return;
        Task t = taskList.get(currentIdx);
        cbEditStatus.setValue(t.get_status().toString());
        cbEditPriority.setValue(t.get_priority().toString());
        taEditDesc.setText(t.get_description());
        // Refresh category and tag combo boxes
        refreshCategoryTagComboBoxes();
        // Set current category
        if (t.get_category_id() > 0) {
            Optional<Category> cat = categoryService.try_get_category(t.get_category_id());
            cat.ifPresent(c -> cbEditCategory.setValue(c.get_id() + " - " + c.get_name()));
        } else {
            cbEditCategory.setValue(null);
        }
        // Set current tag (first tag if any)
        if (!t.get_tags().isEmpty()) {
            String tagName = t.get_tags().get(0);
            Optional<Tag> tag = tagService.get_all_tags().stream().filter(tg -> tg.get_name().equals(tagName)).findFirst();
            tag.ifPresent(tg -> cbEditTag.setValue(tg.get_id() + " - " + tg.get_name()));
        } else {
            cbEditTag.setValue(null);
        }
        detailBox.setVisible(false); detailBox.setManaged(false);
        addBox.setVisible(false); addBox.setManaged(false);
        editBox.setVisible(true); editBox.setManaged(true);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        completedBox.setVisible(false); completedBox.setManaged(false);
        categoryTagBox.setVisible(false); categoryTagBox.setManaged(false);
    }

    // Refresh category and tag combo boxes
    private void refreshCategoryTagComboBoxes() {
        cbAddCategory.getItems().clear();
        cbAddCategory.getItems().add("-- No Category --");
        for (Category c : categoryService.get_all_categories()) {
            cbAddCategory.getItems().add(c.get_id() + " - " + c.get_name());
        }
        cbAddTag.getItems().clear();
        cbAddTag.getItems().add("-- No Tag --");
        for (Tag t : tagService.get_all_tags()) {
            cbAddTag.getItems().add(t.get_id() + " - " + t.get_name());
        }
        cbEditCategory.getItems().clear();
        cbEditCategory.getItems().add("-- No Category --");
        for (Category c : categoryService.get_all_categories()) {
            cbEditCategory.getItems().add(c.get_id() + " - " + c.get_name());
        }
        cbEditTag.getItems().clear();
        cbEditTag.getItems().add("-- No Tag --");
        for (Tag t : tagService.get_all_tags()) {
            cbEditTag.getItems().add(t.get_id() + " - " + t.get_name());
        }
    }

    private void showDeleteForm() {
        if (taskList.isEmpty()) return;
        delConfirm.setText("Delete \"" + taskList.get(currentIdx).get_title() + "\"?");
        cbDeleteSessions.setSelected(false);
        detailBox.setVisible(false); detailBox.setManaged(false);
        addBox.setVisible(false); addBox.setManaged(false);
        editBox.setVisible(false); editBox.setManaged(false);
        deleteBox.setVisible(true); deleteBox.setManaged(true);
        completedBox.setVisible(false); completedBox.setManaged(false);
        categoryTagBox.setVisible(false); categoryTagBox.setManaged(false);
    }

    private void showCompleted() {
        refreshCompletedList();
        detailBox.setVisible(false); detailBox.setManaged(false);
        addBox.setVisible(false); addBox.setManaged(false);
        editBox.setVisible(false); editBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        completedBox.setVisible(true); completedBox.setManaged(true);
        categoryTagBox.setVisible(false); categoryTagBox.setManaged(false);
    }

    private void refreshCompletedList() {
        completedTaskList = taskService.filter_by_status(Status.COMPLETED);
        completedIdx = 0;
        showCompletedDetail();
    }

    private void showCompletedDetail() {
        if (completedTaskList.isEmpty()) {
            ctComTitle.setText("No completed tasks");
            ctComStatus.setText(""); ctComPriority.setText(""); ctComDate.setText("");
            ctComCounter.setText("");
            ctComPrev.setDisable(true); ctComNext.setDisable(true);
            return;
        }
        Task t = completedTaskList.get(completedIdx);
        ctComTitle.setText(t.get_title());
        ViewCoordinator.fitDetailTitle(ctComTitle, t.get_title());
        ViewCoordinator.fitDetail(ctComStatus, "Status: " + t.get_status());
        String pc = priorityColor(t.get_priority());
        ctComPriority.setStyle("-fx-font-weight: bold; -fx-padding: 2 0; -fx-text-fill: " + pc + ";");
        ViewCoordinator.fitDetail(ctComPriority, "Priority: " + t.get_priority());
        ViewCoordinator.fitDetail(ctComDate, "Completed: " + (t.get_completed_at() != null ? t.get_completed_at().toLocalDate().toString() : "Unknown"));
        ViewCoordinator.fitDetail(ctComCounter, (completedIdx + 1) + " of " + completedTaskList.size());
        ctComPrev.setDisable(completedIdx <= 0);
        ctComNext.setDisable(completedIdx >= completedTaskList.size() - 1);
    }

    private void navigateCompleted(int dir) {
        int ni = completedIdx + dir;
        if (ni >= 0 && ni < completedTaskList.size()) {
            completedIdx = ni;
            showCompletedDetail();
        }
    }

    private Node buildOverview() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(660, 660);
        box.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.setPadding(new Insets(0, 10, 0, 10));
        Label title = new Label("Tasks");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #aaaaaa; -fx-padding: 16 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_PAGE_TITLE);
        HBox row = new HBox(ViewCoordinator.CARD_GAP);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(ViewCoordinator.CONTENT_W);
        row.getChildren().addAll(
            ViewCoordinator.statCard(ovTotal, "Total", 4),
            ViewCoordinator.statCard(ovDone, "Done", 4),
            ViewCoordinator.statCard(ovUrgent, "Urgent", 4),
            ViewCoordinator.statCard(ovOverdue, "Overdue", 4));
        Label bt = new Label("PRIORITY BREAKDOWN");
        bt.setStyle("-fx-font-weight: 900; -fx-text-fill: #999999; -fx-padding: 6 0 4 0; -fx-letter-spacing: 1px;");
        ViewCoordinator.applyFont(bt, ViewCoordinator.FONT_SECTION);
        VBox bd = new VBox(4); bd.setAlignment(Pos.CENTER);
        bd.getChildren().addAll(ovUrgentLbl, ovHighLbl, ovMedLbl, ovLowLbl);
        box.getChildren().addAll(title, row, bt, bd);
        return box;
    }

    // Called by buttons in the detail action row
    public void triggerAdd() { showAddForm(); }
    public void triggerEdit() { showEditForm(); }
    public void triggerDelete() { showDeleteForm(); }

    public void showContentPage() { refresh(); swipe.restoreLastPage(); }
    public void refresh() {
        taskList = taskService.filter_incomplete_tasks();
        currentIdx = Math.min(currentIdx, Math.max(0, taskList.size() - 1));
        showCurrent();
        updateOverview();
    }

    private void showCurrent() {
        if (taskList.isEmpty()) {
            ctTitle.setText("No tasks");
            ViewCoordinator.fitDetailTitle(ctTitle, "No tasks");
            ctStatus.setText(""); ctPriority.setText(""); ctDesc.setText(""); ctStoreFlag.setText("");
            ctNavName.setText("");
            ctPrev.setDisable(true); ctNext.setDisable(true);
            return;
        }
        Task t = taskList.get(currentIdx);
        ctTitle.setText(t.get_title());
        ViewCoordinator.fitDetailTitle(ctTitle, t.get_title());
        ViewCoordinator.fitDetail(ctStatus, "Status: " + t.get_status());
        String pc = priorityColor(t.get_priority());
        ctPriority.setStyle("-fx-font-weight: bold; -fx-padding: 1 0; -fx-text-fill: " + pc + ";");
        ViewCoordinator.fitDetail(ctPriority, "Priority: " + t.get_priority());
        ctDesc.setText(t.get_description().isEmpty() ? "" : t.get_description());
        ViewCoordinator.fitTextWrap(ctDesc, t.get_description().isEmpty() ? "" : t.get_description(),
                                ViewCoordinator.CONTENT_W,
                                ViewCoordinator.FONT_DETAIL_TITLE, 9,
                                160);
        if (t.is_store_sessions()) {
            ctStoreFlag.setText("");
        } else {
            ctStoreFlag.setText("not stored");
        }
        ViewCoordinator.fitDetail(ctStoreFlag, ctStoreFlag.getText());
        ctNavName.setText(t.get_title());
        ViewCoordinator.fitText(ctNavName, t.get_title(), ViewCoordinator.CONTENT_W, ViewCoordinator.FONT_SECTION, 9);
        ctPrev.setDisable(currentIdx <= 0);
        ctNext.setDisable(currentIdx >= taskList.size() - 1);
    }

    private void updateOverview() {
        int total = taskService.count_tasks();
        int completed = taskService.count_completed_tasks();
        int urgentCount = taskService.get_urgent_incomplete_tasks().size();
        int overdueCount = taskService.count_overdue_tasks();
        ovTotal.setText(String.valueOf(total));
        ovDone.setText(total > 0 ? (completed * 100 / total) + "%" : "0%");
        ovUrgent.setText(String.valueOf(urgentCount));
        ovOverdue.setText(String.valueOf(overdueCount));
        ViewCoordinator.fitStatValue(ovTotal, ovTotal.getText(), 4);
        ViewCoordinator.fitStatValue(ovDone, ovDone.getText(), 4);
        ViewCoordinator.fitStatValue(ovUrgent, ovUrgent.getText(), 4);
        ViewCoordinator.fitStatValue(ovOverdue, ovOverdue.getText(), 4);
        int low = taskService.filter_by_priority(Priority.LOW).size();
        int med = taskService.filter_by_priority(Priority.MEDIUM).size();
        int high = taskService.filter_by_priority(Priority.HIGH).size();
        int urg = taskService.filter_by_priority(Priority.URGENT).size();
        ovUrgentLbl.setText("\u25CF Urgent: " + urg); ovUrgentLbl.setStyle("-fx-text-fill: #dc2828; -fx-padding: 2 0;"); ViewCoordinator.applyFont(ovUrgentLbl, ViewCoordinator.FONT_DETAIL);
        ovHighLbl.setText("\u25CF High: " + high); ovHighLbl.setStyle("-fx-text-fill: #dc6400; -fx-padding: 2 0;"); ViewCoordinator.applyFont(ovHighLbl, ViewCoordinator.FONT_DETAIL);
        ovMedLbl.setText("\u25CF Medium: " + med); ovMedLbl.setStyle("-fx-text-fill: #c8a000; -fx-padding: 2 0;"); ViewCoordinator.applyFont(ovMedLbl, ViewCoordinator.FONT_DETAIL);
        ovLowLbl.setText("\u25CF Low: " + low); ovLowLbl.setStyle("-fx-text-fill: #00c864; -fx-padding: 2 0;"); ViewCoordinator.applyFont(ovLowLbl, ViewCoordinator.FONT_DETAIL);
    }

    private String priorityColor(Priority p) {
        switch (p) { case URGENT: return "#dc2828"; case HIGH: return "#dc6400"; case MEDIUM: return "#c8a000"; default: return "#00c864"; }
    }

    // --- Category/Tag management methods ---
    private void showCategoryTag() {
        detailBox.setVisible(false); detailBox.setManaged(false);
        addBox.setVisible(false); addBox.setManaged(false);
        editBox.setVisible(false); editBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        completedBox.setVisible(false); completedBox.setManaged(false);
        refreshCategoryTagLists();
        categoryTagBox.setVisible(true); categoryTagBox.setManaged(true);
    }

    private void refreshCategoryTagLists() {
        categoryList.clear();
        categoryList.addAll(categoryService.get_all_categories());
        categoryListView.setItems(FXCollections.observableArrayList(categoryList));

        tagList.clear();
        tagList.addAll(tagService.get_all_tags());
        tagListView.setItems(FXCollections.observableArrayList(tagList));
    }

    private void addCategory() {
        String name = tfCatName.getText().trim();
        if (name.isEmpty()) return;
        String desc = tfCatDesc.getText().trim();
        categoryService.create_category(name, desc);
        if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
        tfCatName.clear(); tfCatDesc.clear();
        refreshCategoryTagLists();
    }

    private void deleteCategory() {
        Category selected = categoryListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        categoryService.delete_category(selected.get_id());
        if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
        refreshCategoryTagLists();
    }

    private void addTag() {
        String name = tfTagName.getText().trim();
        if (name.isEmpty()) return;
        String color = cpTagColor.getValue().toString().replace("0x", "#");
        tagService.create_tag(name, color);
        if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
        tfTagName.clear();
        refreshCategoryTagLists();
    }

    private void deleteTag() {
        Tag selected = tagListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        tagService.delete_tag(selected.get_id());
        if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
        refreshCategoryTagLists();
    }
}
