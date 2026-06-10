package network.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Server-side per-turn timeout scheduling for online games.
 * Exposes {@link #deadlineEpochMillis()} so clients can render a synchronized countdown.
 */
final class SessionTurnClock {
    private final Runnable onTimeout;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "game-session-turn-timer");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledFuture<?> timeoutTask;
    private long deadlineEpochMillis;
    private boolean paused;
    private long frozenRemainingMillis;

    SessionTurnClock(Runnable onTimeout) {
        this.onTimeout = onTimeout;
    }

    /** True while the turn timer is paused and not counting down. */
    boolean isPaused() {
        return paused;
    }

    /** Whole seconds left when paused; {@code 0} when not paused or expired. */
    int frozenSecondsRemaining() {
        if (!paused) {
            return 0;
        }
        return (int) Math.ceil(frozenRemainingMillis / 1000.0);
    }

    /** Epoch millis when the active turn ends; 0 when no timer is running. */
    long deadlineEpochMillis() {
        return deadlineEpochMillis;
    }

    /** Schedules {@link #onTimeout} after {@code turnTimeSeconds} and records the deadline. */
    void start(int turnTimeSeconds) {
        cancel();
        deadlineEpochMillis = System.currentTimeMillis() + turnTimeSeconds * 1000L;
        timeoutTask = executor.schedule(onTimeout, turnTimeSeconds, TimeUnit.SECONDS);
    }

    /** Freezes the countdown until {@link #resume()} is called. */
    void pause() {
        if (paused || timeoutTask == null) {
            return;
        }
        frozenRemainingMillis = Math.max(0, deadlineEpochMillis - System.currentTimeMillis());
        timeoutTask.cancel(false);
        timeoutTask = null;
        paused = true;
    }

    /** Restarts the countdown from the remaining time saved by {@link #pause()}. */
    void resume() {
        if (!paused) {
            return;
        }
        paused = false;
        if (frozenRemainingMillis <= 0) {
            deadlineEpochMillis = 0;
            onTimeout.run();
            return;
        }
        deadlineEpochMillis = System.currentTimeMillis() + frozenRemainingMillis;
        timeoutTask = executor.schedule(onTimeout, frozenRemainingMillis, TimeUnit.MILLISECONDS);
        frozenRemainingMillis = 0;
    }

    void cancel() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
        deadlineEpochMillis = 0;
        paused = false;
        frozenRemainingMillis = 0;
    }

    void shutdown() {
        cancel();
        executor.shutdownNow();
    }
}
