package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class Hotel extends ActionCard {

    private static final String IMPROVEMENT_NAME_PREFIX = "Hotel+";

    public Hotel(String name, String description, CardType type) {
        super(name, description, type, 4);
    }

    public Hotel(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 4);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // The controller chooses the complete set and then calls addHotelToSet.
    }

    /**
     * Adds a hotel to a complete property set. A hotel requires a house first.
     */
    public boolean addHotelToSet(Player player, Color color) {
        if (player == null || color == null || !player.hasCompleteSet(color)) {
            return false;
        }
        if (!hasHouse(player, color) || hasHotel(player, color)) {
            return false;
        }
        player.addProperty(new PropertyCard(IMPROVEMENT_NAME_PREFIX + color, "Hotel on " + color, color, 4));
        return true;
    }

    private boolean hasHouse(Player player, Color color) {
        String expectedName = "House+" + color;
        for (PropertyCard property : player.getPropertiesByColor(color)) {
            if (expectedName.equals(property.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasHotel(Player player, Color color) {
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
