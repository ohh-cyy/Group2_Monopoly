package ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class EmojiReactionOverlay {
    private final Pane overlay;
    private final VBox playerRows;
    private int reactionSequence;

    public EmojiReactionOverlay(Pane overlay, VBox playerRows) {
        this.overlay = overlay;
        this.playerRows = playerRows;
    }

    public void show(int seat, String emoji) {
        if (overlay == null || emoji == null || emoji.isBlank()) {
            return;
        }
        Platform.runLater(() -> showOnFxThread(seat, emoji));
    }

    private void showOnFxThread(int seat, String emoji) {
        Label bubble = new Label(emoji);
        bubble.getStyleClass().add("emoji-reaction-bubble");
        bubble.setManaged(false);
        bubble.setMouseTransparent(true);
        overlay.getChildren().add(bubble);
        bubble.applyCss();
        bubble.autosize();

        Point2D anchor = resolveAnchor(seat);
        double stagger = (reactionSequence++ % 3) * 18.0;
        double x = clamp(anchor.getX() + 46 + stagger, 8, overlay.getWidth() - bubble.getWidth() - 8);
        double y = clamp(anchor.getY() - bubble.getHeight() / 2, 8, overlay.getHeight() - bubble.getHeight() - 8);
        bubble.relocate(x, y);

        bubble.setOpacity(0);
        bubble.setScaleX(0.45);
        bubble.setScaleY(0.45);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), bubble);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        ScaleTransition pop = new ScaleTransition(Duration.millis(190), bubble);
        pop.setFromX(0.45);
        pop.setFromY(0.45);
        pop.setToX(1.15);
        pop.setToY(1.15);

        PauseTransition hold = new PauseTransition(Duration.millis(850));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(620), bubble);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        TranslateTransition floatUp = new TranslateTransition(Duration.millis(620), bubble);
        floatUp.setByY(-52);
        ScaleTransition settle = new ScaleTransition(Duration.millis(620), bubble);
        settle.setFromX(1.15);
        settle.setFromY(1.15);
        settle.setToX(0.9);
        settle.setToY(0.9);

        SequentialTransition animation = new SequentialTransition(
                new ParallelTransition(fadeIn, pop),
                hold,
                new ParallelTransition(fadeOut, floatUp, settle));
        animation.setOnFinished(event -> overlay.getChildren().remove(bubble));
        animation.play();
    }

    private Point2D resolveAnchor(int seat) {
        if (playerRows != null && seat >= 0 && seat < playerRows.getChildren().size()) {
            Node row = playerRows.getChildren().get(seat);
            Bounds bounds = row.localToScene(row.getBoundsInLocal());
            if (bounds != null) {
                return overlay.sceneToLocal(bounds.getMinX(), bounds.getMinY() + Math.min(30, bounds.getHeight() / 2));
            }
        }
        return new Point2D(Math.max(20, overlay.getWidth() * 0.35), Math.max(20, overlay.getHeight() * 0.35));
    }

    private double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
