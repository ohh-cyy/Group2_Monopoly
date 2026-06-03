package engine;

import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Rules for property sets, stealing, and swapping. */
public final class PropertyRules {
    private PropertyRules() {
    }

    /** Checks whether this color has a complete property set. */
    public static boolean isCompleteSet(Player player, Color color) {
        if (color == null) {
            return false;
        }
        return player.getPropertiesByColor(color).size() >= color.getSetSize();
    }

    /**
     * Returns properties that can be stolen or swapped.
     * Cards in complete sets are protected.
     */
    public static List<PropertyCard> getPropertiesOutsideCompleteSets(Player player) {
        List<PropertyCard> result = new ArrayList<>();
        for (PropertyCard property : player.getAllProperties()) {
            Color color = property.getColor();
            if (color == null || !isCompleteSet(player, color)) {
                result.add(property);
            }
        }
        return result;
    }
}
