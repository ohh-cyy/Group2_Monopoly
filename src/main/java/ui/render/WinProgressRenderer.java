package ui.render;

import javafx.scene.control.Label;

/** Text progress for the 3-set win condition. */
public final class WinProgressRenderer {
    public static final int SETS_TO_WIN = 3;

    private WinProgressRenderer() {
    }

    public static Label createLabel(int completeSets) {
        Label label = new Label(format(completeSets));
        label.getStyleClass().add("win-progress-label");
        if (completeSets >= SETS_TO_WIN) {
            label.getStyleClass().add("win-progress-ready");
        }
        return label;
    }

    public static String format(int completeSets) {
        int clamped = Math.max(0, Math.min(SETS_TO_WIN, completeSets));
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < SETS_TO_WIN; i++) {
            dots.append(i < clamped ? "●" : "○");
        }
        return "Sets " + dots + " " + clamped + "/" + SETS_TO_WIN;
    }
}
