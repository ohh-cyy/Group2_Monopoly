package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * A generic action card with no special effect — used solely for its bank
 * value. Useful as a placeholder or for future custom cards.
 */
public class SimpleActionCard extends ActionCard {

    /**
     * Creates a simple action card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param bankValueM  bank deposit value in millions (M)
     */
    public SimpleActionCard(String name, String description, int bankValueM) {
        super(name, description, CardType.ACTION, bankValueM);
    }

    /**
     * Creates a simple action card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (typically {@link CardType#ACTION})
     * @param bankValueM  bank deposit value in millions (M)
     */
    public SimpleActionCard(String instanceId, String name, String description, CardType type, int bankValueM) {
        super(instanceId, name, description, type, bankValueM);
    }

    /** No effect — this card is only useful as bank value. */
    @Override
    public void use(Player player, GameEngine game) {
    }
}
