package ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
        Platform.runLater(() -> showOverlay(anchor, winnerName));
    }

    private static void showOverlay(Node anchor, String winnerName) {
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
        content.setMaxWidth(520);

        VictoryFireworksView fireworks = new VictoryFireworksView(360, 300);
        Label winnerLabel = new Label(name + " 赢得本局");
        winnerLabel.getStyleClass().add("victory-overlay-winner");
        Label hintLabel = new Label("点击任意处关闭");
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

        overlay.setOnMouseClicked(event -> dismissOverlay(host, overlay));
    }

    private static void dismissOverlay(StackPane host, StackPane overlay) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(280), overlay);
        fadeOut.setFromValue(overlay.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> host.getChildren().remove(overlay));
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
