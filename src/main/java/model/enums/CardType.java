package model.enums;

/**
 * Discriminator for the three card categories in the deck.
 */
public enum CardType {
    /** Property cards that form color sets and generate rent. */
    PROPERTY,
    /** Money cards placed directly in the bank for payment. */
    MONEY,
    /** Action cards with special effects or bank value. */
    ACTION
}
