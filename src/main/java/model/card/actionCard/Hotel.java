package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class Hotel extends ActionCard{
    public Hotel(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {

    }
}
