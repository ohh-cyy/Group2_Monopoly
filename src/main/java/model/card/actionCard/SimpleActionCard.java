package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class SimpleActionCard extends ActionCard {

    public SimpleActionCard(String name, String description, int bankValueM) {
        super(name, description, CardType.ACTION, bankValueM);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // 无额外效果
    }
}
