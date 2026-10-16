package jfx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public final class ViewCoordinator {
    private ViewCoordinator() {}

    public static final double W = 660, H = 660;
    public static final double CX = W / 2, CY = H / 2;
    public static final double R = 330;
    public static final double MARGIN = 132;
    public static final double SAFE_L = MARGIN, SAFE_T = MARGIN;
    public static final double SAFE_R = W - MARGIN, SAFE_B = H - MARGIN;
    public static final double SAFE_W = W - 2 * MARGIN, SAFE_H = H - 2 * MARGIN;
    public static final double TITLE_Y = 140;
    public static final double HEADER_END = 160;
    public static final double FOOTER_START = 500;
    public static final double CONTENT_CY = CY;
    public static final double SPACING = clamp(4, SAFE_W * 0.015, 10);
    public static final double GRID_GAP = clamp(6, SAFE_W * 0.028, 14);
    public static final double FONT_TITLE = clamp(9, SAFE_W * 0.028, 14);
    public static final double FONT_STAT_VALUE = clamp(22, SAFE_W * 0.085, 44);
    public static final double FONT_STAT_LABEL = clamp(9, SAFE_W * 0.024, 13);
    public static final double FONT_BODY = clamp(9, SAFE_W * 0.024, 13);
    public static final double FONT_SMALL = clamp(8, SAFE_W * 0.022, 12);
    public static final double FONT_DETAIL_TITLE = clamp(15, SAFE_W * 0.056, 24);
    public static final double FONT_DETAIL = clamp(11, SAFE_W * 0.035, 15);
    public static final double FONT_SECTION = clamp(10, SAFE_W * 0.030, 13);
    public static final double FONT_PAGE_TITLE = clamp(12, SAFE_W * 0.038, 16);

    private static double clamp(double min, double val, double max) {
        return Math.max(min, Math.min(max, val));
    }

    public static VBox createPage() {
        VBox box = new VBox(SPACING);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(W, H);
        box.setPadding(new Insets(20, MARGIN, 20, MARGIN));
        return box;
    }

    public static double centerY(double contentHeight) {
        return HEADER_END + (FOOTER_START - HEADER_END - contentHeight) / 2;
    }

    public static void drawTitle(GraphicsContext g, String text) {
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, FONT_TITLE));
        g.setFill(Color.rgb(170, 170, 170));
        g.fillText(text, CX, TITLE_Y);
    }

    public static void drawStat(GraphicsContext g, double cx, double cy, String value, String label) {
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, FONT_STAT_VALUE));
        g.setFill(Color.WHITE);
        g.fillText(value, cx, cy - 10);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, FONT_STAT_LABEL));
        g.setFill(Color.rgb(150, 150, 150));
        g.fillText(label.toUpperCase(), cx, cy + 24);
    }

    public static void drawFooter(GraphicsContext g, String text) {
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, FONT_SMALL));
        g.setFill(Color.rgb(130, 130, 130));
        g.fillText(text, CX, FOOTER_START);
    }

    private static final Text METER = new Text();

    // ---- Truncation (used only for stat card values as a last resort) ----

    public static String truncate(String text, double fontSize, double maxWidth) {
        if (text == null || text.isEmpty()) return text;
        METER.setFont(Font.font("Segoe UI", FontWeight.BOLD, fontSize));
        METER.setText(text);
        if (METER.getLayoutBounds().getWidth() <= maxWidth) return text;
        String ell = "\u2026";
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            METER.setText(text.substring(0, mid) + ell);
            if (METER.getLayoutBounds().getWidth() <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return lo <= 0 ? ell : text.substring(0, lo) + ell;
    }

    public static int maxChars(double fontSize, double maxWidth) {
        return (int)(maxWidth / (fontSize * 0.55));
    }

    // ---- Layout constants for anti-congestion system ----

    public static final double SAFE_RADIUS = R * 0.6;
    public static final double CONTENT_W = SAFE_RADIUS * 2;
    public static final double CARD_GAP = 8;
    public static final double CARD_PAD_X = 12;

    // ---- Anti-congestion font fitters (NO truncation, NO fallbacks) ----

    /**
     * Core fitter: maximize font within maxWidth. If even minFont is too wide,
     * enable JavaFX wrapping and binary-search the largest font within a
     * generous 300px height budget so text remains fully visible. No "...".
     */
    public static void fitText(javafx.scene.control.Label label, String text,
                               double maxWidth, double maxFont, double minFont) {
        if (text == null) text = "";
        double size = maxFont;
        METER.setWrappingWidth(0);
        METER.setText(text);
        while (size > minFont) {
            METER.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
            if (METER.getLayoutBounds().getWidth() <= maxWidth) break;
            size -= 0.5;
        }
        METER.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
        METER.setText(text);
        if (METER.getLayoutBounds().getWidth() > maxWidth) {
            double lo = minFont, hi = maxFont;
            double best = minFont;
            while (hi - lo > 0.5) {
                double mid = (lo + hi) / 2;
                METER.setFont(Font.font("Segoe UI", FontWeight.BOLD, mid));
                METER.setWrappingWidth(maxWidth);
                if (METER.getLayoutBounds().getHeight() <= 300) { best = mid; lo = mid; }
                else hi = mid;
            }
            size = best;
            label.setWrapText(true);
        }
        label.setText(text);
        label.setStyle(replaceFontSize(label.getStyle(), size));
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(maxWidth);
        METER.setWrappingWidth(0);
    }

    /**
     * Like fitText but with an explicit height budget. Use for labels you know
     * should wrap rather than grow indefinitely.
     */
    public static void fitTextWrap(javafx.scene.control.Label label, String text,
                                   double maxWidth, double maxFont, double minFont,
                                   double maxHeight) {
        if (text == null) text = "";
        double size = maxFont;
        METER.setWrappingWidth(maxWidth);
        METER.setText(text);
        while (size > minFont) {
            METER.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
            if (METER.getLayoutBounds().getHeight() <= maxHeight) break;
            size -= 0.5;
        }
        label.setText(text);
        label.setWrapText(true);
        label.setStyle(replaceFontSize(label.getStyle(), size));
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(maxWidth);
        METER.setWrappingWidth(0);
    }

    /**
     * Non-wrapping fit: shrink until text fits on one line, but NEVER truncate.
     * If text still exceeds maxWidth at minFont, leave it at minFont and let
     * the Label's bounds handle it rather than showing "...".
     */
    public static void fitLabel(javafx.scene.control.Label label, String text,
                                double maxWidth, double maxFont, double minFont) {
        if (text == null) text = "";
        double size = maxFont;
        METER.setWrappingWidth(0);
        METER.setText(text);
        METER.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
        while (size > minFont && METER.getLayoutBounds().getWidth() > maxWidth) {
            size -= 0.5;
            METER.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
        }
        METER.setText(text);
        label.setText(text);
        label.setStyle(replaceFontSize(label.getStyle(), size));
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(maxWidth);
    }

    // ---- Card sizing ----

    public static double statCardWidth(int n) {
        if (n <= 0) return CONTENT_W;
        return (CONTENT_W - (n - 1) * CARD_GAP) / n;
    }

    public static VBox statCard(javafx.scene.control.Label value, String label, int cardCount) {
        double cardW = statCardWidth(cardCount);
        VBox c = new VBox(2);
        c.setAlignment(Pos.CENTER);
        c.getStyleClass().add("stat-card");
        c.setMinWidth(cardW);
        c.setPrefWidth(cardW);
        c.setMaxWidth(cardW);

        if (!value.getStyleClass().contains("stat-value")) value.getStyleClass().add("stat-value");
        value.setAlignment(Pos.CENTER);
        value.setTextAlignment(TextAlignment.CENTER);
        value.setMaxWidth(cardW - CARD_PAD_X);

        javafx.scene.control.Label l = new javafx.scene.control.Label(label);
        l.getStyleClass().add("stat-label");
        l.setAlignment(Pos.CENTER);
        l.setTextAlignment(TextAlignment.CENTER);
        l.setMaxWidth(cardW - CARD_PAD_X);

        c.getChildren().addAll(value, l);
        fitStatValue(value, value.getText(), cardCount);
        return c;
    }

    public static void fitStatValue(javafx.scene.control.Label value, String text, int cardCount) {
        double cardW = statCardWidth(cardCount);
        double avail = cardW - CARD_PAD_X;
        fitText(value, text, avail, FONT_STAT_VALUE, 9);
        value.setAlignment(Pos.CENTER);
        value.setTextAlignment(TextAlignment.CENTER);
        value.setMaxWidth(avail);
    }

    // ---- Detail-panel text sizing ----

    public static void fitDetailTitle(javafx.scene.control.Label label, String text) {
        fitText(label, text, CONTENT_W, FONT_DETAIL_TITLE, 9);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    public static void fitDetail(javafx.scene.control.Label label, String text) {
        fitText(label, text, CONTENT_W, FONT_DETAIL, 9);
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    // ---- Helper: set a fixed coordinator-tier font and center ----

    public static void applyFont(javafx.scene.control.Label label, double fontSize) {
        label.setStyle(replaceFontSize(label.getStyle(), fontSize));
        label.setAlignment(Pos.CENTER);
        label.setTextAlignment(TextAlignment.CENTER);
    }

    // ---- Private: inline style helper ----

    private static String replaceFontSize(String style, double size) {
        String fs = "-fx-font-size: " + (int)Math.round(size) + "px;";
        if (style == null || style.isEmpty()) return fs;
        if (style.contains("-fx-font-size")) {
            return style.replaceAll("-fx-font-size:\\s*[0-9.]+px;?", fs);
        }
        return style.endsWith(";") ? style + " " + fs : style + "; " + fs;
    }
}
