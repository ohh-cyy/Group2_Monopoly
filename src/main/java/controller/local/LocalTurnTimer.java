package controller.local;

import engine.GameEngine;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import model.player.Player;

/**
 * Per-turn countdown for local hot-seat play.
 * <p>
 * Fires a warning at {@link #TURN_WARNING_SECONDS} and delegates timeout handling
 * to the {@link Host} so the controller can skip the current player and advance.
 */
public final class LocalTurnTimer {
    /** Total seconds allowed per turn before auto-skip. */
    public static final int TURN_TIME_SECONDS = 60;
    /** Seconds remaining when a one-time warning is shown. */
    public static final int TURN_WARNING_SECONDS = 10;

    /**
     * Callbacks invoked by the timer; typically implemented by {@link controller.GameController}.
     */
    public interface Host {
        /** Engine used to detect game-over and identify the current player. */
        GameEngine engine();

        /** Called every second so the UI can refresh the countdown label. */
        void onTimerTick(int secondsRemaining);

        /** Called once when {@link #TURN_WARNING_SECONDS} is reached. */
        void onTurnWarning(Player currentPlayer);

        /** Called when the countdown hits zero; host should skip the player. */
        void onTurnTimedOut(Player skipped);
    }

    private final Host host;
    private Timeline timeline;
    private int secondsRemaining = TURN_TIME_SECONDS;
    private boolean warningShown;

    /**
     * @param host callbacks for ticks, warnings, and timeout handling
     */
    public LocalTurnTimer(Host host) {
        this.host = host;
    }

    /** Seconds left in the current turn (0 after timeout or game over). */
    public int getSecondsRemaining() {
        return secondsRemaining;
    }

    /** Restarts the countdown for a new turn; stops if the game has ended. */
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

    /** Stops the JavaFX timeline without changing {@link #secondsRemaining}. */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    /** Pauses the countdown while the game is paused; no-op if not running. */
    public void pause() {
        if (timeline != null) {
            timeline.pause();
        }
    }

    /** Resumes a paused countdown; no-op if not running. */
    public void resume() {
        if (timeline != null) {
            timeline.play();
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
