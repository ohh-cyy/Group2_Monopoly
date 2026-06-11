package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.util.UUID;

/**
 * Abstract base class for all cards in the deck.
 * Each card has a unique instance id, display metadata, and a type discriminator.
 * Subclasses implement {@link #use} to define play behavior.
 */
public abstract class Card {
    /** Unique identifier for this physical card instance in the current game. */
    private final String instanceId;
    /** Display name shown on the card face. */
    protected final String name;
    /** Short rules text describing the card effect. */
    protected final String description;
    /** Category discriminator ({@link CardType#PROPERTY}, {@link CardType#MONEY}, or {@link CardType#ACTION}). */
    protected final CardType type;

    /**
     * Creates a card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category
     */
    public Card(String name, String description, CardType type) {
        this(UUID.randomUUID().toString(), name, description, type);
    }

    /**
     * Creates a card with an explicit instance id (used when restoring saved state).
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category
     */
    public Card(String instanceId, String name, String description, CardType type) {
        this.instanceId = instanceId;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    /** @return unique identifier for this card instance */
    public String getInstanceId() {
        return instanceId;
    }

    /** @return display name */
    public String getName() { return name; }

    /** @return rules description text */
    public String getDescription() { return description; }

    /** @return card category discriminator */
    public CardType getType() { return type; }

    /**
     * Returns the property color for color-bearing cards.
     * Non-property cards return {@code null}.
     *
     * @return property color, or {@code null} if not applicable
     */
    public Color getColor(){
        return null;
    }

    /**
     * 执行卡片效果
     *
     */
    public abstract void use(Player player, GameEngine game);
}
