package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class House extends ActionCard {

    private static final String IMPROVEMENT_NAME_PREFIX = "House+";

    public House(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    public House(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 3);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // The controller chooses the complete set and then calls addHouseToSet.
    }

    /**
     * Adds a house to a complete property set. A set can only have one house.
     */
    public boolean addHouseToSet(Player player, Color color) {
        if (player == null || color == null || !player.hasCompleteSet(color)) {
            return false;
        }
        if (hasHouse(player, color)) {
            return false;
        }
        player.addProperty(new PropertyCard(IMPROVEMENT_NAME_PREFIX + color, "House on " + color, color, 3));
        return true;
    }

    public boolean hasHouse(Player player, Color color) {
        if (player == null || color == null) {
            return false;
        }
        String expectedName = IMPROVEMENT_NAME_PREFIX + color;
        for (PropertyCard property : player.getPropertiesByColor(color)) {
            if (expectedName.equals(property.getName())) {
                return true;
            }
        }
        return false;
    }
}
