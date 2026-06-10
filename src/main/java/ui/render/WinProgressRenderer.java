package ui.render;

import javafx.scene.control.Label;

/**
 * Formats and labels progress toward the three complete-set win condition.
 * <p>
 * Progress is shown as filled and empty dots, e.g. {@code Sets ●●○ 2/3}.
 */
public final class WinProgressRenderer {
    /** Number of complete property sets required to win. */
    public static final int SETS_TO_WIN = 3;

    private WinProgressRenderer() {
    }

    /** Creates a styled label showing win progress dots. */
    public static Label createLabel(int completeSets) {
        Label label = new Label(format(completeSets));
        label.getStyleClass().add("win-progress-label");
        if (completeSets >= SETS_TO_WIN) {
            label.getStyleClass().add("win-progress-ready");
        }
        return label;
    }

    /** Returns formatted progress text such as {@code Sets ●●○ 2/3}. */
    public static String format(int completeSets) {
        int clamped = Math.max(0, Math.min(SETS_TO_WIN, completeSets));
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < SETS_TO_WIN; i++) {
            dots.append(i < clamped ? "●" : "○");
        }
        return "Sets " + dots + " " + clamped + "/" + SETS_TO_WIN;
    }
}
