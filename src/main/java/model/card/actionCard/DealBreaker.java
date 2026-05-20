package model.card.actionCard;

import engine.GameEngine;
import model.card.Card;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class DealBreaker extends ActionCard {

    public DealBreaker(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {
        for (Player otherPlayer : game.getPlayers()) {
            if (!otherPlayer.equals(player)) {
                for (Color color : Color.values()) {
                    if (otherPlayer.hasCompleteSet(color)) {
                        otherPlayer.removePropertySet(color);
                        System.out.println(player.getName() + " used " + getName() +
                            " Stolen " + otherPlayer.getName() + " 's " + color + " property set!");
                        return;
                    }
                }
            }
        }
        System.out.println("No full property set found.");
    }
}
