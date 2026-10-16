import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jfx.LoginView;
import jfx.MainView;
import models.User;
import services.*;
import utils.FileUtils;

import java.util.List;

public class Main extends Application {
    private TaskService taskService;
    private UserService userService;
    private PomodoroService pomodoroService;
    private CategoryService categoryService;
    private TagService tagService;
    private JsonStorage storage;
    private SettingsService settingsService;
    private MainView mainView;
    private User currentUser;
    private final services.PostgresUserDAO postgresUserDAO = new services.PostgresUserDAO();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        taskService = new TaskService();
        userService = new UserService();
        pomodoroService = new PomodoroService();
        categoryService = new CategoryService();
        tagService = new TagService();
        storage = new JsonStorage();
        settingsService = new SettingsService();

        // Load user registry first
        if (storage.has_user_registry()) {
            storage.load_user_registry(userService);
            System.out.println("Loaded user registry: " + userService.count_users() + " users");
        }

        // Load users from PostgreSQL and merge
        List<models.User> pgUsers = postgresUserDAO.getAllUsers();
        for (models.User pgUser : pgUsers) {
            if (!userService.try_get_user((int) pgUser.get_id()).isPresent()) {
                userService.add_user_direct(pgUser);
                System.out.println("  → Loaded user from PostgreSQL: " + pgUser.get_name() + " (" + pgUser.get_role() + ")");
            }
        }
        System.out.println("Total users after PostgreSQL merge: " + userService.count_users());

        // Migrate legacy data if needed
        storage.migrate_legacy_data_if_needed(taskService, userService, pomodoroService, settingsService);

        // Re-merge PostgreSQL users after potential migration (migration reloads JSON only)
        List<models.User> pgUsersAfter = postgresUserDAO.getAllUsers();
        for (models.User pgUser : pgUsersAfter) {
            if (!userService.try_get_user((int) pgUser.get_id()).isPresent()) {
                userService.add_user_direct(pgUser);
                System.out.println("  → Loaded user from PostgreSQL: " + pgUser.get_name() + " (" + pgUser.get_role() + ")");
            }
        }
        System.out.println("Total users after final merge: " + userService.count_users());

        LoginView loginView = new LoginView(userService);
        Scene loginScene = new Scene(loginView, 760, 760);
        loginScene.setFill(Color.TRANSPARENT);
        loginScene.getStylesheets().add("style.css");

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Chronos");
        stage.getIcons().add(new Image("file:icon.png"));
        stage.setResizable(false);
        stage.setScene(loginScene);
        stage.centerOnScreen();
        stage.show();

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
        pause.setOnFinished(e -> {
            stage.toFront();
            loginView.requestFocus();
        });
        pause.play();

        final javafx.animation.Timeline[] pollerHolder = new javafx.animation.Timeline[1];
        pollerHolder[0] = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), ev -> {
                if (loginView.isLoginComplete()) {
                    User selected = loginView.getSelectedUser();
                    Platform.runLater(() -> {
                        pollerHolder[0].stop();
                        openMain(stage, selected);
                    });
                }
            })
        );
        pollerHolder[0].setCycleCount(javafx.animation.Animation.INDEFINITE);
        pollerHolder[0].play();

        stage.setOnCloseRequest(ev -> {
            pollerHolder[0].stop();
            save_and_exit();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                save_and_exit();
            } catch (Exception e) {
                System.err.println("Shutdown save failed: " + e.getMessage());
            }
        }));
    }

    private void openMain(Stage stage, User selectedUser) {
        // Save previous user's data if switching
        if (currentUser != null && mainView != null) {
            storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
        }

        currentUser = selectedUser;

        // Reset services for new user
        taskService = new TaskService();
        pomodoroService = new PomodoroService();
        categoryService = new CategoryService();
        tagService = new TagService();

        if (selectedUser != null) {
            // Load user's data
            storage.load_user_data(selectedUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
            System.out.println("Loaded data for user: " + selectedUser.get_name());
        } else {
            System.out.println("Starting as guest - no data loaded");
        }

        mainView = new MainView(selectedUser, taskService, userService, pomodoroService, storage, settingsService, categoryService, tagService, postgresUserDAO, this::logout_and_relogin);
        Scene mainScene = new Scene(mainView, 760, 760);
        mainScene.setFill(null);
        mainScene.getStylesheets().add("style.css");
        stage.setScene(mainScene);
        stage.centerOnScreen();
    }

    private void logout_and_relogin() {
        // Save current user's data before logout
        if (currentUser != null && mainView != null) {
            storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
        }

        // Reset to login screen
        currentUser = null;
        taskService = new TaskService();
        pomodoroService = new PomodoroService();
        categoryService = new CategoryService();
        tagService = new TagService();
        settingsService = new SettingsService();

        LoginView loginView = new LoginView(userService);
        Scene loginScene = new Scene(loginView, 760, 760);
        loginScene.setFill(Color.TRANSPARENT);
        loginScene.getStylesheets().add("style.css");

        Stage stage = (Stage) mainView.getScene().getWindow();
        stage.setScene(loginScene);
        stage.centerOnScreen();

        // Restart poller
        final javafx.animation.Timeline[] pollerHolder = new javafx.animation.Timeline[1];
        pollerHolder[0] = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), ev -> {
                if (loginView.isLoginComplete()) {
                    User selected = loginView.getSelectedUser();
                    Platform.runLater(() -> {
                        pollerHolder[0].stop();
                        openMain(stage, selected);
                    });
                }
            })
        );
        pollerHolder[0].setCycleCount(javafx.animation.Animation.INDEFINITE);
        pollerHolder[0].play();
    }

    private void save_and_exit() {
        if (storage != null && taskService != null && userService != null && pomodoroService != null && settingsService != null && categoryService != null && tagService != null) {
            if (currentUser != null) {
                storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
                System.out.println("Saved data for user: " + currentUser.get_name());
            } else {
                System.out.println("Guest session - no data saved");
            }
        }
        if (mainView != null) mainView.cleanup();
        Platform.exit();
    }
}
