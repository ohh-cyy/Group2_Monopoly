package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.enums.CardType;
import model.player.Player;

/**
 * Action card that draws extra cards from the deck when played.
 * Draws {@link #CARDS_TO_DRAW} cards, stopping early if the deck is empty.
 */
public class PassGoCard extends ActionCard {

    /** Number of extra cards to draw. */
    public static final int CARDS_TO_DRAW = 2;

    /**
     * Creates a Pass Go card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public PassGoCard(String name, String description, CardType type) {
        super(name, description, type, 1);
    }

    /**
     * Creates a Pass Go card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public PassGoCard(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 1);
    }

    /** 多抽两张卡 */
    @Override
    public void use(Player player, GameEngine game) {
        if (player == null || game == null) {
            return;
        }
        for (int i = 0; i < CARDS_TO_DRAW; i++) {
            if (game.getDeck().isEmpty()) {
                break;
            }
            Card card = game.getDeck().draw();
            player.draw(card);
        }
    }
}
