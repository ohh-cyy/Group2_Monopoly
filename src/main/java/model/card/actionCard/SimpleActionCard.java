package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class SimpleActionCard extends ActionCard {

    public SimpleActionCard(String name, String description, int bankValueM) {
        super(name, description, CardType.ACTION, bankValueM);
    }

    public SimpleActionCard(String instanceId, String name, String description, CardType type, int bankValueM) {
        super(instanceId, name, description, type, bankValueM);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // This card has no special effect.
    }
}
