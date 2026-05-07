package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class ActionCard extends Card {

    public ActionCard(String name, String description) {
        super(name, description, CardType.ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {
        System.out.println(player.getName() + " used action card: " + getName());
        System.out.println("Card effect: " + getDescription());

        // Common Monopoly action logic (direct engine calls)
        // 1. Draw an extra card
        // player.draw(game.getDeck().draw());
        // 2. Skip the opponent's turn
        // game.skipNextPlayer();
        // 3. Obtain a property for free
    }
}