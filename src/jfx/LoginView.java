package jfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import models.User;
import services.UserService;

import java.util.List;

/**
 * LoginView - initial screen shown on app start.
 * Styled like the PomodoroView completion notification.
 * Lists existing users for selection; Cancel enters as GUEST.
 */
import javafx.scene.shape.Rectangle;

public class LoginView extends Pane {
    private final UserService userService;
    private User selectedUser;
    private boolean loginComplete;

    public LoginView(UserService us) {
        this.userService = us;
        this.loginComplete = false;
        setPrefSize(760, 760);
        setPickOnBounds(false);

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #4caf7a; -fx-border-width: 2; -fx-border-radius: 10;");

        Label title = new Label("Chronos");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #4caf7a; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_PAGE_TITLE);
        title.setAlignment(Pos.CENTER);
        title.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label subtitle = new Label("Select your profile to continue");
        subtitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc; -fx-padding: 0 0 12 0;");
        ViewCoordinator.applyFont(subtitle, ViewCoordinator.FONT_DETAIL);
        subtitle.setAlignment(Pos.CENTER);
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        VBox userList = new VBox(8);
        userList.setAlignment(Pos.CENTER);
        List<User> users = userService.get_all_users();
        if (users.isEmpty()) {
            Label empty = new Label("No users found. Starting as guest.");
            empty.setStyle("-fx-text-fill: #888888; -fx-font-size: 12px;");
            userList.getChildren().add(empty);
        } else {
            for (User u : users) {
                Button btn = new Button(u.get_name() + "  (" + u.get_role() + ")");
                btn.getStyleClass().addAll("btn", "btn-nav");
                btn.setMaxWidth(260);
                btn.setOnAction(e -> selectUser(u));
                userList.getChildren().add(btn);
            }
        }

        Button btnCancel = new Button("Cancel — Enter as Guest");
        btnCancel.getStyleClass().addAll("btn", "btn-nav-grey");
        btnCancel.setMaxWidth(260);
        btnCancel.setOnAction(e -> selectUser(null));

        content.getChildren().addAll(title, subtitle, userList, btnCancel);
        content.setMaxWidth(400);
        content.setLayoutX(180);
        content.setLayoutY(130);
        getChildren().add(content);
    }

    private void selectUser(User user) {
        this.selectedUser = user;
        this.loginComplete = true;
    }

    public User getSelectedUser() {
        return selectedUser;
    }

    public boolean isLoginComplete() {
        return loginComplete;
    }
}
