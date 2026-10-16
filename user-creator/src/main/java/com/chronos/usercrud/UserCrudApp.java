package com.chronos.usercrud;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

/**
 * UserCrudApp - standalone user/role management tool.
 * Connects directly to PostgreSQL and manages users independently.
 */
public class UserCrudApp extends Application {
    private final UserDAO userDAO = new UserDAO();
    private final ListView<User> userListView = new ListView<>();
    private final TextField nameField = new TextField();
    private final TextField emailField = new TextField();
    private final ComboBox<User.Role> roleComboBox = new ComboBox<>();
    private final Label statusLabel = new Label();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Chronos User Manager");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1a1a1a;");

        // Title
        Label title = new Label("USER MANAGEMENT");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #4caf7a; -fx-font-size: 18px; -fx-letter-spacing: 2px;");

        // Status label
        statusLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");

        // Form fields
        nameField.setPromptText("Name");
        nameField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #666666;");
        nameField.setMaxWidth(300);

        emailField.setPromptText("Email");
        emailField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #666666;");
        emailField.setMaxWidth(300);

        roleComboBox.getItems().addAll(User.Role.ADMIN, User.Role.MANAGER, User.Role.DEVELOPER);
        roleComboBox.setValue(User.Role.DEVELOPER);
        roleComboBox.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ffffff;");
        roleComboBox.setMaxWidth(300);

        // Buttons
        Button btnCreate = new Button("Create User");
        btnCreate.getStyleClass().addAll("btn", "btn-nav-green");
        btnCreate.setOnAction(e -> createUser());

        Button btnUpdate = new Button("Update Role");
        btnUpdate.getStyleClass().addAll("btn", "btn-nav");
        btnUpdate.setOnAction(e -> updateUserRole());

        Button btnDelete = new Button("Delete User");
        btnDelete.getStyleClass().addAll("btn", "btn-nav-red");
        btnDelete.setOnAction(e -> deleteUser());

        Button btnRefresh = new Button("Refresh");
        btnRefresh.getStyleClass().addAll("btn", "btn-nav-grey");
        btnRefresh.setOnAction(e -> refreshUserList());

        HBox buttonBox = new HBox(10, btnCreate, btnUpdate, btnDelete, btnRefresh);
        buttonBox.setAlignment(Pos.CENTER);

        // User list
        userListView.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ffffff;");
        userListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(String.format("%s | %s | %s | ID: %d",
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getId()));
                }
            }
        });

        VBox formBox = new VBox(10, nameField, emailField, roleComboBox);
        formBox.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, statusLabel, formBox, buttonBox, userListView);
        VBox.setVgrow(userListView, Priority.ALWAYS);

        Scene scene = new Scene(root, 600, 500);
        scene.setFill(Color.rgb(30, 30, 30));

        primaryStage.setScene(scene);
        primaryStage.show();

        refreshUserList();
    }

    private void createUser() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        User.Role role = roleComboBox.getValue();

        if (name.isEmpty() || email.isEmpty()) {
            showStatus("Please fill in all fields", "#cc4444");
            return;
        }

        if (userDAO.userExists(email)) {
            showStatus("User with this email already exists", "#cc4444");
            return;
        }

        User user = new User(name, email, role);
        if (userDAO.createUser(user)) {
            showStatus("User created successfully: " + name, "#4caf7a");
            clearForm();
            refreshUserList();
        } else {
            showStatus("Failed to create user", "#cc4444");
        }
    }

    private void updateUserRole() {
        User selected = userListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select a user", "#cc4444");
            return;
        }

        User.Role newRole = roleComboBox.getValue();
        selected.setRole(newRole);
        selected.setUpdatedAt(java.time.LocalDateTime.now());

        if (userDAO.updateUser(selected)) {
            showStatus("User role updated to: " + newRole, "#4caf7a");
            refreshUserList();
        } else {
            showStatus("Failed to update user", "#cc4444");
        }
    }

    private void deleteUser() {
        User selected = userListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select a user", "#cc4444");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete User");
        confirm.setContentText("Are you sure you want to delete " + selected.getName() + "?");
        confirm.initStyle(StageStyle.TRANSPARENT);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (userDAO.deleteUser(selected.getId())) {
                showStatus("User deleted: " + selected.getName(), "#4caf7a");
                refreshUserList();
            } else {
                showStatus("Failed to delete user", "#cc4444");
            }
        }
    }

    private void refreshUserList() {
        List<User> users = userDAO.getAllUsers();
        userListView.getItems().clear();
        userListView.getItems().addAll(users);
        showStatus("Loaded " + users.size() + " users", "#888888");
    }

    private void clearForm() {
        nameField.clear();
        emailField.clear();
        roleComboBox.setValue(User.Role.DEVELOPER);
    }

    private void showStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
    }
}
