package ui.render;

/**
 * Converts a server-provided turn deadline into whole seconds remaining.
 * <p>
 * Used by online controllers to display countdown text on the board header.
 */
public final class TurnDeadlineClock {
    private TurnDeadlineClock() {
    }

    /**
     * Returns seconds left, 0 if expired, or -1 when no timer is active.
     *
     * @param turnDeadlineEpochMillis server turn deadline in epoch milliseconds, or {@code <= 0} if none
     * @return whole seconds remaining, {@code 0} when expired, or {@code -1} when inactive
     */
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
