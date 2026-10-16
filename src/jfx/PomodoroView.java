package jfx;

import java.awt.Toolkit;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import models.Pomodoro;
import models.Status;
import models.Task;
import models.User;
import services.*;
import utils.AudioPlayer;

public class PomodoroView extends StackPane {
    private final PomodoroService ps;
    private final TaskService ts;
    private final UserService us;
    private final JsonStorage storage;
    private final Consumer<Integer> onTabSwitch;
    private final User currentUser;
    private final SettingsService settingsService;
    private final CategoryService categoryService;
    private final TagService tagService;

    private int timeRemaining, totalTime;
    private boolean isRunning, isPaused;
    private Pomodoro current;

    private final Canvas canvas;
    private final Label statusLabel;
    private final ComboBox<String> taskBox;
    private final Spinner<Integer> spinner;
    private Button btnStart, btnPause, btnReset;

    private final SwipeContainer swipe;

    private final StackPane compTasks, compSessions, compFocus, compToday;
    private final Label compTasksVal, compSessionsVal, compFocusVal, compTodayVal;

    private final Label ovSessions, ovFocus, ovTasks, ovToday;

    private Timeline countdown;
    private AnimationTimer glow;
    private long endTimeMillis;
    private boolean runningReachedZero;
    private float pulse;
    private javafx.stage.Stage completionStage;
    private boolean suppressSelect;

    private static final Color ARC = Color.rgb(180, 180, 180);
    private static final Color ARC_DIM = Color.rgb(100, 100, 100);
    private static final Color TRACK = Color.rgb(60, 60, 60);

    public PomodoroView(PomodoroService ps, TaskService ts, UserService us, JsonStorage s, Consumer<Integer> onTab, User currentUser, SettingsService settingsService, CategoryService categoryService, TagService tagService) {
        this.ps = ps; this.ts = ts; this.us = us; this.storage = s; this.onTabSwitch = onTab; this.currentUser = currentUser; this.settingsService = settingsService; this.categoryService = categoryService; this.tagService = tagService;
        int d = ps.get_default_duration();
        timeRemaining = d * 60; totalTime = timeRemaining;

        setPrefSize(660, 660);
        setStyle("-fx-background-color: transparent;");

        canvas = new Canvas(ViewCoordinator.SAFE_W * 0.65, ViewCoordinator.SAFE_W * 0.65);

        statusLabel = new Label("select task");
        statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold;");
        ViewCoordinator.applyFont(statusLabel, ViewCoordinator.FONT_DETAIL);

        ovSessions = new Label("0"); ovSessions.getStyleClass().add("stat-value");
        ovFocus = new Label("0 min"); ovFocus.getStyleClass().add("stat-value");
        ovTasks = new Label("0"); ovTasks.getStyleClass().add("stat-value");
        ovToday = new Label("0"); ovToday.getStyleClass().add("stat-value");

        taskBox = new ComboBox<>();
        taskBox.setPrefWidth(160);
        taskBox.setVisibleRowCount(5);
        taskBox.getStyleClass().add("combo-box");
        taskBox.setOnAction(e -> on_select());

        int spinMin = (currentUser == null) ? 25 : 1;
        spinner = new Spinner<>(spinMin, 180, Math.max(ps.get_default_duration(), spinMin));
        spinner.setPrefWidth(56);
        spinner.getStyleClass().add("spinner");
        Label minLbl = new Label("min");
        minLbl.setStyle("-fx-text-fill: #999999; -fx-font-weight: bold;");
        ViewCoordinator.applyFont(minLbl, ViewCoordinator.FONT_SMALL);

        Button setBtn = new Button("Set");
        setBtn.getStyleClass().addAll("btn-outline");
        setBtn.setOnAction(e -> set_dur());

        btnStart = new Button("Start");
        btnStart.getStyleClass().addAll("pill-btn", "pill-btn-green");
        btnStart.setOnAction(e -> start());
        btnStart.setMinHeight(22);

        btnPause = new Button("Pause");
        btnPause.getStyleClass().addAll("pill-btn", "pill-btn-grey");
        btnPause.setOnAction(e -> pause());
        btnPause.setDisable(true);
        btnPause.setMinHeight(22);

        btnReset = new Button("Reset");
        btnReset.getStyleClass().addAll("pill-btn", "pill-btn-red");
        btnReset.setOnAction(e -> reset());
        btnReset.setDisable(true);
        btnReset.setMinHeight(22);

        HBox innerSpinner = new HBox(6);
        innerSpinner.setAlignment(Pos.CENTER);
        innerSpinner.getChildren().addAll(spinner, minLbl, setBtn);

        HBox innerButtons = new HBox(8);
        innerButtons.setAlignment(Pos.CENTER);
        innerButtons.getChildren().addAll(btnStart, btnPause, btnReset);

        compTasksVal = new Label("0"); compTasksVal.getStyleClass().add("comp-value");
        compSessionsVal = new Label("0"); compSessionsVal.getStyleClass().add("comp-value");
        compFocusVal = new Label("0"); compFocusVal.getStyleClass().add("comp-value");
        compTodayVal = new Label("0"); compTodayVal.getStyleClass().add("comp-value");

        compTasks = buildComplication(compTasksVal, "tasks", 1);
        compSessions = buildComplication(compSessionsVal, "sessions", 4);
        compFocus = buildComplication(compFocusVal, "focus", 3);
        compToday = buildComplication(compTodayVal, "today", 0);

        swipe = new SwipeContainer(
            buildOverview(),
            buildContent(taskBox, innerSpinner, innerButtons)
        );
        getChildren().add(swipe);

        refresh_sessions();
        glow_start();
    }

