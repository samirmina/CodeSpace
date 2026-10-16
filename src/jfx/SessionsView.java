package jfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import models.Pomodoro;
import models.Task;
import models.User;
import services.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.util.Pair;

public class SessionsView extends StackPane {
    private final PomodoroService pomodoroService;
    private final TaskService taskService;
    private final UserService userService;
    private final JsonStorage storage;
    private final SwipeContainer swipe;
    private final User currentUser;
    private final SettingsService settingsService;

    private final Label ovSessions, ovFocus, ovRange, ovMost, ovRecent, ovInsight;
    private final Label ctTask, ctSessions, ctDur, ctHist, ctDetail;
    private final Label ctNavName;
    private final Button ctPrev, ctNext, btnDetails;
    private List<Pomodoro> pomList = new java.util.ArrayList<>();
    private int currentIdx;
    private int currentTaskId = -1;

    private final CategoryService categoryService;
    private final TagService tagService;

    private final StackPane centerPane;
    private final VBox detailBox, durBox, resetBox, deleteBox, detailSessionBox;
    private final Spinner<Integer> spDur;
    private final Label durTask, resetConfirm, delConfirm;
    private final javafx.scene.canvas.Canvas recentCanvas, mostProductiveCanvas;

    public SessionsView(PomodoroService ps, TaskService ts, UserService us, JsonStorage s, User currentUser, SettingsService settingsService, CategoryService categoryService, TagService tagService) {
        this.pomodoroService = ps; this.taskService = ts; this.userService = us; this.storage = s; this.currentUser = currentUser; this.settingsService = settingsService; this.categoryService = categoryService; this.tagService = tagService;
        setPrefSize(660, 660);

        ovSessions = new Label("0"); ovSessions.getStyleClass().add("stat-value");
        ovFocus = new Label("0 m"); ovFocus.getStyleClass().add("stat-value");
        ovRange = new Label("\u2014"); ovRange.getStyleClass().add("stat-value");
        ovMost = new Label(); ovMost.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc; -fx-padding: 2 0;");
        ovRecent = new Label(); ovRecent.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc; -fx-padding: 2 0;");
        ovInsight = new Label(); ovInsight.setStyle("-fx-font-weight: bold; -fx-text-fill: #888888; -fx-padding: 6 0; -fx-font-size: 11px;");
        ovInsight.setMaxWidth(ViewCoordinator.CONTENT_W);
        ovInsight.setAlignment(Pos.CENTER);
        ovInsight.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        recentCanvas = new javafx.scene.canvas.Canvas(ViewCoordinator.W, ViewCoordinator.H);
        mostProductiveCanvas = new javafx.scene.canvas.Canvas(ViewCoordinator.W, ViewCoordinator.H);

        ctTask = new Label(); ctTask.setStyle("-fx-font-weight: 900; -fx-text-fill: #ffffff; -fx-padding: 0 0 4 0;");
        ctTask.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctSessions = new Label(); ctSessions.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctSessions.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctDur = new Label(); ctDur.setStyle("-fx-font-weight: bold; -fx-text-fill: #cccccc;");
        ctDur.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctHist = new Label(); ctHist.setStyle("-fx-text-fill: #aaaaaa; -fx-padding: 2 0;");
        ctHist.setWrapText(true);
        ctHist.setAlignment(Pos.CENTER);
        ctHist.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        ctHist.setMaxWidth(ViewCoordinator.CONTENT_W);
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
        ctNext.setOnAction(e -> { if (currentIdx < pomList.size() - 1) { currentIdx++; showDetail(); } });

        Button btnSet = new Button("Set Duration");
        btnSet.getStyleClass().addAll("btn", "btn-nav");
        Button btnReset = new Button("Reset Sessions");
        btnReset.getStyleClass().addAll("btn", "btn-nav-amber");
        Button btnDelete = new Button("\u2715 Delete");
        btnDelete.getStyleClass().addAll("btn", "btn-nav-red");

        // --- Inline edit form fields ---
        spDur = new Spinner<>(1, 180, 25);
        spDur.setPrefWidth(100);

        // --- Detail mode ---
        detailBox = new VBox(6);
        detailBox.setAlignment(Pos.CENTER);
        Label dtlTitle = new Label("SESSION DETAILS");
        dtlTitle.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 8 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(dtlTitle, ViewCoordinator.FONT_SECTION);
        VBox detail = new VBox(4); detail.setAlignment(Pos.CENTER);
        detail.getChildren().addAll(ctTask, ctSessions, ctDur, ctHist);
        btnDetails = new Button("Show Details");
        btnDetails.getStyleClass().addAll("btn", "btn-nav");
        btnDetails.setMaxWidth(140);
        HBox actRow = new HBox(8);
        actRow.setAlignment(Pos.CENTER);
        actRow.getChildren().addAll(btnDetails, btnSet, btnReset, btnDelete);
        btnSet.setOnAction(e -> showDurForm());
        btnReset.setOnAction(e -> showResetForm());
        btnDelete.setOnAction(e -> showDeleteForm());
        btnDetails.setOnAction(e -> showSessionDetails());
        Region topSpacer = new Region();
        Region bottomSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);
        detailBox.getChildren().addAll(topSpacer, dtlTitle, detail, actRow, bottomSpacer);

