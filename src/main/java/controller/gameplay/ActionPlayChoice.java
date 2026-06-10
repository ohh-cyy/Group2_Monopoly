package controller.gameplay;

/**
 * Player decision when playing an action or bankable wild property card.
 * <p>
 * Presented by {@link StandardCardPlayPrompts} before effect resolution or banking.
 */
public enum ActionPlayChoice {
    /** Execute the card's game effect (rent, steal, etc.). */
    USE_EFFECT,
    /** Skip the effect and deposit the card into the player's bank for its M value. */
    DEPOSIT_BANK
}
