package ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Objects;

/**
 * Full-screen semi-transparent victory overlay with procedural fireworks.
 */
public final class GameVictoryScreen {
    private static final String OVERLAY_HOST_KEY = "victoryOverlayHost";

    private GameVictoryScreen() {
    }

    public static void show(Node anchor, String winnerName) {
        show(anchor, winnerName, null);
    }

    public static void show(Node anchor, String winnerName, Runnable onDismissed) {
        Platform.runLater(() -> showOverlay(anchor, winnerName, onDismissed));
    }

    private static void showOverlay(Node anchor, String winnerName, Runnable onDismissed) {
        if (anchor == null || anchor.getScene() == null) {
            return;
        }
        Scene scene = anchor.getScene();
        StackPane host = ensureOverlayHost(scene);
        if (host.getChildren().stream().anyMatch(node -> node.getStyleClass().contains("victory-overlay"))) {
            return;
        }

        String name = winnerName == null || winnerName.isBlank() ? "Unknown" : winnerName.trim();

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("victory-overlay");
        overlay.setPickOnBounds(true);

        VBox content = new VBox(18);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("victory-overlay-content");
        content.setMaxWidth(640);

        VictoryFireworksView fireworks = new VictoryFireworksView(520, 360);
        Label winnerLabel = new Label(name + " wins this game!");
        winnerLabel.getStyleClass().add("victory-overlay-winner");
        Label hintLabel = new Label("Click anywhere to close");
        hintLabel.getStyleClass().add("victory-overlay-hint");

        content.getChildren().addAll(fireworks, winnerLabel, hintLabel);
        overlay.getChildren().add(content);
        StackPane.setAlignment(content, Pos.CENTER);

        overlay.setOpacity(0);
        host.getChildren().add(overlay);
        overlay.toFront();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(450), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        overlay.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (overlay.getProperties().putIfAbsent("victoryDismissing", Boolean.TRUE) != null) {
                return;
            }
            event.consume();
            dismissOverlay(host, overlay, onDismissed);
        });
    }

    private static void dismissOverlay(StackPane host, StackPane overlay, Runnable onDismissed) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(280), overlay);
        fadeOut.setFromValue(overlay.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            host.getChildren().remove(overlay);
            if (onDismissed != null) {
                Platform.runLater(onDismissed);
            }
        });
        fadeOut.play();
    }

    private static StackPane ensureOverlayHost(Scene scene) {
        Parent root = scene.getRoot();
        if (root instanceof StackPane stack
                && Boolean.TRUE.equals(stack.getProperties().get(OVERLAY_HOST_KEY))) {
            return stack;
        }
        StackPane host = new StackPane();
        host.getProperties().put(OVERLAY_HOST_KEY, true);
        attachThemeStylesheet(host);
        host.getChildren().add(root);
        scene.setRoot(host);
        return host;
    }

    private static void attachThemeStylesheet(StackPane node) {
        try {
            String css = Objects.requireNonNull(
                    GameVictoryScreen.class.getResource("/ui/game-theme.css")).toExternalForm();
            if (!node.getStylesheets().contains(css)) {
                node.getStylesheets().add(css);
            }
        } catch (Exception ignored) {
        }
    }
}
