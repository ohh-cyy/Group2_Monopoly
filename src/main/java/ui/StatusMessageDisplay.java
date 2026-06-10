package ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Displays transient status messages on the game board with a fade-in animation.
 * <p>
 * Error messages are routed to {@link GameAlertDialogs#showError(javafx.scene.Node, String)}
 * instead of updating the label.
 */
public final class StatusMessageDisplay {
    private final Label statusMessage;
    private String lastMessage = "";
    private boolean lastError = false;
    private FadeTransition fadeTransition;

    /**
     * Creates a display bound to the given status label.
     *
     * @param statusMessage label that receives non-error status text
     */
    public StatusMessageDisplay(Label statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * Shows a status message with fade-in animation.
     * <p>
     * Error messages are routed to {@link GameAlertDialogs#showError}.
     *
     * @param message  text to display
     * @param isError  when {@code true}, opens an error dialog instead of fading into the label
     */
    public void show(String message, boolean isError) {
        if (isError) {
            GameAlertDialogs.showError(statusMessage, message);
            return;
        }
        if (statusMessage == null) {
            return;
        }
        if (message.equals(lastMessage) && isError == lastError) {
            return;
        }
        lastMessage = message;
        lastError = isError;

        if (fadeTransition != null) {
            fadeTransition.stop();
        }
        statusMessage.setOpacity(0.4);
        statusMessage.setText(message);
        statusMessage.setStyle(isError
                ? "-fx-text-fill: #e74c3c; -fx-font-size: 14px;"
                : "-fx-text-fill: white; -fx-font-size: 14px;");
        fadeTransition = new FadeTransition(Duration.millis(160), statusMessage);
        fadeTransition.setFromValue(0.4);
        fadeTransition.setToValue(1);
        fadeTransition.setInterpolator(Interpolator.EASE_OUT);
        fadeTransition.play();
    }
}
