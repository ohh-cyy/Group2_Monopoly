package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.enums.CardType;
import model.player.Player;

public class PassGoCard extends ActionCard {

    /** Number of extra cards to draw. */
    public static final int CARDS_TO_DRAW = 2;

    public PassGoCard(String name, String description, CardType type) {
        super(name, description, type, 1);
    }

    public PassGoCard(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 1);
    }

    /** Draws 2 extra cards from the deck. Stops early if the deck runs out. */
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
