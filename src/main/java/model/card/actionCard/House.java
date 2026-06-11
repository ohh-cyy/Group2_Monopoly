package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

/**
 * Action card that adds a house to a complete property set.
 * Each set may contain at most one house. Target selection is handled
 * by the controller via {@link #addHouseToSet}.
 */
public class House extends ActionCard {

    /** Name prefix used to identify house improvement cards on a set. */
    private static final String IMPROVEMENT_NAME_PREFIX = "House+";

    /**
     * Creates a House card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public House(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    /**
     * Creates a House card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public House(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 3);
    }

    /**
     * Empty by design — the controller chooses the target set via UI and
     * calls {@link #addHouseToSet}.
     */
    @Override
    public void use(Player player, GameEngine game) {
    }

    /**
     * 添加一个house到完整的property set，只有一套房子可以盖
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

    /**
     * 检查是否已经有房子
     */
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
