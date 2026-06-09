package controller.network;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import network.protocol.GameStateDto;
import ui.render.TurnDeadlineClock;

/** Keeps the online turn countdown label updating between server syncs. */
public final class OnlineTurnTimerDisplay {
    public static final int TURN_WARNING_SECONDS = 10;

    public interface Host {
        GameStateDto state();

        boolean isMyTurn();

        String currentPlayerName();

        void refreshTimerLabel();

        void onTurnWarning(String playerName);
    }

    private final Host host;
    private Timeline timeline;
    private boolean warningShown;
    private int lastTurnIndex = -1;

    public OnlineTurnTimerDisplay(Host host) {
        this.host = host;
    }

    public void onStateUpdated() {
        resetWarningIfNewTurn();
        ensureRunning();
        host.refreshTimerLabel();
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }

    private void ensureRunning() {
        GameStateDto state = host.state();
        if (state == null || state.gameOver || state.turnDeadlineEpochMillis <= 0) {
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
        if (state == null || state.gameOver || state.turnDeadlineEpochMillis <= 0) {
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
