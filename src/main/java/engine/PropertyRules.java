package engine;

import model.card.Card;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Property-set rules: rent, improvements, win detection, and payment eligibility.
 * Complete sets are protected from theft and forced payment.
 */
public final class PropertyRules {
    /** Complete property sets required to win the game. */
    public static final int WINNING_SET_COUNT = 3;
    /** Extra rent (in millions) added when a House improvement is on the set. */
    public static final int HOUSE_RENT_BONUS_M = 3;
    /** Extra rent (in millions) added when a Hotel improvement is on the set. */
    public static final int HOTEL_RENT_BONUS_M = 4;
    /** Name prefix for House improvement cards placed on a complete set. */
    private static final String HOUSE_PREFIX = "House+";
    /** Name prefix for Hotel improvement cards placed on a complete set. */
    private static final String HOTEL_PREFIX = "Hotel+";

    private PropertyRules() {
    }

    /** True when the property is a House or Hotel improvement card, not a billable lot. */
    public static boolean isSetImprovement(PropertyCard property) {
        if (property == null) {
            return false;
        }
        String name = property.getName();
        return name != null && (name.startsWith(HOUSE_PREFIX) || name.startsWith(HOTEL_PREFIX));
    }

    /** {@link #isSetImprovement(PropertyCard)} overload for any {@link Card}. */
    public static boolean isSetImprovement(Card card) {
        return card instanceof PropertyCard property && isSetImprovement(property);
    }

    /** Counts billable (non-improvement) properties the player owns in {@code color}. */
    public static int countBillableProperties(Player player, Color color) {
        if (player == null || color == null) {
            return 0;
        }
        return countBillableProperties(player.getPropertiesByColor(color));
    }

    /** Counts billable properties in a flat card collection for one color group. */
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

    /** True when a House improvement sits on the player's {@code color} set. */
    public static boolean hasHouse(Player player, Color color) {
        return hasImprovement(player, color, HOUSE_PREFIX);
    }

    /** True when a Hotel improvement sits on the player's {@code color} set. */
    public static boolean hasHotel(Player player, Color color) {
        return hasImprovement(player, color, HOTEL_PREFIX);
    }

    /** {@link #hasHouse(Player, Color)} for a pre-filtered card list. */
    public static boolean hasHouse(Collection<? extends Card> cards, Color color) {
        return hasImprovement(cards, color, HOUSE_PREFIX);
    }

    /** {@link #hasHotel(Player, Color)} for a pre-filtered card list. */
    public static boolean hasHotel(Collection<? extends Card> cards, Color color) {
        return hasImprovement(cards, color, HOTEL_PREFIX);
    }

    /** Sum of House and Hotel rent bonuses for the player's {@code color} set. */
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

    /** {@link #getImprovementRentBonus(Player, Color)} for a pre-filtered card list. */
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

    /** Base rent from {@link RentTable} plus House/Hotel bonuses for the player's set. */
    public static int calculateRent(Player player, Color color) {
        int propertyCount = countBillableProperties(player, color);
        if (propertyCount <= 0) {
            return 0;
        }
        return RentTable.getRent(color, propertyCount) + getImprovementRentBonus(player, color);
    }

    /** {@link #calculateRent(Player, Color)} for a flat card list instead of a player board. */
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

    /** Counts how many full property sets the player currently has. */
    public static int countCompleteSets(Player player) {
        if (player == null) {
            return 0;
        }
        int complete = 0;
        for (Color color : Color.values()) {
            if (isCompleteSet(player, color)) {
                complete++;
            }
        }
        return complete;
    }

    /** Returns true when the player owns three complete property sets. */
    public static boolean hasWon(Player player) {
        return countCompleteSets(player) >= WINNING_SET_COUNT;
    }

    /** Counts complete sets from a flat property card list (for board view snapshots). */
    public static int countCompleteSetsFromCards(List<? extends Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return 0;
        }
        Player scratch = new Player("__stats__");
        for (Card card : cards) {
            if (card instanceof PropertyCard property) {
                scratch.addProperty(property);
            }
        }
        return countCompleteSets(scratch);
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

    /** Alias for {@link #getPropertiesOutsideCompleteSets(Player)}. */
    public static List<PropertyCard> getPayableProperties(Player player) {
        return getPropertiesOutsideCompleteSets(player);
    }

    /** True when {@code property} is on the player's board and not in a complete set. */
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
