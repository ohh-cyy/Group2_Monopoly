package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class House extends ActionCard {

    public House(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    public House(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 3);
    }

    @Override
    public void use(Player player, GameEngine game) {
        addHouseToSet(player, Color.GREEN);
    }

    public boolean addHouseToSet(Player player, Color color) {
        if (!player.hasCompleteSet(color)) {
            return false;
        }
        player.addProperty(new PropertyCard("House+" + color, "House on " + color, color, 3));
        return true;
    }
}
