package ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.scene.control.Label;
import javafx.util.Duration;

/** Fades status text into the game board status label. */
public final class StatusMessageDisplay {
    private final Label statusMessage;
    private String lastMessage = "";
    private boolean lastError = false;
    private FadeTransition fadeTransition;

    public StatusMessageDisplay(Label statusMessage) {
        this.statusMessage = statusMessage;
    }

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