        // --- Duration mode ---
        durBox = new VBox(8);
        durBox.setAlignment(Pos.CENTER);
        Label durLbl = new Label("SET DURATION");
        durLbl.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(durLbl, ViewCoordinator.FONT_SECTION);
        durTask = new Label();
        ViewCoordinator.applyFont(durTask, ViewCoordinator.FONT_DETAIL);
        durTask.setStyle("-fx-text-fill: #cccccc;");
        VBox durFields = new VBox(6);
        durFields.setAlignment(Pos.CENTER);
        durFields.getChildren().addAll(new Label("Minutes:"), spDur);
        Button btnDurOk = new Button("OK");
        btnDurOk.getStyleClass().addAll("btn", "btn-nav-green");
        Button btnDurCancel = new Button("Cancel");
        btnDurCancel.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox durBtns = new HBox(10); durBtns.setAlignment(Pos.CENTER);
        durBtns.getChildren().addAll(btnDurCancel, btnDurOk);
        Region durTop = new Region();
        Region durBottom = new Region();
        VBox.setVgrow(durTop, Priority.ALWAYS);
        VBox.setVgrow(durBottom, Priority.ALWAYS);
        durBox.getChildren().addAll(durTop, durLbl, durTask, durFields, durBtns, durBottom);
        btnDurOk.setOnAction(e -> {
            if (pomList.isEmpty()) return;
            pomList.get(currentIdx).set_duration_minutes(spDur.getValue());
            if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
            refresh();
            showDetail();
        });
        btnDurCancel.setOnAction(e -> showDetail());

        // --- Reset mode ---
        resetConfirm = new Label();
        ViewCoordinator.applyFont(resetConfirm, ViewCoordinator.FONT_DETAIL);
        resetConfirm.setStyle("-fx-text-fill: #cccccc;");
        resetBox = new VBox(8);
        resetBox.setAlignment(Pos.CENTER);
        Label resetLbl = new Label("RESET SESSIONS?");
        resetLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #cc8820; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(resetLbl, ViewCoordinator.FONT_SECTION);
        Button btnResetYes = new Button("Yes");
        btnResetYes.getStyleClass().addAll("btn", "btn-nav-amber");
        Button btnResetNo = new Button("No");
        btnResetNo.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox resetBtns = new HBox(10); resetBtns.setAlignment(Pos.CENTER);
        resetBtns.getChildren().addAll(btnResetNo, btnResetYes);
        Region resetTop = new Region();
        Region resetBottom = new Region();
        VBox.setVgrow(resetTop, Priority.ALWAYS);
        VBox.setVgrow(resetBottom, Priority.ALWAYS);
        resetBox.getChildren().addAll(resetTop, resetLbl, resetConfirm, resetBtns, resetBottom);
        btnResetYes.setOnAction(e -> {
            if (pomList.isEmpty()) return;
            pomodoroService.reset_sessions(pomList.get(currentIdx).get_id());
            if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
            refresh();
            showDetail();
        });
        btnResetNo.setOnAction(e -> showDetail());

