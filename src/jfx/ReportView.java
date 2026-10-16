package jfx;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import models.Pomodoro;
import models.Status;
import models.Task;
import services.*;

public class ReportView extends StackPane {
    private final TaskService taskService;
    private final UserService userService;
    private final PomodoroService pomodoroService;
    private final ReportService reportService;
    private final SwipeContainer swipe;
    private final Canvas overviewCanvas, ringCanvas, sessCanvas, perfCanvas;

    private long statTotal, statSessions, statActive, statOverdue;
    private int pend, prog, done, canc;
    private List<Task> taskList;
    private Map<LocalDate, Integer> dayCount;
    private TreeMap<Integer, Integer> sessPerTask;
    private String topTask;

    public ReportView(TaskService ts, UserService us, PomodoroService ps) {
        this.taskService = ts; this.userService = us; this.pomodoroService = ps;
        this.reportService = new ReportService(ts, us, ps);
        setPrefSize(ViewCoordinator.W, ViewCoordinator.H);

        overviewCanvas = new Canvas(ViewCoordinator.W, ViewCoordinator.H);
        ringCanvas = new Canvas(ViewCoordinator.W, ViewCoordinator.H);
        sessCanvas = new Canvas(ViewCoordinator.W, ViewCoordinator.H);
        perfCanvas = new Canvas(ViewCoordinator.W, ViewCoordinator.H);

        Button settingsBtn = new Button("\u2699");
        settingsBtn.getStyleClass().add("settings-btn");

        Button backBtn = new Button("\u2190 Back");
        backBtn.getStyleClass().add("back-btn");

        Button saveBtn = new Button("Save Report");
        saveBtn.getStyleClass().addAll("btn", "btn-grey");

        Node ov = buildOverview();
        Node rc = buildRingChart(settingsBtn);
        Node sc = buildSessBars();
        Node pf = buildPerf();
        Node st = buildSettings(backBtn, saveBtn);

        swipe = new SwipeContainer(ov, rc, sc, pf, st);
        getChildren().add(swipe);

        settingsBtn.setOnAction(e -> swipe.goTo(4));
        backBtn.setOnAction(e -> swipe.showContent());
        saveBtn.setOnAction(e -> save_report());

        refresh();
    }

    private Node buildOverview() {
        StackPane sp = new StackPane(overviewCanvas);
        sp.setPrefSize(ViewCoordinator.W, ViewCoordinator.H);
        return sp;
    }

    private Node buildRingChart(Button settingsBtn) {
        StackPane sp = new StackPane(ringCanvas);
        sp.setPrefSize(ViewCoordinator.W, ViewCoordinator.H);
        StackPane.setAlignment(settingsBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsBtn, new Insets(12, 55, 0, 0));
        sp.getChildren().add(settingsBtn);
        return sp;
    }

    private Node buildSessBars() {
        StackPane sp = new StackPane(sessCanvas);
        sp.setPrefSize(ViewCoordinator.W, ViewCoordinator.H);
        return sp;
    }

    private Node buildPerf() {
        StackPane sp = new StackPane(perfCanvas);
        sp.setPrefSize(ViewCoordinator.W, ViewCoordinator.H);
        return sp;
    }

    private Label errLabel;

