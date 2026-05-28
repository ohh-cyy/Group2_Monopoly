package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * A generic action card with no special effect — used solely for its bank
 * value. Useful as a placeholder or for future custom cards.
 */
public class SimpleActionCard extends ActionCard {

    public SimpleActionCard(String name, String description, int bankValueM) {
        super(name, description, CardType.ACTION, bankValueM);
    }

    public SimpleActionCard(String instanceId, String name, String description, CardType type, int bankValueM) {
        super(instanceId, name, description, type, bankValueM);
    }

    /** No effect — this card is only useful as bank value. */
    @Override
    public void use(Player player, GameEngine game) {
    }
}