        // --- Delete mode ---
        delConfirm = new Label();
        ViewCoordinator.applyFont(delConfirm, ViewCoordinator.FONT_DETAIL);
        delConfirm.setStyle("-fx-text-fill: #cccccc;");
        deleteBox = new VBox(8);
        deleteBox.setAlignment(Pos.CENTER);
        Label delLbl = new Label("DELETE SESSION?");
        delLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 900; -fx-text-fill: #cc4444; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(delLbl, ViewCoordinator.FONT_SECTION);
        Button btnDelYes = new Button("Yes");
        btnDelYes.getStyleClass().addAll("btn", "btn-nav-red");
        Button btnDelNo = new Button("No");
        btnDelNo.getStyleClass().addAll("btn", "btn-nav-grey");
        HBox delBtns = new HBox(10); delBtns.setAlignment(Pos.CENTER);
        delBtns.getChildren().addAll(btnDelNo, btnDelYes);
        Region delTop = new Region();
        Region delBottom = new Region();
        VBox.setVgrow(delTop, Priority.ALWAYS);
        VBox.setVgrow(delBottom, Priority.ALWAYS);
        deleteBox.getChildren().addAll(delTop, delLbl, delConfirm, delBtns, delBottom);
        btnDelYes.setOnAction(e -> {
            if (pomList.isEmpty()) return;
            pomodoroService.delete_pomodoro(pomList.get(currentIdx).get_id());
            if (currentUser != null) storage.save_user_data(currentUser.get_id(), taskService, pomodoroService, settingsService, categoryService, tagService);
            refresh();
            showDetail();
        });
        btnDelNo.setOnAction(e -> showDetail());

        // --- Detail session list mode ---
        detailSessionBox = new VBox(8);
        detailSessionBox.setAlignment(Pos.CENTER);
        Label dsTitle = new Label("SESSION HISTORY");
        dsTitle.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 8 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(dsTitle, ViewCoordinator.FONT_SECTION);
        ctDetail = new Label();
        ctDetail.setStyle("-fx-text-fill: #cccccc; -fx-font-weight: bold;");
        ctDetail.setMaxWidth(ViewCoordinator.CONTENT_W);
        ctDetail.setWrapText(true);
        ctDetail.setAlignment(Pos.CENTER);
        ctDetail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        ScrollPane spDetail = new ScrollPane(ctDetail);
        spDetail.setFitToWidth(true);
        spDetail.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        spDetail.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        spDetail.getStyleClass().add("transparent-scroll");
        VBox.setVgrow(spDetail, Priority.ALWAYS);
        Button btnBack = new Button("Back");
        btnBack.getStyleClass().addAll("btn", "btn-nav-grey");
        btnBack.setMaxWidth(100);
        btnBack.setOnAction(e -> showDetail());
        detailSessionBox.getChildren().addAll(dsTitle, spDetail, btnBack);

        // --- Center pane ---
        centerPane = new StackPane(detailBox, durBox, resetBox, deleteBox, detailSessionBox);
        centerPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        showDetail();

        HBox contentRow = new HBox(0);
        contentRow.setAlignment(Pos.CENTER);
        contentRow.setPrefSize(660, 660);
        contentRow.getChildren().addAll(ctPrev, centerPane, ctNext);
        HBox.setHgrow(centerPane, Priority.ALWAYS);

        swipe = new SwipeContainer(
            buildStatsOverview(),
            buildMostProductiveOverview(),
            buildRecentOverview(),
            contentRow
        );
        getChildren().add(swipe);

        ViewCoordinator.applyFont(ovMost, ViewCoordinator.FONT_BODY);
        ViewCoordinator.applyFont(ovRecent, ViewCoordinator.FONT_DETAIL);

