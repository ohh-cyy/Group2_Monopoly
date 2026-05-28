package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * Response card used to cancel an action card played against the player.
 * Can only be played as a reaction, never proactively during one's own turn.
 * The actual response prompt is handled by the controller.
 */
public class JustSayNo extends ActionCard {

    public JustSayNo(String name, String description, CardType type) {
        super(name, description, type, 4);
    }

    public JustSayNo(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 4);
    }

    /**
     * This card is only playable as a defensive response. The controller
     * detects it in the defender's hand and prompts the defender directly;
     * this method is intentionally a no-op for proactive use.
     */
    @Override
    public void use(Player player, GameEngine game) {
    }
}
