package ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Shows a prominent, non-blocking notification when an online turn reaches the local player.
 * <p>
 * The supplied FXML overlay remains in the scene graph so its layout is stable, but is hidden
 * between notifications. Repeated calls restart the same animation instead of stacking nodes.
 */
public final class YourTurnOverlay {
    /** Full-board overlay styled by the {@code your-turn-*} CSS classes. */
    private final StackPane overlay;
    /** Current animation, retained so a newer turn notification can replace it cleanly. */
    private SequentialTransition animation;

    /**
     * Creates a notification controller for an existing FXML overlay.
     *
     * @param overlay full-board, mouse-transparent overlay to animate
     */
    public YourTurnOverlay(StackPane overlay) {
        this.overlay = overlay;
    }

    /** Requests the notification on the JavaFX application thread. */
    public void show() {
        if (overlay == null) {
            return;
        }
        Platform.runLater(this::play);
    }

    private void play() {
        if (animation != null) {
            animation.stop();
        }

        // Reset every animated property because the same FXML node is reused for each turn.
        overlay.setVisible(true);
        overlay.setOpacity(0);
        overlay.setScaleX(0.72);
        overlay.setScaleY(0.72);
        overlay.setTranslateY(18);
        overlay.toFront();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(190), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(260), overlay);
        scaleIn.setFromX(0.72);
        scaleIn.setFromY(0.72);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        TranslateTransition riseIn = new TranslateTransition(Duration.millis(260), overlay);
        riseIn.setFromY(18);
        riseIn.setToY(0);

        // Keep the message readable without blocking card or control input underneath it.
        PauseTransition hold = new PauseTransition(Duration.millis(1_350));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(420), overlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(420), overlay);
        scaleOut.setFromX(1);
        scaleOut.setFromY(1);
        scaleOut.setToX(1.08);
        scaleOut.setToY(1.08);
        TranslateTransition riseOut = new TranslateTransition(Duration.millis(420), overlay);
        riseOut.setFromY(0);
        riseOut.setToY(-24);

        animation = new SequentialTransition(
                new ParallelTransition(fadeIn, scaleIn, riseIn),
                hold,
                new ParallelTransition(fadeOut, scaleOut, riseOut));
        animation.setOnFinished(event -> {
            // Hiding instead of removing preserves FXML ownership and avoids layout churn.
            overlay.setVisible(false);
            overlay.setTranslateY(0);
        });
        animation.play();
    }
}