        refresh();
    }

    private void showDetail() {
        detailBox.setVisible(true); detailBox.setManaged(true);
        durBox.setVisible(false); durBox.setManaged(false);
        resetBox.setVisible(false); resetBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        if (pomList.isEmpty()) {
            ctTask.setText("No sessions");
            ctSessions.setText(""); ctDur.setText(""); ctHist.setText("");
            ctNavName.setText("\u2014");
            ctPrev.setDisable(true); ctNext.setDisable(true);
            return;
        }
        showCurrent();
    }

    private void showDurForm() {
        if (pomList.isEmpty()) return;
        Pomodoro p = pomList.get(currentIdx);
        durTask.setText("Duration for: " + p.get_task_name());
        spDur.getValueFactory().setValue(p.get_duration_minutes());
        detailBox.setVisible(false); detailBox.setManaged(false);
        durBox.setVisible(true); durBox.setManaged(true);
        resetBox.setVisible(false); resetBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
    }

    private void showResetForm() {
        if (pomList.isEmpty()) return;
        resetConfirm.setText("Reset sessions for \"" + pomList.get(currentIdx).get_task_name() + "\"?");
        detailBox.setVisible(false); detailBox.setManaged(false);
        durBox.setVisible(false); durBox.setManaged(false);
        resetBox.setVisible(true); resetBox.setManaged(true);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
    }

    private void showDeleteForm() {
        if (pomList.isEmpty()) return;
        delConfirm.setText("Delete \"" + pomList.get(currentIdx).get_task_name() + "\"?");
        detailBox.setVisible(false); detailBox.setManaged(false);
        durBox.setVisible(false); durBox.setManaged(false);
        resetBox.setVisible(false); resetBox.setManaged(false);
        deleteBox.setVisible(true); deleteBox.setManaged(true);
    }

    private Node buildStatsOverview() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(660, 660);
        box.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.setPadding(new Insets(0, 10, 0, 10));
        Label title = new Label("SESSIONS");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #aaaaaa; -fx-padding: 16 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_PAGE_TITLE);
        HBox row = new HBox(ViewCoordinator.CARD_GAP);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(ViewCoordinator.CONTENT_W);
        VBox sessionsCard = ViewCoordinator.statCard(ovSessions, "Sessions", 3);
        VBox focusCard = ViewCoordinator.statCard(ovFocus, "Focus", 3);
        VBox rangeCard = ViewCoordinator.statCard(ovRange, "Range", 3);
        sessionsCard.setOnMouseClicked(e -> swipe.goTo(3));
        focusCard.setOnMouseClicked(e -> swipe.goTo(3));
        rangeCard.setOnMouseClicked(e -> swipe.goTo(3));
        row.getChildren().addAll(sessionsCard, focusCard, rangeCard);
        box.getChildren().addAll(title, row, ovInsight);
        return box;
    }

    private Node buildMostProductiveOverview() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(660, 660);
        box.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.setPadding(new Insets(0, 10, 0, 10));
        Label title = new Label("MOST PRODUCTIVE");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #999999; -fx-padding: 16 0 4 0; -fx-letter-spacing: 1px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_SECTION);
        StackPane sp = new StackPane(mostProductiveCanvas);
        sp.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.getChildren().addAll(title, sp);
        return box;
    }

    private Node buildRecentOverview() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(660, 660);
        box.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.setPadding(new Insets(0, 10, 0, 10));
        Label title = new Label("RECENT SESSIONS");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #999999; -fx-padding: 16 0 4 0; -fx-letter-spacing: 1px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_SECTION);
        StackPane sp = new StackPane(recentCanvas);
        sp.setMaxWidth(ViewCoordinator.CONTENT_W);
        box.getChildren().addAll(title, sp);
        return box;
    }

    public void showContentPage() { refresh(); swipe.restoreLastPage(); }
    public void refresh() {
        java.util.List<Pomodoro> all = pomodoroService.get_all_pomodoros();
        java.util.List<Pomodoro> visible = new java.util.ArrayList<>();
        for (Pomodoro p : all) {
            if (is_pomodoro_visible(p)) visible.add(p);
        }
        pomList = visible;
        currentIdx = Math.min(currentIdx, Math.max(0, pomList.size() - 1));
        showCurrent();
        updateOverview();
    }

    private boolean is_pomodoro_visible(Pomodoro p) {
        if (p == null) return false;
        java.util.Optional<Task> task = taskService.try_get_task(p.get_task_id());
        return task.isPresent() && task.get().is_store_sessions();
    }

    private void showCurrent() {
        if (pomList.isEmpty()) {
            ctTask.setText("No sessions");
            ViewCoordinator.fitDetailTitle(ctTask, "No sessions");
            ctSessions.setText(""); ctDur.setText(""); ctHist.setText("");
            ctNavName.setText("");
            ctPrev.setDisable(true); ctNext.setDisable(true);
            btnDetails.setDisable(true);
            return;
        }
        Pomodoro p = pomList.get(currentIdx);
        int taskId = p.get_task_id();
        currentTaskId = taskId;
        ctTask.setText(p.get_task_name());
        ViewCoordinator.fitDetailTitle(ctTask, p.get_task_name());
        int totalSess = 0, totalFocus = 0;
        java.util.List<javafx.util.Pair<LocalDateTime, Integer>> sessions = new java.util.ArrayList<>();
        for (Pomodoro pm : pomodoroService.get_all_pomodoros()) {
            if (pm.get_task_id() == taskId && is_pomodoro_visible(pm)) {
                totalSess += pm.get_completed_sessions();
                totalFocus += pm.get_total_focus_time_minutes();
                List<LocalDateTime> history = pm.get_session_history();
                List<Integer> durations = pm.get_session_durations();
                int size = Math.min(history.size(), durations.size());
                for (int i = 0; i < size; i++) {
                    sessions.add(new javafx.util.Pair<>(history.get(i), durations.get(i)));
                }
            }
        }
        ViewCoordinator.fitDetail(ctSessions, "Sessions: " + totalSess);
        ViewCoordinator.fitDetail(ctDur, "Focus: " + PomodoroView.formatFocusTime(totalFocus));
        int h = sessions.size();
        String histStr = h == 0 ? "No sessions yet" : "Last: " + sessions.get(h - 1).getKey().format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) + " (" + sessions.get(h - 1).getValue() + "m)";
        ctHist.setText(histStr);
        ViewCoordinator.fitTextWrap(ctHist, histStr,
                                ViewCoordinator.CONTENT_W,
                                ViewCoordinator.FONT_DETAIL, 9,
                                100);
        ctNavName.setText(p.get_task_name());
        ViewCoordinator.fitText(ctNavName, p.get_task_name(), ViewCoordinator.CONTENT_W, ViewCoordinator.FONT_SECTION, 9);
        ctPrev.setDisable(currentIdx <= 0);
        ctNext.setDisable(currentIdx >= pomList.size() - 1);
        btnDetails.setDisable(sessions.isEmpty());
    }

    private void updateOverview() {
        java.util.List<Pomodoro> visible = new java.util.ArrayList<>();
        for (Pomodoro p : pomodoroService.get_all_pomodoros()) {
            if (is_pomodoro_visible(p)) visible.add(p);
        }

        int allSess = 0, allFocus = 0;
        LocalDateTime earliest = null, latest = null;
        java.util.Map<String, Integer> focusByTask = new java.util.HashMap<>();
        java.util.Map<String, Integer> countsByTask = new java.util.HashMap<>();
        java.util.List<javafx.util.Pair<String, javafx.util.Pair<LocalDateTime, Integer>>> allSessions = new java.util.ArrayList<>();

        for (Pomodoro p : visible) {
            allSess += p.get_completed_sessions();
            allFocus += p.get_total_focus_time_minutes();
            focusByTask.merge(p.get_task_name(), p.get_total_focus_time_minutes(), Integer::sum);
            countsByTask.merge(p.get_task_name(), p.get_completed_sessions(), Integer::sum);

            if (!p.get_session_history().isEmpty()) {
                if (earliest == null || p.get_session_history().get(0).isBefore(earliest)) earliest = p.get_session_history().get(0);
                if (latest == null || p.get_session_history().get(p.get_session_history().size() - 1).isAfter(latest)) latest = p.get_session_history().get(p.get_session_history().size() - 1);
            }

            List<LocalDateTime> history = p.get_session_history();
            List<Integer> durations = p.get_session_durations();
            int size = Math.min(history.size(), durations.size());
            for (int i = 0; i < size; i++) {
                allSessions.add(new javafx.util.Pair<>(p.get_task_name(), new javafx.util.Pair<>(history.get(i), durations.get(i))));
            }
        }

        ovSessions.setText(String.valueOf(allSess));
        ovFocus.setText(PomodoroView.formatFocusTime(allFocus));
        if (earliest != null) ovRange.setText(earliest.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d")) + "\u2192" + latest.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d")));
        else ovRange.setText(allSess > 0 ? "\u2014 (restored)" : "No sessions yet");
        ViewCoordinator.fitStatValue(ovSessions, ovSessions.getText(), 3);
        ViewCoordinator.fitStatValue(ovFocus, ovFocus.getText(), 3);
        ViewCoordinator.fitStatValue(ovRange, ovRange.getText(), 3);

        int activeDays = 0;
        if (earliest != null && latest != null) {
            activeDays = (int) ChronoUnit.DAYS.between(earliest.toLocalDate(), latest.toLocalDate()) + 1;
            if (activeDays < 1) activeDays = 1;
        }
        double daySpan = activeDays;
        double avgPerDay = daySpan > 0 ? (double) allSess / daySpan : 0;
        StringBuilder insight = new StringBuilder();
        insight.append(allSess).append(" sessions \u00B7 ").append(PomodoroView.formatFocusTime(allFocus)).append(" total");
        if (allSess > 0) {
            insight.append(" \u00B7 ").append(String.format("%.1f/day avg", avgPerDay));
        }
        insight.append(" \u00B7 ").append(activeDays).append(" active day").append(activeDays == 1 ? "" : "s");
        ovInsight.setText(insight.toString());
        ViewCoordinator.applyFont(ovInsight, ViewCoordinator.FONT_DETAIL);

        drawMostProductive(focusByTask, countsByTask);

        allSessions.sort((a, b) -> b.getValue().getKey().compareTo(a.getValue().getKey()));
        java.util.List<javafx.util.Pair<String, javafx.util.Pair<LocalDateTime, Integer>>> last = allSessions.subList(0, Math.min(4, allSessions.size()));
        drawRecentSessions(last);
    }

    private void drawRecentSessions(java.util.List<javafx.util.Pair<String, javafx.util.Pair<LocalDateTime, Integer>>> last) {
        javafx.scene.canvas.GraphicsContext g = recentCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, ViewCoordinator.W, ViewCoordinator.H);
        if (last.isEmpty()) {
            g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            g.setTextBaseline(javafx.geometry.VPos.CENTER);
            g.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 14));
            g.setFill(javafx.scene.paint.Color.rgb(100, 100, 100));
            g.fillText("No sessions yet", ViewCoordinator.CX, ViewCoordinator.CY);
            return;
        }
        int maxDur = 0;
        for (javafx.util.Pair<String, javafx.util.Pair<LocalDateTime, Integer>> s : last) {
            maxDur = Math.max(maxDur, s.getValue().getValue());
        }
        maxDur = Math.max(maxDur, 1);
        double rowH = 38;
        double firstY = ViewCoordinator.centerY(last.size() * rowH);
        firstY = Math.max(firstY, ViewCoordinator.TITLE_Y + 30);
        double xName = ViewCoordinator.SAFE_L + 10;
        double xBar = ViewCoordinator.SAFE_L + 200;
        double maxBarW = ViewCoordinator.SAFE_R - xBar - 50;
        DateTimeFormatter relFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (int i = 0; i < last.size(); i++) {
            double y = firstY + i * rowH;
            javafx.util.Pair<String, javafx.util.Pair<LocalDateTime, Integer>> s = last.get(i);
            String taskName = s.getKey();
            java.time.LocalDateTime dt = s.getValue().getKey();
            int dur = s.getValue().getValue();
            long mins = java.time.temporal.ChronoUnit.MINUTES.between(dt, now);
            String rel;
            if (mins < 0) rel = "just now";
            else if (mins < 60) rel = mins + "m ago";
            else if (mins < 1440) rel = (mins / 60) + "h ago";
            else rel = dt.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d"));
            double bw = (double) dur / maxDur * maxBarW;
            int hue = (taskName.hashCode() * 37) % 360;
            if (hue < 0) hue += 360;
            javafx.scene.paint.Color barColor = javafx.scene.paint.Color.hsb(hue, 0.6, 0.6);
            g.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 11));
            g.setFill(javafx.scene.paint.Color.rgb(200, 200, 200));
            g.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
            g.setTextBaseline(javafx.geometry.VPos.CENTER);
            String label = taskName + " \u00B7 " + rel + " (" + dur + "m)";
            g.fillText(label, xName, y);
            g.setFill(barColor);
            g.fillRoundRect(xBar, y - 10, Math.max(bw, 4), 20, 4, 4);
            g.setFill(javafx.scene.paint.Color.WHITE);
            g.fillText(String.valueOf(dur), xBar + bw + 6, y);
        }
    }

    private void drawMostProductive(java.util.Map<String, Integer> focusByTask, java.util.Map<String, Integer> countsByTask) {
        javafx.scene.canvas.GraphicsContext g = mostProductiveCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, ViewCoordinator.W, ViewCoordinator.H);
        java.util.List<java.util.Map.Entry<String, Integer>> entries = new java.util.ArrayList<>(focusByTask.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        java.util.List<java.util.Map.Entry<String, Integer>> top = entries.subList(0, Math.min(3, entries.size()));
        if (top.isEmpty()) {
            g.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            g.setTextBaseline(javafx.geometry.VPos.CENTER);
            g.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 14));
            g.setFill(javafx.scene.paint.Color.rgb(100, 100, 100));
            g.fillText("No data yet", ViewCoordinator.CX, ViewCoordinator.CY);
            return;
        }
        int totalFocus = 0;
        for (java.util.Map.Entry<String, Integer> e : top) totalFocus += e.getValue();
        int maxFocus = top.get(0).getValue();
        double rowH = 52;
        double barH = 22;
        double totalHeight = top.size() * rowH;
        double firstY = Math.max(ViewCoordinator.TITLE_Y + 30, ViewCoordinator.centerY(totalHeight));
        double xName = ViewCoordinator.SAFE_L + 10;
        double xBar = ViewCoordinator.SAFE_L + 170;
        double maxBarW = ViewCoordinator.SAFE_R - xBar - 60;
        for (int i = 0; i < top.size(); i++) {
            double y = firstY + i * rowH;
            java.util.Map.Entry<String, Integer> e = top.get(i);
            String name = e.getKey();
            int focus = e.getValue();
            int count = countsByTask.getOrDefault(name, 0);
            double pct = totalFocus > 0 ? (focus * 100.0 / totalFocus) : 0;
            double bw = (double) focus / maxFocus * maxBarW;
            int hue = (name.hashCode() * 37) % 360;
            if (hue < 0) hue += 360;
            javafx.scene.paint.Color barColor = javafx.scene.paint.Color.hsb(hue, 0.55, 0.55);
            g.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 12));
            g.setFill(javafx.scene.paint.Color.WHITE);
            g.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
            g.setTextBaseline(javafx.geometry.VPos.CENTER);
            g.fillText(name, xName, y);
            g.setFill(barColor);
            g.fillRoundRect(xBar, y - barH / 2, Math.max(bw, 4), barH, 4, 4);
            g.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 11));
            g.setFill(javafx.scene.paint.Color.WHITE);
            g.setTextAlign(javafx.scene.text.TextAlignment.RIGHT);
            g.fillText(count + " sessions", xBar + bw + 6, y - 6);
            g.setFont(javafx.scene.text.Font.font("Segoe UI", javafx.scene.text.FontWeight.BOLD, 10));
            g.setFill(javafx.scene.paint.Color.rgb(180, 180, 180));
            g.fillText(String.format("%.0f%%", pct), xBar + bw + 6, y + 8);
        }
    }

    private void showSessionDetails() {
        if (currentTaskId <= 0 || pomList.isEmpty()) return;
        java.util.List<javafx.util.Pair<LocalDateTime, Integer>> all = pomodoroService.get_task_sessions(currentTaskId);
        java.util.List<javafx.util.Pair<LocalDateTime, Integer>> visible = new java.util.ArrayList<>();
        for (Pomodoro pm : pomodoroService.get_all_pomodoros()) {
            if (pm.get_task_id() == currentTaskId && is_pomodoro_visible(pm)) {
                List<LocalDateTime> history = pm.get_session_history();
                List<Integer> durations = pm.get_session_durations();
                int size = Math.min(history.size(), durations.size());
                for (int i = 0; i < size; i++) {
                    visible.add(new javafx.util.Pair<>(history.get(i), durations.get(i)));
                }
            }
        }
        visible.sort((a, b) -> a.getKey().compareTo(b.getKey()));
        if (visible.isEmpty()) {
            ctDetail.setText("No sessions for this task");
        } else {
            StringBuilder sb = new StringBuilder();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, HH:mm");
            for (javafx.util.Pair<LocalDateTime, Integer> s : visible) {
                sb.append("\u2022 ").append(s.getKey().format(fmt)).append(" (").append(s.getValue()).append("m)\n");
            }
            ctDetail.setText(sb.toString());
        }
        detailBox.setVisible(false); detailBox.setManaged(false);
        durBox.setVisible(false); durBox.setManaged(false);
        resetBox.setVisible(false); resetBox.setManaged(false);
        deleteBox.setVisible(false); deleteBox.setManaged(false);
        detailSessionBox.setVisible(true); detailSessionBox.setManaged(true);
    }
}
