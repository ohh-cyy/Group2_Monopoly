package ui.animation;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.card.Card;
import model.card.PropertyCard;
import ui.CardView;

/**
 * Animates a card flying from the hand dock toward its play destination.
 * <p>
 * Uses a snapshot ghost image that fades and scales while translating toward
 * the bank, property board, or a fallback target node.
 */
public final class PlayCardFlyAnimation {
    /**
     * Animates a ghost image from {@code source} toward the appropriate target node.
     *
     * @param handDock          hand dock whose parent pane hosts the flying ghost image
     * @param source            card view to snapshot and hide during the animation
     * @param played            card being played; used to pick bank vs property target
     * @param depositedToBank   when {@code true}, animates toward the bank target
     * @param bankTarget        destination node for bank deposits
     * @param propertyTarget    destination node for property plays
     * @param fallbackTarget    fallback destination when a specific target is unavailable
     * @param onFinished        callback run after the animation completes or is skipped
     */
    public void play(VBox handDock,
                     CardView source,
                     Card played,
                     boolean depositedToBank,
                     Node bankTarget,
                     Node propertyTarget,
                     Node fallbackTarget,
                     Runnable onFinished) {
        if (source == null || source.getScene() == null || handDock == null) {
            onFinished.run();
            return;
        }

        Parent parent = handDock.getParent();
        if (!(parent instanceof Pane overlay)) {
            onFinished.run();
            return;
        }

        Node target = targetNodeForPlayedCard(played, depositedToBank, bankTarget, propertyTarget, fallbackTarget);
        if (target == null || target.getScene() == null) {
            onFinished.run();
            return;
        }

        Bounds sourceBounds = source.localToScene(source.getBoundsInLocal());
        Bounds targetBounds = target.localToScene(target.getBoundsInLocal());
        Point2D start = overlay.sceneToLocal(sourceBounds.getMinX(), sourceBounds.getMinY());
        Point2D targetPoint = overlay.sceneToLocal(
                targetBounds.getMinX() + targetBounds.getWidth() / 2,
                targetBounds.getMinY() + targetBounds.getHeight() / 2);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        WritableImage snapshot = source.snapshot(params, null);
        ImageView ghost = new ImageView(snapshot);
        ghost.setFitWidth(sourceBounds.getWidth());
        ghost.setFitHeight(sourceBounds.getHeight());
        ghost.setPreserveRatio(false);
        ghost.setMouseTransparent(true);
        ghost.setManaged(false);
        ghost.setLayoutX(start.getX());
        ghost.setLayoutY(start.getY());

        source.setVisible(false);
        overlay.getChildren().add(ghost);
        ghost.toFront();

        double targetX = targetPoint.getX() - start.getX() - sourceBounds.getWidth() / 2;
        double targetY = targetPoint.getY() - start.getY() - sourceBounds.getHeight() / 2;

        TranslateTransition move = new TranslateTransition(Duration.millis(420), ghost);
        move.setByX(targetX);
        move.setByY(targetY);
        move.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(Duration.millis(420), ghost);
        scale.setToX(depositedToBank ? 0.45 : 0.65);
        scale.setToY(depositedToBank ? 0.45 : 0.65);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        RotateTransition rotate = new RotateTransition(Duration.millis(420), ghost);
        rotate.setByAngle(depositedToBank ? -10 : 12);
        rotate.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fade = new FadeTransition(Duration.millis(420), ghost);
        fade.setFromValue(0.98);
        fade.setToValue(0.12);

        ParallelTransition fly = new ParallelTransition(move, scale, rotate, fade);
        fly.setOnFinished(e -> {
            overlay.getChildren().remove(ghost);
            onFinished.run();
        });
        fly.play();
    }

    private static Node targetNodeForPlayedCard(Card played,
                                                boolean depositedToBank,
                                                Node bankTarget,
                                                Node propertyTarget,
                                                Node fallbackTarget) {
        if (depositedToBank) {
            return bankTarget != null ? bankTarget : fallbackTarget;
        }
        if (played instanceof PropertyCard) {
            return propertyTarget != null ? propertyTarget : fallbackTarget;
        }
        return fallbackTarget != null ? fallbackTarget : propertyTarget;
    }
}
