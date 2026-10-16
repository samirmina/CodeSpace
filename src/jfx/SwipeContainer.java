package jfx;

import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class SwipeContainer extends StackPane {
    private static final double W = 660;
    private static final double H = 660;
    private static final Duration DUR = Duration.millis(220);

    private final Node[] pages;
    private int current = 0;
    private int lastPage = 1;
    private double startX;
    private boolean dragging;

    public SwipeContainer(Node... pagesArr) {
        pages = pagesArr;
        setPrefSize(W, H);

        getChildren().addAll(pages);
        for (int i = 0; i < pages.length; i++) pages[i].setTranslateX(i * W);

        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> { startX = e.getX(); dragging = false; });
        addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onDrag);
        addEventFilter(MouseEvent.MOUSE_RELEASED, this::onRelease);
    }

    private void onDrag(MouseEvent e) {
        double dx = e.getX() - startX;
        if (!dragging && Math.abs(dx) > 15) dragging = true;
        if (dragging) {
            e.consume();
            double offset = e.getX() - startX;
            for (int i = 0; i < pages.length; i++)
                pages[i].setTranslateX((i - current) * W + offset);
        }
    }

    private void onRelease(MouseEvent e) {
        if (!dragging) return;
        e.consume();
        double dx = e.getX() - startX;
        if (Math.abs(dx) > 60) {
            int target = current + (dx < 0 ? 1 : -1);
            if (target >= 0 && target < pages.length) goTo(target);
            else snapTo(current);
        } else {
            snapTo(current);
        }
        dragging = false;
    }

    public void goTo(int page) {
        if (page < 0 || page >= pages.length) return;
        lastPage = current;
        current = page;
        for (int i = 0; i < pages.length; i++) {
            TranslateTransition tt = new TranslateTransition(DUR, pages[i]);
            tt.setToX((i - current) * W);
            tt.play();
        }
    }

    private void snapTo(int page) {
        for (int i = 0; i < pages.length; i++) {
            TranslateTransition tt = new TranslateTransition(DUR, pages[i]);
            tt.setToX((i - page) * W);
            tt.play();
        }
    }

    public void goToImmediate(int page) {
        if (page < 0 || page >= pages.length) return;
        lastPage = current;
        current = page;
        for (int i = 0; i < pages.length; i++) pages[i].setTranslateX((i - current) * W);
    }

    public void showSettings() { goTo(pages.length - 1); }
    public void showContent() { goTo(1); }
    public void showOverview() { goTo(0); }
    public void showOverviewImmediate() { goToImmediate(0); }
    public void showContentImmediate() { goToImmediate(1); }

    /**
     * Restore the page that was active before this container was hidden
     * (e.g. when switching back to a tab).
     */
    public void restoreLastPage() {
        int target = lastPage;
        if (target < 0 || target >= pages.length) target = 1;
        goToImmediate(target);
    }
}
