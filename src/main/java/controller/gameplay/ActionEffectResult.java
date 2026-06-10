package controller.gameplay;

/**
 * Outcome of resolving an action card effect in local play.
 * <p>
 * Consumed by {@link CardPlayOutcome} and {@link LocalCardPlayService} to decide
 * logging, discard behavior, and whether an extra play is consumed.
 */
public enum ActionEffectResult {
    /** Effect applied (or partially applied, e.g. rent with some blocks). */
    SUCCESS,
    /** Invalid target or preconditions; card may still be discarded. */
    FAILED,
    /** Player cancelled a dialog; card stays in hand. */
    CANCELLED,
    /** Effect stopped by a Just Say No response chain. */
    BLOCKED
}
