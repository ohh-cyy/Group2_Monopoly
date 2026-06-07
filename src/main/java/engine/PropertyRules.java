package engine;

import model.card.Card;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Rules for property sets, stealing, and swapping. */
public final class PropertyRules {
    public static final int HOUSE_RENT_BONUS_M = 3;
    public static final int HOTEL_RENT_BONUS_M = 4;
    private static final String HOUSE_PREFIX = "House+";
    private static final String HOTEL_PREFIX = "Hotel+";

    private PropertyRules() {
    }

    public static boolean isSetImprovement(PropertyCard property) {
        if (property == null) {
            return false;
        }
        String name = property.getName();
        return name != null && (name.startsWith(HOUSE_PREFIX) || name.startsWith(HOTEL_PREFIX));
    }

    public static boolean isSetImprovement(Card card) {
        return card instanceof PropertyCard property && isSetImprovement(property);
    }

    public static int countBillableProperties(Player player, Color color) {
        if (player == null || color == null) {
            return 0;
        }
        return countBillableProperties(player.getPropertiesByColor(color));
    }

    public static int countBillableProperties(Collection<? extends Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Card card : cards) {
            if (card instanceof PropertyCard property && !isSetImprovement(property)) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasHouse(Player player, Color color) {
        return hasImprovement(player, color, HOUSE_PREFIX);
    }

    public static boolean hasHotel(Player player, Color color) {
        return hasImprovement(player, color, HOTEL_PREFIX);
    }

    public static boolean hasHouse(Collection<? extends Card> cards, Color color) {
        return hasImprovement(cards, color, HOUSE_PREFIX);
    }

    public static boolean hasHotel(Collection<? extends Card> cards, Color color) {
        return hasImprovement(cards, color, HOTEL_PREFIX);
    }

    public static int getImprovementRentBonus(Player player, Color color) {
        int bonus = 0;
        if (hasHouse(player, color)) {
            bonus += HOUSE_RENT_BONUS_M;
        }
        if (hasHotel(player, color)) {
            bonus += HOTEL_RENT_BONUS_M;
        }
        return bonus;
    }

    public static int getImprovementRentBonus(Collection<? extends Card> cards, Color color) {
        int bonus = 0;
        if (hasHouse(cards, color)) {
            bonus += HOUSE_RENT_BONUS_M;
        }
        if (hasHotel(cards, color)) {
            bonus += HOTEL_RENT_BONUS_M;
        }
        return bonus;
    }

    public static int calculateRent(Player player, Color color) {
        int propertyCount = countBillableProperties(player, color);
        if (propertyCount <= 0) {
            return 0;
        }
        return RentTable.getRent(color, propertyCount) + getImprovementRentBonus(player, color);
    }

    public static int calculateRent(Color color, Collection<? extends Card> cards) {
        int propertyCount = countBillableProperties(cards);
        if (propertyCount <= 0) {
            return 0;
        }
        return RentTable.getRent(color, propertyCount) + getImprovementRentBonus(cards, color);
    }

    /** Checks whether this color has a complete property set. */
    public static boolean isCompleteSet(Player player, Color color) {
        if (color == null) {
            return false;
        }
        return countBillableProperties(player, color) >= color.getSetSize();
    }

    /** Whether another billable property can be added to this color (false once the set is complete). */
    public static boolean canAddBillableProperty(Player player, Color color) {
        if (player == null || color == null) {
            return false;
        }
        return !isCompleteSet(player, color);
    }

    /**
     * Returns properties that can be stolen, swapped, or taken as rent payment.
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

    public static List<PropertyCard> getPayableProperties(Player player) {
        return getPropertiesOutsideCompleteSets(player);
    }

    public static boolean canPayWithProperty(Player player, PropertyCard property) {
        if (player == null || property == null) {
            return false;
        }
        return getPayableProperties(player).contains(property);
    }

    private static boolean hasImprovement(Player player, Color color, String prefix) {
        if (player == null || color == null || prefix == null) {
            return false;
        }
        return hasImprovement(player.getPropertiesByColor(color), color, prefix);
    }

    private static boolean hasImprovement(Collection<? extends Card> cards, Color color, String prefix) {
        if (cards == null || color == null || prefix == null) {
            return false;
        }
        String expectedName = prefix + color;
        for (Card card : cards) {
            if (card instanceof PropertyCard property && expectedName.equals(property.getName())) {
                return true;
            }
        }
        return false;
    }
}
