package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class JustSayNo extends ActionCard{
    public JustSayNo(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {

    }
}
