package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * Activates the double rent effect for the next Rent card played by the same player.
 */
public class DoubleTheRent extends ActionCard {

    public DoubleTheRent(String name, String description, CardType type) {
        super(name, description, type, 1);
    }

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
     */
    public boolean activateForNextRent(GameEngine game) {
        if (game == null || game.isRentDoubled()) {
            return false;
        }
        game.setRentDoubled(true);
        return true;
    }
}
