package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.enums.CardType;
import model.player.Player;

public abstract class ActionCard extends Card {
    public ActionCard(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }
    @Override
    public abstract void use(Player player, GameEngine game);

}
