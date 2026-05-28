package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * Response card used to cancel an action card played against the player.
 * The actual response choice is handled by the controller.
 */
public class JustSayNo extends ActionCard {

    public JustSayNo(String name, String description, CardType type) {
        super(name, description, type, 4);
    }

    public JustSayNo(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 4);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // This card is played as a response, so the controller handles the effect.
    }
}
