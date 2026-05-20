package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class DoubleTheRent extends ActionCard {

    public DoubleTheRent(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {
        System.out.println(player.getName() + " used " + getName() + "  to double the rent!");
    }
}
