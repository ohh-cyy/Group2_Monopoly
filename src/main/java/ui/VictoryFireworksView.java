package ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Rich procedural firework bursts with glow, curved trails, and multi-color sparks. */
public final class VictoryFireworksView extends Pane {
    private static final Random RANDOM = new Random();

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final List<Burst> bursts = new ArrayList<>();
    private AnimationTimer timer;
    private double spawnClock;
    private double width;
    private double height;

    public VictoryFireworksView(double width, double height) {
        this.width = width;
        this.height = height;
        canvas = new Canvas(width, height);
        gc = canvas.getGraphicsContext2D();
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        setMouseTransparent(true);
        getChildren().add(canvas);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && timer != null) {
                timer.stop();
            }
        });

        widthProperty().addListener((obs, oldW, newW) -> updateCanvasSize(newW.doubleValue(), getHeight()));
        heightProperty().addListener((obs, oldH, newH) -> updateCanvasSize(getWidth(), newH.doubleValue()));
        startAnimation();
    }

    private void updateCanvasSize(double newWidth, double newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            return;
        }
        width = newWidth;
        height = newHeight;
        canvas.setWidth(newWidth);
        canvas.setHeight(newHeight);
    }

    private void startAnimation() {
        if (timer != null) {
            timer.stop();
        }
        bursts.clear();
        spawnClock = 0;
        scheduleInitialBursts();

        timer = new AnimationTimer() {
            private long lastNanos;

            @Override
            public void handle(long now) {
                if (lastNanos == 0) {
                    lastNanos = now;
                    return;
                }
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                tick(dt);
            }
        };
        timer.start();
    }

    private void scheduleInitialBursts() {
        spawnBurst(width * 0.34, height * 0.36, BurstTheme.GOLD);
        spawnBurst(width * 0.66, height * 0.32, BurstTheme.MAGENTA);
    }

    private void tick(double dt) {
        spawnClock += dt;
        if (spawnClock >= 2.8) {
            spawnClock = 0;
            bursts.clear();
            scheduleInitialBursts();
        }

        Iterator<Burst> iterator = bursts.iterator();
        while (iterator.hasNext()) {
            Burst burst = iterator.next();
            burst.age += dt;
            if (burst.age > burst.duration + 0.35) {
                iterator.remove();
            }
        }

        gc.clearRect(0, 0, width, height);
        for (Burst burst : bursts) {
            drawBurst(burst);
        }
    }

    private void spawnBurst(double centerX, double centerY, BurstTheme theme) {
        Burst burst = new Burst(centerX, centerY, theme);
        burst.buildParticles(RANDOM);
        bursts.add(burst);
    }

    private void drawBurst(Burst burst) {
        double t = burst.progress();
        if (t <= 0) {
            return;
        }

        double fade = burst.fade();
        drawCoreFlash(burst, t, fade);

        for (Particle particle : burst.particles) {
            drawParticleTrail(particle, burst, t, fade);
        }
        for (Spark spark : burst.sparks) {
            drawSpark(spark, burst, t, fade);
        }
    }

    private void drawCoreFlash(Burst burst, double t, double fade) {
        if (t > 0.55) {
            return;
        }
        double flash = (1.0 - t / 0.55) * fade;
        gc.setFill(Color.rgb(255, 255, 255, flash * 0.95));
        gc.fillOval(burst.x - 8, burst.y - 8, 16, 16);
        gc.setFill(Color.rgb(255, 240, 180, flash * 0.55));
        gc.fillOval(burst.x - 18, burst.y - 18, 36, 36);
    }

    private void drawParticleTrail(Particle particle, Burst burst, double t, double fade) {
        double eased = easeOut(Math.min(1.0, t * 1.05));
        double length = particle.maxLength * eased;
        if (length < 2) {
            return;
        }

        double endX = burst.x + Math.cos(particle.angle) * length;
        double endY = burst.y + Math.sin(particle.angle) * length
                + particle.curve * Math.sin(Math.PI * eased);
        double midX = burst.x + Math.cos(particle.angle) * length * 0.55
                + Math.cos(particle.angle + Math.PI / 2) * particle.curve * 0.35;
        double midY = burst.y + Math.sin(particle.angle) * length * 0.55
                + Math.sin(particle.angle + Math.PI / 2) * particle.curve * 0.35
                + particle.curve * 0.2;

        Color glow = particle.color.deriveColor(0, 1, 1, fade * 0.22);
        Color body = particle.color.deriveColor(0, 1, 1, fade * 0.75);
        Color tip = particle.color.brighter().deriveColor(0, 1, 1, fade);

        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setStroke(glow);
        gc.setLineWidth(particle.width + 7);
        gc.strokePolyline(
                new double[]{burst.x, midX, endX},
                new double[]{burst.y, midY, endY},
                3);

        gc.setStroke(body);
        gc.setLineWidth(particle.width + 2.5);
        gc.strokePolyline(
                new double[]{burst.x, midX, endX},
                new double[]{burst.y, midY, endY},
                3);

        gc.setStroke(tip);
        gc.setLineWidth(Math.max(1.2, particle.width));
        gc.strokePolyline(
                new double[]{burst.x, midX, endX},
                new double[]{burst.y, midY, endY},
                3);

        if (eased > 0.72) {
            gc.setFill(tip);
            gc.fillOval(endX - 2.2, endY - 2.2, 4.4, 4.4);
        }
    }

    private void drawSpark(Spark spark, Burst burst, double t, double fade) {
        double eased = easeOut(Math.min(1.0, (t - 0.08) * 1.1));
        if (eased <= 0) {
            return;
        }
        double radius = spark.distance * eased;
        double x = burst.x + Math.cos(spark.angle) * radius;
        double y = burst.y + Math.sin(spark.angle) * radius + spark.drift * Math.sin(Math.PI * eased);
        double alpha = fade * (1.0 - eased * 0.55) * 0.9;

        gc.setFill(spark.color.deriveColor(0, 1, 1, alpha * 0.35));
        gc.fillOval(x - spark.size * 2.2, y - spark.size * 2.2, spark.size * 4.4, spark.size * 4.4);
        gc.setFill(spark.color.deriveColor(0, 1, 1, alpha));
        gc.fillOval(x - spark.size, y - spark.size, spark.size * 2, spark.size * 2);
    }

    private static double easeOut(double t) {
        return 1.0 - Math.pow(1.0 - t, 2.8);
    }

    private enum BurstTheme {
        GOLD(
                Color.web("#FFE066"),
                Color.web("#FFB703"),
                Color.web("#FFD43B"),
                Color.web("#74C0FC"),
                Color.web("#FF6B9D"),
                Color.web("#FFA94D")
        ),
        MAGENTA(
                Color.web("#FF6B9D"),
                Color.web("#F06595"),
                Color.web("#FF922B"),
                Color.web("#FFD43B"),
                Color.web("#4DABF7"),
                Color.web("#B197FC")
        );

        private final Color[] palette;

        BurstTheme(Color... palette) {
            this.palette = palette;
        }

        Color pick(Random random) {
            return palette[random.nextInt(palette.length)];
        }
    }

    private static final class Burst {
        private final double x;
        private final double y;
        private final BurstTheme theme;
        private final double duration = 1.35;
        private final List<Particle> particles = new ArrayList<>();
        private final List<Spark> sparks = new ArrayList<>();
        private double age;

        private Burst(double x, double y, BurstTheme theme) {
            this.x = x;
            this.y = y;
            this.theme = theme;
        }

        private void buildParticles(Random random) {
            int trails = 34 + random.nextInt(8);
            for (int i = 0; i < trails; i++) {
                Particle particle = new Particle();
                particle.angle = (Math.PI * 2 * i / trails) + random.nextDouble() * 0.18 - 0.09;
                particle.maxLength = 72 + random.nextDouble() * 58;
                particle.width = 1.4 + random.nextDouble() * 1.8;
                particle.curve = (random.nextDouble() - 0.5) * 26;
                particle.color = theme.pick(random);
                particles.add(particle);
            }

            int sparkCount = 26 + random.nextInt(10);
            for (int i = 0; i < sparkCount; i++) {
                Spark spark = new Spark();
                spark.angle = random.nextDouble() * Math.PI * 2;
                spark.distance = 28 + random.nextDouble() * 72;
                spark.size = 1.0 + random.nextDouble() * 2.0;
                spark.drift = (random.nextDouble() - 0.5) * 18;
                spark.color = theme.pick(random);
                sparks.add(spark);
            }
        }

        private double progress() {
            return Math.min(1.0, age / duration);
        }

        private double fade() {
            if (age <= duration * 0.72) {
                return 1.0;
            }
            return Math.max(0, 1.0 - (age - duration * 0.72) / (duration * 0.48 + 0.35));
        }
    }

    private static final class Particle {
        private double angle;
        private double maxLength;
        private double width;
        private double curve;
        private Color color;
    }

    private static final class Spark {
        private double angle;
        private double distance;
        private double size;
        private double drift;
        private Color color;
    }
}
