package ui.render;

/** Computes remaining turn time from a server-provided deadline. */
public final class TurnDeadlineClock {
    private TurnDeadlineClock() {
    }

    /** Returns seconds left, 0 if expired, or -1 when no timer is active. */
    public static int secondsRemaining(long turnDeadlineEpochMillis) {
        if (turnDeadlineEpochMillis <= 0) {
            return -1;
        }
        long millisLeft = turnDeadlineEpochMillis - System.currentTimeMillis();
        if (millisLeft <= 0) {
            return 0;
        }
        return (int) ((millisLeft + 999) / 1000);
    }
}
