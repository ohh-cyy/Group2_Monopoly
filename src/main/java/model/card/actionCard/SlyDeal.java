package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class SlyDeal extends ActionCard {
    public SlyDeal(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {

    }
}
