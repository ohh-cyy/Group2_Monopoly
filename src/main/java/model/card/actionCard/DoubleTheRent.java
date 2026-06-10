package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * Activates the double rent effect for the next Rent card played by the same player.
 */
public class DoubleTheRent extends ActionCard {

    /**
     * Creates a Double the Rent card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public DoubleTheRent(String name, String description, CardType type) {
        super(name, description, type, 1);
    }

    /**
     * Creates a Double the Rent card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public DoubleTheRent(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 1);
    }

    /** Activates the double-rent effect immediately. */
    @Override
    public void use(Player player, GameEngine game) {
        activateForNextRent(game);
    }

    /**
     * Marks the next Rent card as doubled. The effect cannot be stacked —
     * calling this twice before a Rent card is played will return false.
     *
     * @param game game engine holding the double-rent flag
     * @return {@code true} if the double-rent effect was activated
     */
    public boolean activateForNextRent(GameEngine game) {
        if (game == null || game.isRentDoubled()) {
            return false;
        }
        game.setRentDoubled(true);
        return true;
    }
}
