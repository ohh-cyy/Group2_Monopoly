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

    SessionTurnClock(Runnable onTimeout) {
        this.onTimeout = onTimeout;
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

    void cancel() {
        if (timeoutTask != null) {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
        deadlineEpochMillis = 0;
    }

    void shutdown() {
        cancel();
        executor.shutdownNow();
    }
}