    private StackPane buildComplication(Label val, String label, int tabIdx) {
        Circle circle = new Circle(ViewCoordinator.SAFE_W * 0.076);
        circle.getStyleClass().add("comp-circle");

        Label lbl = new Label(label);
        lbl.getStyleClass().add("comp-label");

        VBox vb = new VBox(-1);
        vb.setAlignment(Pos.CENTER);
        vb.getChildren().addAll(val, lbl);

        StackPane sp = new StackPane();
        sp.getChildren().addAll(circle, vb);
        sp.setOnMouseClicked(e -> { if (onTabSwitch != null) onTabSwitch.accept(tabIdx); });
        return sp;
    }

    private Node buildOverview() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(660, 660);
        box.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.setPadding(new Insets(0, 10, 0, 10));

        Label title = new Label("Timer");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #aaaaaa; -fx-padding: 16 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_PAGE_TITLE);

        HBox row = new HBox(ViewCoordinator.CARD_GAP);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(ViewCoordinator.CONTENT_W);
        row.getChildren().addAll(
            ViewCoordinator.statCard(ovToday, "Today", 4),
            ViewCoordinator.statCard(ovSessions, "Total", 4),
            ViewCoordinator.statCard(ovFocus, "Focus", 4),
            ViewCoordinator.statCard(ovTasks, "Tasks", 4));
        box.getChildren().addAll(title, row);
        return box;
    }

    private StackPane warnOverlay;
    private Label warnLabel;

    private Node buildContent(ComboBox<String> taskBox, HBox innerSpinner, HBox innerButtons) {
        VBox body = new VBox(10);
        body.setAlignment(Pos.CENTER);
        body.setPrefSize(660, 660);
        body.setPadding(new Insets(4, 40, 4, 40));

        double timerSize = ViewCoordinator.SAFE_W * 0.65;
        double circleR = ViewCoordinator.SAFE_W * 0.076;
        double off = timerSize / 2 + circleR + 8;
        // Keep complications inside the middle 60% band: the farthest point of a
        // complication is at distance sqrt(2)*off + circleR from center; cap it.
        double maxOff = (ViewCoordinator.SAFE_RADIUS - circleR) / Math.sqrt(2);
        if (off > maxOff) off = maxOff;
        compTasks.setTranslateX(-off);    compTasks.setTranslateY(-off);
        compSessions.setTranslateX(off);  compSessions.setTranslateY(-off);
        compFocus.setTranslateX(-off);    compFocus.setTranslateY(off);
        compToday.setTranslateX(off);     compToday.setTranslateY(off);

        StackPane canvasWrap = new StackPane(canvas);
        canvasWrap.setAlignment(Pos.CENTER);

        StackPane timerGroup = new StackPane();
        timerGroup.setAlignment(Pos.CENTER);
        timerGroup.getChildren().addAll(compTasks, compSessions, compFocus, compToday, canvasWrap);
        // The lower complications are translated downward by `off` beyond the
        // canvas, but the StackPane only sizes to the canvas. Reserve that extra
        // height so the dropdown/controls below don't overlap the circles.
        double lowerOverhang = off + circleR - timerSize / 2;
        double groupHeight = timerSize + 2 * Math.max(0, lowerOverhang);
        timerGroup.setMinHeight(groupHeight);
        timerGroup.setPrefHeight(groupHeight);

        taskBox.setMaxWidth(200);
        // Push the controls clear of the lower complications.
        VBox.setMargin(taskBox, new Insets(Math.max(0, lowerOverhang) + 6, 0, 0, 0));
        // Extra breathing room so dropdown / minutes / buttons read as 3 groups.
        VBox.setMargin(innerButtons, new Insets(4, 0, 0, 0));

        body.getChildren().addAll(timerGroup, taskBox, innerSpinner, innerButtons);

        // --- Warn overlay ---
        warnLabel = new Label();
        warnLabel.setStyle("-fx-text-fill: #cc6666; -fx-font-weight: bold; -fx-background-color: rgba(40,10,10,0.9); -fx-padding: 16 24; -fx-background-radius: 6;");
        ViewCoordinator.applyFont(warnLabel, ViewCoordinator.FONT_DETAIL);
        warnOverlay = new StackPane(warnLabel);
        warnOverlay.setStyle("-fx-background-color: rgba(20,20,20,0.7);");
        warnOverlay.setVisible(false);
        warnOverlay.setOnMouseClicked(e -> warnOverlay.setVisible(false));

        StackPane root = new StackPane(body, warnOverlay);
        return root;
    }

    public void refresh() {
        suppressSelect = true;
        String prev = taskBox.getValue();
        taskBox.setItems(FXCollections.observableArrayList("-- select --"));
        for (Task t : ts.get_all_tasks()) {
            if (t.get_status() != Status.COMPLETED && t.get_status() != Status.CANCELLED) {
                String title = t.get_title();
                int maxLen = ViewCoordinator.maxChars(ViewCoordinator.FONT_BODY, 140);
                if (title.length() > maxLen) title = title.substring(0, maxLen - 1) + "\u2026";
                taskBox.getItems().add(t.get_id() + " - " + title);
            }
        }
        if (prev != null && !prev.equals("-- select --")) {
            for (String item : taskBox.getItems()) { if (item.equals(prev)) { taskBox.setValue(prev); break; } }
        }
        suppressSelect = false;
        refresh_sessions();
        update_overview();
        update_complications();
    }

    private void refresh_sessions() {
        long active = ts.get_all_tasks().stream().filter(t ->
            t.get_status() == Status.PENDING || t.get_status() == Status.IN_PROGRESS
        ).count();
        compTasksVal.setText(String.valueOf(active));
        compSessionsVal.setText(String.valueOf(ps.get_total_sessions()));
        compFocusVal.setText(formatFocusTime(ps.get_total_focus_time_minutes()));
        int today = 0;
        java.time.LocalDate now = java.time.LocalDate.now();
        for (Pomodoro p : ps.get_all_pomodoros()) {
            for (java.time.LocalDateTime dt : p.get_session_history()) {
                if (dt.toLocalDate().equals(now)) today++;
            }
        }
        compTodayVal.setText(String.valueOf(today));
        ViewCoordinator.fitText(compTasksVal, compTasksVal.getText(), ViewCoordinator.SAFE_W * 0.076 * 2, ViewCoordinator.FONT_STAT_VALUE, 2);
        ViewCoordinator.fitText(compSessionsVal, compSessionsVal.getText(), ViewCoordinator.SAFE_W * 0.076 * 2, ViewCoordinator.FONT_STAT_VALUE, 2);
        ViewCoordinator.fitText(compFocusVal, compFocusVal.getText(), ViewCoordinator.SAFE_W * 0.076 * 2, ViewCoordinator.FONT_STAT_VALUE, 2);
        ViewCoordinator.fitText(compTodayVal, compTodayVal.getText(), ViewCoordinator.SAFE_W * 0.076 * 2, ViewCoordinator.FONT_STAT_VALUE, 2);
    }

    private void update_complications() {
        long active = ts.get_all_tasks().stream().filter(t ->
            t.get_status() == Status.PENDING || t.get_status() == Status.IN_PROGRESS
        ).count();
        compTasksVal.setText(String.valueOf(active));
        compSessionsVal.setText(String.valueOf(ps.get_total_sessions()));
        compFocusVal.setText(formatFocusTime(ps.get_total_focus_time_minutes()));
        int today = 0;
        java.time.LocalDate now = java.time.LocalDate.now();
        for (Pomodoro p : ps.get_all_pomodoros()) {
            for (java.time.LocalDateTime dt : p.get_session_history()) {
                if (dt.toLocalDate().equals(now)) today++;
            }
        }
        compTodayVal.setText(String.valueOf(today));
        ViewCoordinator.fitText(compTasksVal, compTasksVal.getText(), ViewCoordinator.SAFE_W * 0.076 * 2, ViewCoordinator.FONT_STAT_VALUE, 2);
        ViewCoordinator.fitText(compSessionsVal, compSessionsVal.getText(), ViewCoordinator.SAFE_W * 0.076 * 2, ViewCoordinator.FONT_STAT_VALUE, 2);
        ViewCoordinator.fitText(compFocusVal, compFocusVal.getText(), ViewCoordinator.SAFE_W * 0.076 * 2, ViewCoordinator.FONT_STAT_VALUE, 2);
        ViewCoordinator.fitText(compTodayVal, compTodayVal.getText(), ViewCoordinator.SAFE_W * 0.076 * 2, ViewCoordinator.FONT_STAT_VALUE, 2);
    }

    static String formatFocusTime(int totalMinutes) {
        if (totalMinutes < 60) return totalMinutes + "m";
        int totalHours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (totalHours < 24) return mins > 0 ? totalHours + "h" + mins + "m" : totalHours + "h";
        int days = totalHours / 24;
        int hours = totalHours % 24;
        if (days < 7) return hours > 0 ? days + "d" + hours + "h" : days + "d";
        int weeks = days / 7;
        int remainDays = days % 7;
        if (weeks < 4) return remainDays > 0 ? weeks + "w" + remainDays + "d" : weeks + "w";
        int months = days / 30;
        int remainWeeks = (days % 30) / 7;
        if (months < 12) return remainWeeks > 0 ? months + "mo" + remainWeeks + "w" : months + "mo";
        int years = days / 365;
        int remainMonths = (days % 365) / 30;
        return remainMonths > 0 ? years + "y" + remainMonths + "mo" : years + "y";
    }

    private void update_overview() {
        ovSessions.setText(String.valueOf(ps.get_total_sessions()));
        ovFocus.setText(formatFocusTime(ps.get_total_focus_time_minutes()));
        ovTasks.setText(String.valueOf(ts.count_tasks()));
        int today = 0;
        java.time.LocalDate now = java.time.LocalDate.now();
        for (Pomodoro p : ps.get_all_pomodoros()) {
            for (java.time.LocalDateTime dt : p.get_session_history()) {
                if (dt.toLocalDate().equals(now)) today++;
            }
        }
        ovToday.setText(String.valueOf(today));
        ViewCoordinator.fitStatValue(ovSessions, ovSessions.getText(), 4);
        ViewCoordinator.fitStatValue(ovFocus, ovFocus.getText(), 4);
        ViewCoordinator.fitStatValue(ovTasks, ovTasks.getText(), 4);
        ViewCoordinator.fitStatValue(ovToday, ovToday.getText(), 4);
    }

    private void on_select() {
        if (suppressSelect) return;
        if (isRunning) return;
        String sel = taskBox.getValue();
        if (sel == null || sel.equals("-- select --")) return;
        int id = Integer.parseInt(sel.split(" - ")[0]);
        ts.try_get_task(id).ifPresent(t -> {
            Optional<Pomodoro> ex = find(id);
            boolean existed = ex.isPresent();
            current = ex.orElseGet(() -> ps.create_pomodoro(id, t.get_title()));
            statusLabel.setText("ready");
            statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold;");
            if (existed && current.get_completed_sessions() > 0) {
                // Existing pomodoro with history: preserve its stored duration
                spinner.getValueFactory().setValue(current.get_duration_minutes());
                timeRemaining = current.get_duration_minutes() * 60;
                totalTime = timeRemaining;
            } else {
                // New or fresh pomodoro: apply spinner value
                current.set_duration_minutes(spinner.getValue());
                timeRemaining = spinner.getValue() * 60;
                totalTime = timeRemaining;
            }
            draw();
        });
    }

    private Optional<Pomodoro> find(int id) {
        for (Pomodoro p : ps.get_all_pomodoros()) if (p.get_task_id() == id) return Optional.of(p);
        return Optional.empty();
    }

    private void set_dur() {
        int d = spinner.getValue();
        if (current != null) {
            current.set_duration_minutes(d);
            if (!isRunning) {
                timeRemaining = d * 60; totalTime = timeRemaining;
                draw();
            }
        }
    }

    private void start() {
        if (current == null) { warn("select a task"); return; }
        timeRemaining = current.get_duration_minutes() * 60;
        totalTime = timeRemaining;
        runningReachedZero = false;
        if (isPaused) {
            isPaused = false;
            endTimeMillis = System.currentTimeMillis() + (long) timeRemaining * 1000L;
        } else {
            isRunning = true;
            endTimeMillis = System.currentTimeMillis() + (long) timeRemaining * 1000L;
        }
        statusLabel.setText("focus");
        statusLabel.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: 900;");
        btnStart.setDisable(true); btnPause.setDisable(false); btnReset.setDisable(false);
        taskBox.setDisable(true); spinner.setDisable(true);
        start_countdown();
    }

    private void start_countdown() {
        if (countdown != null) countdown.stop();
        countdown = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            long now = System.currentTimeMillis();
            timeRemaining = (int) Math.max(0, (endTimeMillis - now) / 1000L);
            if (timeRemaining <= 0 && isRunning && !runningReachedZero) {
                runningReachedZero = true;
                complete();
                return;
            }
            draw();
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void pause() {
        timeRemaining = (int) Math.max(0, (endTimeMillis - System.currentTimeMillis()) / 1000L);
        if (countdown != null) countdown.stop();
        isPaused = true;
        statusLabel.setText("paused");
        statusLabel.setStyle("-fx-text-fill: #999999; -fx-font-weight: bold;");
        btnStart.setDisable(false); btnStart.setText("Resume");
        btnPause.setDisable(true);
        draw();
    }

    private void reset() {
        if (countdown != null) countdown.stop();
        isRunning = false; isPaused = false; runningReachedZero = false;
        int d = current != null ? current.get_duration_minutes() : ps.get_default_duration();
        timeRemaining = d * 60; totalTime = timeRemaining;
        statusLabel.setText("ready");
        statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold;");
        btnStart.setDisable(false); btnStart.setText("Start");
        btnPause.setDisable(true); btnReset.setDisable(true);
        taskBox.setDisable(false); spinner.setDisable(false);
        draw();
    }

    private void complete() {
        if (countdown != null) countdown.stop();
        timeRemaining = 0;
        isRunning = false; isPaused = false; runningReachedZero = false;
        AudioPlayer.playNotification();
        if (current != null) {
            boolean storeSession = ts.try_get_task(current.get_task_id())
                .map(t -> t.is_store_sessions())
                .orElse(true);
            if (storeSession) {
                ps.increment_session(current.get_id());
            }
            if (currentUser != null && storeSession) {
                storage.save_user_data(currentUser.get_id(), ts, ps, settingsService, categoryService, tagService);
            }
        }
        refresh_sessions();
        update_complications();
        update_overview();
        statusLabel.setText("done");
        statusLabel.setStyle("-fx-text-fill: #4caf7a; -fx-font-weight: 900;");
        btnStart.setDisable(false); btnStart.setText("Start");
        btnPause.setDisable(true); btnReset.setDisable(true);
        taskBox.setDisable(false); spinner.setDisable(false);
        draw();
        showCompletionNotification();
    }

    private void showCompletionNotification() {
        if (getScene() == null || getScene().getWindow() == null) return;
        if (completionStage != null && completionStage.isShowing()) {
            completionStage.close();
        }
        completionStage = new javafx.stage.Stage(javafx.stage.StageStyle.TRANSPARENT);
        completionStage.initOwner(getScene().getWindow());
        completionStage.setAlwaysOnTop(true);
        completionStage.setResizable(false);

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #4caf7a; -fx-border-width: 2; -fx-border-radius: 10;");

        Label msg = new Label("Session Complete!");
        msg.setStyle("-fx-text-fill: #4caf7a; -fx-font-weight: 900;");
        ViewCoordinator.applyFont(msg, ViewCoordinator.FONT_DETAIL_TITLE);
        msg.setAlignment(Pos.CENTER);
        msg.setTextAlignment(TextAlignment.CENTER);
        Label sub = new Label("Take a break or continue.");
        sub.setStyle("-fx-text-fill: #cccccc;");
        ViewCoordinator.applyFont(sub, ViewCoordinator.FONT_DETAIL);
        sub.setAlignment(Pos.CENTER);
        sub.setTextAlignment(TextAlignment.CENTER);

        ComboBox<String> taskBox = new ComboBox<>();
        taskBox.setPromptText("Select next task");
        taskBox.setMaxWidth(240);
        taskBox.getStyleClass().add("combo-box");
        java.util.List<Task> activeTasks = ts.get_all_tasks().stream()
            .filter(t -> t.get_status() != Status.COMPLETED && t.get_status() != Status.CANCELLED)
            .collect(java.util.stream.Collectors.toList());
        if (activeTasks.isEmpty()) {
            taskBox.getItems().add("-- no active tasks --");
            taskBox.setValue("-- no active tasks --");
            taskBox.setDisable(true);
        } else {
            for (Task t : activeTasks) {
                taskBox.getItems().add(t.get_id() + " - " + t.get_title());
            }
            if (current != null) {
                String cur = current.get_task_id() + " - " + current.get_task_name();
                if (taskBox.getItems().contains(cur)) {
                    taskBox.setValue(cur);
                } else {
                    taskBox.getSelectionModel().selectFirst();
                }
            } else {
                taskBox.getSelectionModel().selectFirst();
            }
        }

        int notifMin = (currentUser == null) ? 25 : 1;
        Spinner<Integer> durSpinner = new Spinner<>(notifMin, 180, current != null ? Math.max(current.get_duration_minutes(), notifMin) : Math.max(ps.get_default_duration(), notifMin));
        durSpinner.setPrefWidth(80);
        durSpinner.getStyleClass().add("spinner");
        Label minLbl = new Label("min");
        minLbl.setStyle("-fx-text-fill: #999999; -fx-font-weight: bold;");
        ViewCoordinator.applyFont(minLbl, ViewCoordinator.FONT_SMALL);
        HBox timeRow = new HBox(6);
        timeRow.setAlignment(Pos.CENTER);
        timeRow.getChildren().addAll(durSpinner, minLbl);

        Button btnStartNext = new Button("Start Next Session");
        btnStartNext.getStyleClass().addAll("btn", "btn-nav-green");
        btnStartNext.setPrefWidth(180);
        Button btnBack = new Button("Back to Timer");
        btnBack.getStyleClass().addAll("btn", "btn-nav-grey");
        btnBack.setPrefWidth(140);
        Button btnDismiss = new Button("Dismiss");
        btnDismiss.getStyleClass().addAll("btn", "btn-outline");
        btnDismiss.setPrefWidth(100);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.getChildren().addAll(btnDismiss, btnBack, btnStartNext);

        btnStartNext.setOnAction(e -> {
            String sel = taskBox.getValue();
            if (sel == null || sel.startsWith("--")) {
                warn("select a valid task");
                return;
            }
            int taskId = Integer.parseInt(sel.split(" - ")[0]);
            if (!ts.try_get_task(taskId).isPresent()) {
                warn("task not found");
                return;
            }
            int dur = durSpinner.getValue();
            current = ps.create_pomodoro(taskId, ts.try_get_task(taskId).get().get_title(), dur);
            PomodoroView.this.taskBox.setValue(sel);
            PomodoroView.this.spinner.getValueFactory().setValue(dur);
            completionStage.close();
            if (onTabSwitch != null) onTabSwitch.accept(0);
            start();
        });
        btnBack.setOnAction(e -> {
            completionStage.close();
            stop_timers(); isRunning = false; isPaused = false;
            btnStart.setDisable(false); btnStart.setText("Start");
            btnPause.setDisable(true); btnReset.setDisable(true);
            taskBox.setDisable(false); spinner.setDisable(false);
            if (current != null) {
                timeRemaining = current.get_duration_minutes() * 60;
                totalTime = timeRemaining;
                statusLabel.setText("ready");
                statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold;");
            } else {
                current = null;
                statusLabel.setText("select task");
                statusLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-weight: bold;");
                int defDur = ps.get_default_duration();
                timeRemaining = defDur * 60; totalTime = defDur * 60;
            }
            draw();
            if (onTabSwitch != null) onTabSwitch.accept(0);
        });
        btnDismiss.setOnAction(e -> completionStage.close());

        content.getChildren().addAll(msg, sub, taskBox, timeRow, btnRow);
        javafx.scene.Scene scene = new javafx.scene.Scene(content);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        completionStage.setScene(scene);
        completionStage.centerOnScreen();
        completionStage.show();
    }

    private void closeCompletionNotification() {
        if (completionStage != null && completionStage.isShowing()) {
            completionStage.close();
        }
    }

    private void stop_timers() { if (countdown != null) countdown.stop(); }

    private void glow_start() {
        glow = new AnimationTimer() {
            @Override public void handle(long now) {
                pulse = (float)(Math.sin(now / 1_000_000_000.0) * 0.04 + 0.96);
                if (isRunning && !isPaused) {
                    long remainingMs = Math.max(0, endTimeMillis - System.currentTimeMillis());
                    timeRemaining = (int)(remainingMs / 1000);
                    if (timeRemaining <= 0 && !runningReachedZero) {
                        runningReachedZero = true;
                        complete();
                        return;
                    }
                }
                draw();
            }
        };
        glow.start();
    }

    public void showContentPage() {
        refresh();
        swipe.showContentImmediate();
        if (isRunning && !isPaused) {
            long remainingMs = Math.max(0, endTimeMillis - System.currentTimeMillis());
            timeRemaining = (int)(remainingMs / 1000);
            if (timeRemaining <= 0) {
                complete();
                return;
            }
            draw();
            start_countdown();
            swipe.showContentImmediate();
        } else if (current != null) {
            draw();
            swipe.showContentImmediate();
        } else {
            swipe.showOverviewImmediate();
        }
    }
    public void on_hide() {
        if (countdown != null) countdown.stop();
    }
    public void cleanup() { if (countdown != null) countdown.stop(); if (glow != null) glow.stop(); closeCompletionNotification(); }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth(), h = canvas.getHeight();
        g.clearRect(0, 0, w, h);

        double cx = w / 2, cy = h / 2;
        double r = Math.min(w, h) / 2 - 20;
        double f = totalTime > 0 ? (double) timeRemaining / totalTime : 0;
        float p = pulse;

        // Track ring
        g.setStroke(TRACK);
        g.setLineWidth(3);
        g.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // Tick marks
        double tr = r + 2;
        for (int i = 0; i < 60; i++) {
            double a = Math.toRadians(i * 6 - 90);
            boolean major = i % 5 == 0;
            double len = major ? 10 : 5;
            double x1 = cx + (tr - 2) * Math.cos(a);
            double y1 = cy + (tr - 2) * Math.sin(a);
            double x2 = cx + (tr - 2 + len) * Math.cos(a);
            double y2 = cy + (tr - 2 + len) * Math.sin(a);
            g.setLineWidth(major ? 2 : 1);
            g.setStroke(major ? Color.rgb(200, 200, 200) : Color.rgb(100, 100, 100));
            g.strokeLine(x1, y1, x2, y2);
        }

        // Hour numbers
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        g.setFill(Color.rgb(150, 150, 150));
        double nr = r - 6;
        for (int i = 1; i <= 12; i++) {
            double a = Math.toRadians(i * 30 - 90);
            double nx = cx + nr * Math.cos(a);
            double ny = cy + nr * Math.sin(a);
            g.fillText(String.valueOf(i), nx, ny);
        }

        // Glow layers
        for (int i = 4; i >= 0; i--) {
            g.setLineWidth(3 + i * 2);
            g.setStroke(new Color(0.7, 0.7, 0.7, (0.04 + i * 0.025) * p));
            if (f > 0.005) g.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -(int)(f * 360), javafx.scene.shape.ArcType.OPEN);
        }

        // Main arc
        if (f > 0.005) {
            g.setLineWidth(3);
            g.setStroke(new Color(0.7, 0.7, 0.7, 0.85 * p));
            g.strokeArc(cx - r, cy - r, r * 2, r * 2, 90, -(int)(f * 360), javafx.scene.shape.ArcType.OPEN);
        }

        // Digital time
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(javafx.geometry.VPos.CENTER);
        int fs = Math.max(24, Math.min(64, (int)(r / 3.8)));
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, fs));
        String timeStr = String.format("%02d:%02d", timeRemaining / 60, timeRemaining % 60);
        g.setFill(new Color(0, 0, 0, 0.3));
        g.fillText(timeStr, cx + 2, cy + 2);
        g.setFill(isPaused ? Color.rgb(150, 150, 150) : new Color(1, 1, 1, 0.9 * p));
        g.fillText(timeStr, cx, cy);

        // Status label below time
        String status = statusLabel.getText();
        if (status != null && !status.isEmpty()) {
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(10, (int)(r / 12))));
            g.setFill(Color.rgb(150, 150, 150));
            g.fillText(status, cx, cy + fs * 0.6);
        }

        if (isPaused && r >= 60) {
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, Math.max(9, (int)(r / 14))));
            g.setFill(Color.rgb(150, 150, 150));
            g.fillText("paused", cx, cy + fs * 0.6 + 16);
        }
    }

    private void warn(String m) {
        warnLabel.setText(m);
        warnOverlay.setVisible(true);
    }
}
