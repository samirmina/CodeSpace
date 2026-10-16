package jfx;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import models.User;
import services.*;

public class MainView extends Pane {
    private static final double W = 760, H = 760;
    private static final double CX = W / 2, CY = H / 2;
    private static final double R = W / 2 - 50;
    private static final double CONTENT_W = 660, CONTENT_H = 660;
    private static final double CONTENT_X = (W - CONTENT_W) / 2;
    private static final double CONTENT_Y = (H - CONTENT_H) / 2;
    private static final double DRAG_THRESHOLD = 0.82;
    private static final double[] ANGLES = {-90, -30, 30, 90, 150};
    private static final String[] LABELS = {"Timer", "Tasks", "Users", "Reports", "Sessions"};
    private static final int N = 5;

    private final User currentUser;
    private final SettingsService settingsService;
    private final services.PostgresUserDAO postgresUserDAO;
    private final StackPane contentArea;
    private final PomodoroView pomodoroView;
    private final TaskView taskView;
    private final UserView userView;
    private final ReportView reportView;
    private final SessionsView sessionsView;
    private final Button[] edgeBtns = new Button[N];
    private final Circle[] dots = new Circle[N];
    private final double[][] positions = new double[N][2];
    private final Button upBtn, downBtn;
    private int currentTab;
    private final Runnable onClose;

