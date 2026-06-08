package controller.gameplay;

/** Result of attempting to play a card in local mode. */
public final class CardPlayOutcome {
    public final ActionEffectResult result;
    public final boolean depositedToBank;
    public final boolean consumesExtraPlay;

    public CardPlayOutcome(ActionEffectResult result, boolean depositedToBank, boolean consumesExtraPlay) {
        this.result = result;
        this.depositedToBank = depositedToBank;
        this.consumesExtraPlay = consumesExtraPlay;
    }

    public static CardPlayOutcome cancelled() {
        return new CardPlayOutcome(ActionEffectResult.CANCELLED, false, false);
    }
}
