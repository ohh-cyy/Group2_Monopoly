package engine;

import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/** 地产套组相关规则（偷牌、交换等）。 */
public final class PropertyRules {
    private PropertyRules() {
    }

    /** 该颜色的地产是否已凑成完整套组 */
    public static boolean isCompleteSet(Player player, Color color) {
        if (color == null) {
            return false;
        }
        return player.getPropertiesByColor(color).size() >= color.getSetSize();
    }

    /**
     * 可被偷取/用于强制交换的地产：不属于已凑齐的完整套组。
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