    public MainView(User currentUser, TaskService ts, UserService us, PomodoroService ps, JsonStorage storage, SettingsService settingsService, CategoryService categoryService, TagService tagService, services.PostgresUserDAO postgresUserDAO, Runnable onClose) {
        this.currentUser = currentUser;
        this.settingsService = settingsService;
        this.postgresUserDAO = postgresUserDAO;
        this.onClose = onClose;
        setPrefSize(W, H);
        setPickOnBounds(false);

        pomodoroView = new PomodoroView(ps, ts, us, storage, this::switch_tab, currentUser, settingsService, categoryService, tagService);
        taskView = new TaskView(ts, us, ps, storage, currentUser, settingsService, categoryService, tagService);
        userView = new UserView(ts, us, ps, storage, currentUser, settingsService, postgresUserDAO, categoryService, tagService);
        reportView = new ReportView(ts, us, ps);
        sessionsView = new SessionsView(ps, ts, us, storage, currentUser, settingsService, categoryService, tagService);

        contentArea = new StackPane();
        contentArea.setPrefSize(CONTENT_W, CONTENT_H);
        contentArea.setLayoutX(CONTENT_X);
        contentArea.setLayoutY(CONTENT_Y);
        contentArea.getStyleClass().add("content-area");
        contentArea.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.12)));
        contentArea.getChildren().addAll(pomodoroView, taskView, userView, reportView, sessionsView);
        pomodoroView.setVisible(true);
        pomodoroView.refresh();
        taskView.setVisible(false);
        userView.setVisible(false);
        reportView.setVisible(false);
        sessionsView.setVisible(false);
        getChildren().add(contentArea);

        // Bezel ring
        Circle bezel = new Circle(CX, CY, R - 2);
        bezel.setFill(null);
        bezel.setStroke(Color.rgb(210, 210, 210));
        bezel.setStrokeWidth(3);
        bezel.setMouseTransparent(true);
        getChildren().add(bezel);

        // Tick marks
        Canvas ticks = new Canvas(W, H);
        ticks.setMouseTransparent(true);
        GraphicsContext tg = ticks.getGraphicsContext2D();
        tg.setStroke(Color.rgb(190, 190, 190));
        tg.setLineWidth(1.2);
        for (int i = 0; i < 12; i++) {
            double rad = Math.toRadians(i * 30 - 90);
            int len = (i % 3 == 0) ? 14 : 8;
            double x1 = CX + (R - 24) * Math.cos(rad);
            double y1 = CY + (R - 24) * Math.sin(rad);
            double x2 = CX + (R - 24 + len) * Math.cos(rad);
            double y2 = CY + (R - 24 + len) * Math.sin(rad);
            tg.strokeLine(x1, y1, x2, y2);
        }
        getChildren().add(ticks);

        // Edge buttons
        for (int i = 0; i < N; i++) {
            double rad = Math.toRadians(ANGLES[i]);
            positions[i][0] = CX + R * 0.80 * Math.cos(rad);
            positions[i][1] = CY + R * 0.80 * Math.sin(rad);

            Circle dot = new Circle(3, Color.rgb(180, 180, 180, 0.5));
            dot.setCenterX(positions[i][0]);
            dot.setCenterY(positions[i][1]);
            dots[i] = dot;
            getChildren().add(dot);

            int idx = i;
            Button btn = new Button(String.valueOf(LABELS[i].charAt(0)));
            btn.setOpacity(0);
            btn.setScaleX(0.5);
            btn.setScaleY(0.5);
            btn.setLayoutX(positions[i][0] - 16);
            btn.setLayoutY(positions[i][1] - 16);
            btn.setPrefSize(32, 32);
            btn.setShape(new Circle(16));
            btn.getStyleClass().add("edge-nav");
            edgeBtns[i] = btn;
            btn.setOnAction(e -> switch_tab(idx));
            getChildren().add(btn);
        }

        upBtn = new Button("\u25B2");
        upBtn.setLayoutX(CX - 18);
        upBtn.setLayoutY(CY - R * 0.9 - 18);
        upBtn.setPrefSize(36, 36);
        upBtn.setShape(new Circle(18));
        upBtn.getStyleClass().add("edge-nav-lg");
        upBtn.setOnAction(e -> switch_tab((currentTab - 1 + N) % N));
        getChildren().add(upBtn);

        downBtn = new Button("\u25BC");
        downBtn.setLayoutX(CX - 18);
        downBtn.setLayoutY(CY + R * 0.9 - 18);
        downBtn.setPrefSize(36, 36);
        downBtn.setShape(new Circle(18));
        downBtn.getStyleClass().add("edge-nav-lg");
        downBtn.setOnAction(e -> switch_tab((currentTab + 1) % N));
        getChildren().add(downBtn);

        Button logoutBtn = new Button("Logout");
        logoutBtn.setLayoutX(CX - 30);
        logoutBtn.setLayoutY(CY + R * 0.9 + 24);
        logoutBtn.setPrefSize(60, 20);
        logoutBtn.getStyleClass().add("btn-logout");
        logoutBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });
        getChildren().add(logoutBtn);

        Circle clip = new Circle(CX, CY, R);
        setClip(clip);

        addEventHandler(MouseEvent.MOUSE_MOVED, e -> handle_hover(e.getX(), e.getY()));
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            for (int i = 0; i < N; i++) hide_edge(i);
        });

        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            double nx = (e.getX() - CX) / (R * DRAG_THRESHOLD);
            double ny = (e.getY() - CY) / (R * DRAG_THRESHOLD);
            if (nx * nx + ny * ny > 1) {
                xOff = e.getSceneX(); yOff = e.getSceneY();
            } else {
                xOff = Double.NaN;
            }
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (Double.isNaN(xOff)) return;
            javafx.stage.Stage st = (javafx.stage.Stage) getScene().getWindow();
            st.setX(e.getScreenX() - xOff);
            st.setY(e.getScreenY() - yOff);
        });

        contentArea.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            swipeStartX = e.getX(); swipeStartY = e.getY();
            verticalSwipe = false;
        });
        contentArea.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (verticalSwipe) { e.consume(); return; }
            double dy = e.getY() - swipeStartY;
            double dx = e.getX() - swipeStartX;
            if (Math.abs(dy) > 15 && Math.abs(dy) > Math.abs(dx) * 2) {
                verticalSwipe = true;
                e.consume();
            }
        });
        contentArea.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (!verticalSwipe) return;
            double dy = e.getY() - swipeStartY;
            if (Math.abs(dy) > 60) {
                e.consume();
                if (dy < 0) switch_tab((currentTab - 1 + N) % N);
                else switch_tab((currentTab + 1) % N);
            }
        });
    }

    private double xOff = Double.NaN, yOff, swipeStartX, swipeStartY;
    private boolean verticalSwipe;

    private void handle_hover(double mx, double my) {
        for (int i = 0; i < N; i++) {
            double d = Math.sqrt(Math.pow(mx - positions[i][0], 2) + Math.pow(my - positions[i][1], 2));
            boolean near = d < 60;
            dots[i].setRadius(near ? 4.5 : 3);
            dots[i].setOpacity(near ? 0.8 : 0.4);
            if (near && edgeBtns[i].getOpacity() < 0.01) show_edge(i);
            else if (!near && edgeBtns[i].getOpacity() > 0.01) hide_edge(i);
        }
    }

    private void show_edge(int i) {
        Button b = edgeBtns[i];
        FadeTransition ft = new FadeTransition(Duration.millis(220), b);
        ft.setToValue(1);
        ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(220), b);
        st.setToX(1); st.setToY(1);
        st.play();
    }

    private void hide_edge(int i) {
        Button b = edgeBtns[i];
        FadeTransition ft = new FadeTransition(Duration.millis(160), b);
        ft.setToValue(0);
        ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(160), b);
        st.setToX(0.5); st.setToY(0.5);
        st.play();
    }

    public void cleanup() { pomodoroView.cleanup(); }

    public boolean isGuestSession() {
        return currentUser == null;
    }

    private boolean isTabVisible(int idx) {
        if (currentUser == null) {
            return idx == 0 || idx == 3 || idx == 4;
        }
        return true;
    }

    private void switch_tab(int idx) {
        if (!isTabVisible(idx)) return;
        currentTab = idx;
        for (Node n : contentArea.getChildren()) n.setVisible(false);
        contentArea.getChildren().get(idx).setVisible(true);
        pomodoroView.on_hide();
        if (idx == 0) pomodoroView.showContentPage();
        else if (idx == 1) taskView.showContentPage();
        else if (idx == 2) userView.showContentPage();
        else if (idx == 3) reportView.showContentPage();
        else if (idx == 4) sessionsView.showContentPage();
    }
}
