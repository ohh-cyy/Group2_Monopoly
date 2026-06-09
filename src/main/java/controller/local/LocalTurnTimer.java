package controller.local;

import engine.GameEngine;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import model.player.Player;

/** Local hot-seat turn countdown and timeout handling. */
public final class LocalTurnTimer {
    public static final int TURN_TIME_SECONDS = 60;
    public static final int TURN_WARNING_SECONDS = 10;

    public interface Host {
        GameEngine engine();

        void onTimerTick(int secondsRemaining);

        void onTurnWarning(Player currentPlayer);

        void onTurnTimedOut(Player skipped);
    }

    private final Host host;
    private Timeline timeline;
    private int secondsRemaining = TURN_TIME_SECONDS;
    private boolean warningShown;

    public LocalTurnTimer(Host host) {
        this.host = host;
    }

    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    public void reset() {
        stop();
        GameEngine engine = host.engine();
        if (engine == null || engine.isGameOver()) {
            secondsRemaining = 0;
            return;
        }
        secondsRemaining = TURN_TIME_SECONDS;
        warningShown = false;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void tick() {
        GameEngine engine = host.engine();
        if (engine == null || engine.isGameOver()) {
            stop();
            return;
        }
        secondsRemaining = Math.max(0, secondsRemaining - 1);
        if (!warningShown && secondsRemaining == TURN_WARNING_SECONDS) {
            warningShown = true;
            host.onTurnWarning(engine.getCurrentPlayer());
        }
        host.onTimerTick(secondsRemaining);
        if (secondsRemaining == 0) {
            stop();
            host.onTurnTimedOut(engine.getCurrentPlayer());
        }
    }
}
