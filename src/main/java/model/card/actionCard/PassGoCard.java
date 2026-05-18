package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.enums.CardType;
import model.player.Player;

public class PassGoCard extends ActionCard{


    public PassGoCard(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {
        for (int i = 0; i < 2; i++) {
            Card card = game.getDeck().draw();
            player.draw(card);
        }
    }
}
