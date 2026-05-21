package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.enums.CardType;
import model.player.Player;

public class PassGoCard extends ActionCard {

    public PassGoCard(String name, String description, CardType type) {
        super(name, description, type, 1);
    }

    @Override
    public void use(Player player, GameEngine game) {
        for (int i = 0; i < 2; i++) {
            if (game.getDeck().isEmpty()) {
                break;
            }
            Card card = game.getDeck().draw();
            player.draw(card);
        }
    }
}
