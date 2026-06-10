package controller.network;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import network.protocol.GameStateDto;
import ui.render.TurnDeadlineClock;

/**
 * Client-side ticker that keeps the online turn deadline label fresh between server syncs.
 * <p>
 * Uses {@link network.protocol.GameStateDto#turnDeadlineEpochMillis} and delegates
 * label refresh to the {@link Host} (typically {@link controller.NetworkGameController}).
 */
public final class OnlineTurnTimerDisplay {
    /** Seconds remaining when a one-time warning is shown to the local player. */
    public static final int TURN_WARNING_SECONDS = 10;

    /**
     * Callbacks for reading state and updating the UI each second.
     */
    public interface Host {
        /** Latest server snapshot containing the turn deadline. */
        GameStateDto state();

        /** Whether the local seat is the active player. */
        boolean isMyTurn();

        /** Display name of the player whose turn is active. */
        String currentPlayerName();

        /** Repaints turn status labels (invoked every second while running). */
        void refreshTimerLabel();

        /** Called once per turn when the warning threshold is reached on the local player's turn. */
        void onTurnWarning(String playerName);
    }

    private final Host host;
    private Timeline timeline;
    private boolean warningShown;
    private int lastTurnIndex = -1;

    /**
     * @param host callbacks for state reads and label refresh
     */
    public OnlineTurnTimerDisplay(Host host) {
        this.host = host;
    }

    /** Call after each state sync to reset warnings and ensure the ticker is running. */
    public void onStateUpdated() {
        resetWarningIfNewTurn();
        ensureRunning();
        host.refreshTimerLabel();
    }

    /** Stops the one-second JavaFX timeline (e.g. on game over). */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    /** Pauses the display ticker while the game is paused. */
    public void pause() {
        if (timeline != null) {
            timeline.pause();
        }
    }

    /** Resumes a paused display ticker. */
    public void resume() {
        if (timeline != null) {
            timeline.play();
        }
    }

    private void ensureRunning() {
        GameStateDto state = host.state();
        if (state == null || state.gameOver || (!state.gamePaused && state.turnDeadlineEpochMillis <= 0)) {
            stop();
            return;
        }
        if (timeline == null) {
            timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.play();
        }
    }

    private void tick() {
        host.refreshTimerLabel();
        GameStateDto state = host.state();
        if (state == null || state.gameOver) {
            stop();
            return;
        }
        if (state.gamePaused) {
            return;
        }
        if (state.turnDeadlineEpochMillis <= 0) {
            stop();
            return;
        }
        int seconds = TurnDeadlineClock.secondsRemaining(state.turnDeadlineEpochMillis);
        if (host.isMyTurn() && !warningShown && seconds == TURN_WARNING_SECONDS) {
            warningShown = true;
            host.onTurnWarning(host.currentPlayerName());
        }
        if (seconds <= 0) {
            stop();
        }
    }

    private void resetWarningIfNewTurn() {
        GameStateDto state = host.state();
        if (state == null) {
            return;
        }
        if (state.currentPlayerIndex != lastTurnIndex) {
            lastTurnIndex = state.currentPlayerIndex;
            warningShown = false;
        }
    }
}
