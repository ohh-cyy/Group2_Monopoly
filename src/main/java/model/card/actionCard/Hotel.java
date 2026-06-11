package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

/**
 * Action card that adds a hotel to a complete property set.
 * Requires an existing house on the set and allows at most one hotel per set.
 * Target selection is handled by the controller via {@link #addHotelToSet}.
 */
public class Hotel extends ActionCard {

    /** Name prefix used to identify hotel improvement cards on a set. */
    private static final String IMPROVEMENT_NAME_PREFIX = "Hotel+";

    /**
     * Creates a Hotel card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public Hotel(String name, String description, CardType type) {
        super(name, description, type, 4);
    }

    /**
     * Creates a Hotel card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public Hotel(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 4);
    }

    /**
     * Empty by design — the controller chooses the target set via UI and
     * calls {@link #addHotelToSet}.
     */
    @Override
    public void use(Player player, GameEngine game) {
    }

    /**
     * 添加一个hotel到完整的property set，需要先有房子，并且只有一套hotel
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

    /**
     * 检查是否已经有hotel
     */
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
