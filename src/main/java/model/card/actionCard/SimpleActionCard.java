package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

/**
 * Simple Action Card Implementation
 * Used for basic action cards without special logic
 */
public class SimpleActionCard extends ActionCard {
    
    public SimpleActionCard(String name, String description) {
        super(name, description, CardType.ACTION);
    }
    
    @Override
    public void use(Player player, GameEngine game) {
        // Default behavior: add card to discard pile
        game.getDiscardPile().addCard(this);
        System.out.println(player.getName() + " used action card: " + getName());
        System.out.println("Effect: " + getDescription());
    }
}
