package ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

/** Procedural firework bursts for the victory overlay. */
public final class VictoryFireworksView extends Pane {
    private static final Color[] BURST_COLORS = {
            Color.web("#FFD54F"),
            Color.web("#FFB300"),
            Color.web("#FFECB3"),
            Color.web("#FFA726")
    };

    private Timeline loopTimeline;

    public VictoryFireworksView(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setMouseTransparent(true);
        widthProperty().addListener((obs, oldW, newW) -> restart(newW.doubleValue(), getHeight()));
        heightProperty().addListener((obs, oldH, newH) -> restart(getWidth(), newH.doubleValue()));
        restart(width, height);
    }

    private void restart(double width, double height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (loopTimeline != null) {
            loopTimeline.stop();
        }
        getChildren().clear();
        playCycle(width, height);
        loopTimeline = new Timeline(new javafx.animation.KeyFrame(Duration.seconds(2.4), e -> {
            getChildren().clear();
            playCycle(width, height);
        }));
        loopTimeline.setCycleCount(Timeline.INDEFINITE);
        loopTimeline.play();
    }

    private void playCycle(double width, double height) {
        spawnBurst(width * 0.50, height * 0.42, 0, 0);
        spawnBurst(width * 0.32, height * 0.30, 350, 1);
        spawnBurst(width * 0.68, height * 0.34, 700, 2);
    }

    private void spawnBurst(double centerX, double centerY, long delayMs, int colorIndex) {
        Group burst = buildBurst(colorIndex);
        burst.setLayoutX(centerX);
        burst.setLayoutY(centerY);
        burst.setScaleX(0.15);
        burst.setScaleY(0.15);
        burst.setOpacity(0);
        getChildren().add(burst);

        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(event -> animateBurst(burst));
        pause.play();
    }

    private Group buildBurst(int colorIndex) {
        Group burst = new Group();
        Color main = BURST_COLORS[colorIndex % BURST_COLORS.length];
        Color spark = main.brighter();

        int rays = 18;
        for (int i = 0; i < rays; i++) {
            double angle = 2 * Math.PI * i / rays;
            double length = 34 + (i % 3) * 8;
            Line ray = new Line(0, 0, Math.cos(angle) * length, Math.sin(angle) * length);
            ray.setStroke(main);
            ray.setStrokeWidth(i % 2 == 0 ? 3.2 : 2.2);
            ray.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            burst.getChildren().add(ray);
        }

        for (int i = 0; i < 14; i++) {
            double angle = 2 * Math.PI * i / 14 + 0.15;
            double radius = 18 + (i % 4) * 5;
            Circle dot = new Circle(Math.cos(angle) * radius, Math.sin(angle) * radius, 2.2 + (i % 2), spark);
            burst.getChildren().add(dot);
        }

        Circle core = new Circle(0, 0, 4.5, Color.WHITE);
        burst.getChildren().add(core);
        return burst;
    }

    private void animateBurst(Group burst) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(650), burst);
        scale.setFromX(0.15);
        scale.setFromY(0.15);
        scale.setToX(1.15);
        scale.setToY(1.15);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), burst);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(520), burst);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0.15);
        fadeOut.setDelay(Duration.millis(420));

        ParallelTransition grow = new ParallelTransition(scale, fadeIn);
        grow.setOnFinished(event -> fadeOut.play());
        grow.play();
    }
}