    private Node buildSettings(Button backBtn, Button saveBtn) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(ViewCoordinator.W, ViewCoordinator.H);
        box.setPadding(new Insets(0, 55, 0, 55));
        Label title = new Label("REPORTS");
        title.setStyle("-fx-font-weight: 900; -fx-text-fill: #888888; -fx-padding: 0 0 4 0; -fx-letter-spacing: 2px;");
        ViewCoordinator.applyFont(title, ViewCoordinator.FONT_SECTION);
        errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #cc6666; -fx-padding: 6 0;");
        ViewCoordinator.applyFont(errLabel, ViewCoordinator.FONT_DETAIL);
        errLabel.setWrapText(true);
        errLabel.setMaxWidth(500);
        errLabel.setVisible(false);
        errLabel.setOnMouseClicked(e -> errLabel.setVisible(false));
        box.getChildren().addAll(backBtn, title, saveBtn, errLabel);
        return box;
    }

    public void showContentPage() { refresh(); swipe.restoreLastPage(); }

    public void refresh() {
        gather_data();
        drawOverview();
        drawRingChart();
        drawSessBars();
        drawPerf();
    }

    private void gather_data() {
        taskList = taskService.get_all_tasks();
        statTotal = taskList.size();
        statSessions = pomodoroService.get_total_stored_sessions(taskService);
        statActive = taskList.stream().filter(t ->
            t.get_status() == Status.PENDING || t.get_status() == Status.IN_PROGRESS).count();
        statOverdue = taskService.count_overdue_tasks();

        pend = 0; prog = 0; done = 0; canc = 0;
        for (Task t : taskList) {
            switch (t.get_status()) {
                case PENDING: pend++; break;
                case IN_PROGRESS: prog++; break;
                case COMPLETED: done++; break;
                case CANCELLED: canc++; break;
            }
        }

        sessPerTask = new TreeMap<>();
        Map<Integer, Integer> storedByTaskId = pomodoroService.get_stored_sessions_by_task_id(taskService);
        for (Task t : taskList) {
            if (!t.is_store_sessions()) continue;
            Integer sess = storedByTaskId.get(t.get_id());
            if (sess != null && sess > 0) sessPerTask.put(t.get_id(), sess);
        }

        dayCount = new TreeMap<>();
        for (Pomodoro p : pomodoroService.get_all_pomodoros()) {
            java.util.Optional<Task> task = taskService.try_get_task(p.get_task_id());
            if (!task.isPresent() || !task.get().is_store_sessions()) continue;
            for (LocalDateTime dt : p.get_session_history()) {
                LocalDate d = dt.toLocalDate();
                LocalDate cutoff = LocalDate.now().minusDays(45);
                if (d.isAfter(cutoff) || d.isEqual(cutoff))
                    dayCount.put(d, dayCount.getOrDefault(d, 0) + 1);
            }
        }

        topTask = pomodoroService.get_most_productive_stored_task(taskService);
    }

    private void drawOverview() {
        GraphicsContext g = overviewCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, ViewCoordinator.W, ViewCoordinator.H);
        ViewCoordinator.drawTitle(g, "OVERVIEW");

        double row1y = ViewCoordinator.CONTENT_CY - 160;
        double row2y = ViewCoordinator.CONTENT_CY + 100;
        ViewCoordinator.drawStat(g, ViewCoordinator.CX - 100, row1y, String.valueOf(statTotal), "Tasks");
        ViewCoordinator.drawStat(g, ViewCoordinator.CX + 100, row1y, String.valueOf(statActive), "Active");
        ViewCoordinator.drawStat(g, ViewCoordinator.CX - 100, row2y, String.valueOf(statSessions), "Sessions");
        ViewCoordinator.drawStat(g, ViewCoordinator.CX + 100, row2y, String.valueOf(statOverdue), "Overdue");

        if (topTask != null)
            ViewCoordinator.drawFooter(g, ViewCoordinator.truncate("BEST: " + topTask, (int)ViewCoordinator.FONT_SMALL, ViewCoordinator.CONTENT_W));
    }

    private void drawRingChart() {
        GraphicsContext g = ringCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, ViewCoordinator.W, ViewCoordinator.H);
        ViewCoordinator.drawTitle(g, "TASK STATUS");

        double r = Math.min(ViewCoordinator.SAFE_W, ViewCoordinator.SAFE_H) / 2 - 60;
        double cy = ViewCoordinator.CONTENT_CY - 20;
        int[] vals = {pend, prog, done, canc};
        int total = pend + prog + done + canc;
        Color[] colors = {Color.rgb(200, 160, 0), Color.rgb(60, 140, 255), Color.rgb(0, 200, 100), Color.rgb(120, 120, 120)};
        String[] labels = {"Pending", "In Progress", "Completed", "Cancelled"};

        if (total == 0) {
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            g.setFill(Color.rgb(100, 100, 100));
            g.fillText("No data", ViewCoordinator.CX, cy);
        } else {
            double start = 90;
            for (int i = 0; i < 4; i++) {
                if (vals[i] > 0) {
                    double sweep = -360.0 * vals[i] / total;
                    g.setLineWidth(30);
                    g.setStroke(colors[i]);
                    g.strokeArc(ViewCoordinator.CX - r, cy - r, r * 2, r * 2, start, sweep, ArcType.OPEN);
                    start += sweep;
                }
            }

            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
            g.setFill(Color.WHITE);
            g.fillText(String.valueOf(total), ViewCoordinator.CX, cy);
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
            g.setFill(Color.rgb(150, 150, 150));
            g.fillText("TOTAL", ViewCoordinator.CX, cy + 24);

            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.CENTER);
            // Two-per-row legend, centered horizontally, kept above the footer band.
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            double colW = 150;
            double legendW = colW * 2;
            double lx0 = ViewCoordinator.CX - legendW / 2;
            double ly0 = cy + r + 26;
            for (int i = 0; i < 4; i++) {
                double lx = lx0 + (i % 2) * colW;
                double ly = ly0 + (i / 2) * 20;
                g.setFill(colors[i]);
                g.fillOval(lx, ly - 4, 10, 10);
                g.setFill(Color.rgb(200, 200, 200));
                g.fillText(labels[i] + " " + vals[i], lx + 16, ly);
            }
        }
    }

    private void drawSessBars() {
        GraphicsContext g = sessCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, ViewCoordinator.W, ViewCoordinator.H);
        ViewCoordinator.drawTitle(g, "SESSIONS PER TASK");

        int n = sessPerTask.size();
        if (n > 0) {
            List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(sessPerTask.entrySet());
            entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            int maxVal = entries.get(0).getValue();

            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.CENTER);

            double xName = ViewCoordinator.SAFE_L + 5;
            double xBar = ViewCoordinator.SAFE_L + 190;
            double maxBarW = ViewCoordinator.SAFE_R - xBar - 40;
            double barH = 22;
            double rowH = 34;
            double firstY = Math.max(ViewCoordinator.TITLE_Y + 30, ViewCoordinator.centerY(entries.size() * rowH));
            Color barColor = Color.rgb(60, 160, 255);

            int idx = 0;
            for (Map.Entry<Integer, Integer> e : entries) {
                double y = firstY + idx * rowH;
                if (y + barH / 2 > ViewCoordinator.FOOTER_START - 20) {
                    g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
                    g.setFill(Color.rgb(100, 100, 100));
                    g.fillText("\u2026 " + (entries.size() - idx) + " more", xName, y);
                    break;
                }

                String name = "";
                for (Task t : taskList) if (t.get_id() == e.getKey()) { name = t.get_title(); break; }
                int maxNameLen = ViewCoordinator.maxChars(ViewCoordinator.FONT_BODY, xBar - xName - 8);
                if (name.length() > maxNameLen) name = name.substring(0, maxNameLen - 1) + "\u2026";

                double bw = (double) e.getValue() / maxVal * maxBarW;

                g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                g.setFill(Color.rgb(200, 200, 200));
                g.fillText(name, xName, y);

                g.setFill(barColor);
                g.fillRoundRect(xBar, y - barH / 2, Math.max(bw, 4), barH, 4, 4);

                g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
                g.setFill(Color.WHITE);
                g.fillText(String.valueOf(e.getValue()), xBar + bw + 8, y);

                idx++;
            }
        } else {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            g.setFill(Color.rgb(100, 100, 100));
            g.fillText("No session data", ViewCoordinator.CX, ViewCoordinator.CONTENT_CY);
        }
    }

    private void drawPerf() {
        GraphicsContext g = perfCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, ViewCoordinator.W, ViewCoordinator.H);
        ViewCoordinator.drawTitle(g, "DAILY PERFORMANCE (45 days)");

        double cy = ViewCoordinator.CONTENT_CY - 30;
        double r = Math.min(ViewCoordinator.SAFE_W, ViewCoordinator.SAFE_H) / 2 - 60;
        int totalDays = dayCount.size();
        int maxDay = dayCount.values().stream().mapToInt(v -> v).max().orElse(1);

        g.setStroke(Color.rgb(50, 50, 50));
        g.setLineWidth(2);
        g.strokeOval(ViewCoordinator.CX - r, cy - r, r * 2, r * 2);

        if (totalDays > 0) {
            double dotR = 8;
            int idx = 0;
            for (Map.Entry<LocalDate, Integer> e : dayCount.entrySet()) {
                double ang = Math.toRadians(idx * 360.0 / Math.max(totalDays, 45) - 90);
                double dx = ViewCoordinator.CX + (r - 2) * Math.cos(ang);
                double dy = cy + (r - 2) * Math.sin(ang);
                double frac = Math.min(1, e.getValue() / (double) Math.max(maxDay, 1));
                Color c = Color.rgb(160, 160, 160, 0.3 + frac * 0.7);
                g.setFill(c);
                g.fillOval(dx - dotR, dy - dotR, dotR * 2, dotR * 2);
                idx++;
            }
        } else {
            g.setTextAlign(TextAlignment.CENTER);
            g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            g.setFill(Color.rgb(100, 100, 100));
            g.fillText("No performance data", ViewCoordinator.CX, cy);
        }

        LocalDate today = LocalDate.now();
        int todaySess = dayCount.getOrDefault(today, 0);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        g.setFill(Color.WHITE);
        g.fillText(String.valueOf(todaySess), ViewCoordinator.CX, cy - 6);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        g.setFill(Color.rgb(150, 150, 150));
        g.fillText("TODAY", ViewCoordinator.CX, cy + 18);

        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        g.setFill(Color.rgb(150, 150, 150));
        double textBaseY = cy + r + 24;
        g.fillText("Sessions (45d): " + dayCount.values().stream().mapToInt(v -> v).sum(), ViewCoordinator.CX, textBaseY);
        g.fillText("Active days: " + totalDays + "   \u00B7   Best day: " + maxDay, ViewCoordinator.CX, textBaseY + 18);
    }

    private void save_report() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Report");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
        java.io.File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;
        try (FileWriter w = new FileWriter(f)) {
            w.write(reportService.generate_summary_report());
        } catch (IOException e) {
            errLabel.setText("Could not save: " + e.getMessage());
            errLabel.setVisible(true);
        }
    }
}
