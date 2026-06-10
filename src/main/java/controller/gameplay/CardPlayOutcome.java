package controller.gameplay;

/**
 * Immutable result of attempting to play a card in local mode.
 * <p>
 * Returned by {@link LocalCardPlayService} so {@link controller.GameController}
 * can animate, log, and count plays without re-deriving effect details.
 */
public final class CardPlayOutcome {
    /** Whether the play succeeded, failed, was cancelled, or blocked. */
    public final ActionEffectResult result;
    /** {@code true} when the card was banked instead of using its effect. */
    public final boolean depositedToBank;
    /** {@code true} when a second play must be consumed (e.g. Double the Rent). */
    public final boolean consumesExtraPlay;

    /**
     * @param result            effect resolution status
     * @param depositedToBank   whether the card was banked instead of used
     * @param consumesExtraPlay whether a second play counter should be consumed
     */
    public CardPlayOutcome(ActionEffectResult result, boolean depositedToBank, boolean consumesExtraPlay) {
        this.result = result;
        this.depositedToBank = depositedToBank;
        this.consumesExtraPlay = consumesExtraPlay;
    }

    /** Convenience outcome when the player dismisses a play dialog. */
    public static CardPlayOutcome cancelled() {
        return new CardPlayOutcome(ActionEffectResult.CANCELLED, false, false);
    }
}
