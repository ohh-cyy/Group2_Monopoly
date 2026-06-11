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
     * 标记下一张Rent卡为双倍，不能叠加，
     * 如果两次调用之间没有Play Rent卡，则返回false
     */
    public boolean activateForNextRent(GameEngine game) {
        if (game == null || game.isRentDoubled()) {
            return false;
        }
        game.setRentDoubled(true);
        return true;
    }
}
