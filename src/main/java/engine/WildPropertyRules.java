package engine;

import model.card.PropertyCard;
import model.card.WildpropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Rules for recoloring wild property cards already on the board.
 * Validates target colors against set capacity and rolls back failed moves.
 */
public final class WildPropertyRules {
    private WildPropertyRules() {
    }

    /** True when the wild card can switch between at least two colors. */
    public static boolean isRecolorable(WildpropertyCard wild) {
        return wild != null && wild.getAvailableColors().size() > 1;
    }

    /** Finds the live board copy of {@code wild} owned by {@code player}, matched by instance id. */
    public static WildpropertyCard findOwnedWild(Player player, WildpropertyCard wild) {
        if (player == null || wild == null) {
            return null;
        }
        String id = wild.getInstanceId();
        for (PropertyCard property : player.getAllProperties()) {
            if (property instanceof WildpropertyCard owned && id.equals(owned.getInstanceId())) {
                return owned;
            }
        }
        return null;
    }

    /**
     * Returns colors the wild may switch to without completing an already-full set.
     * Excludes the current color and colors where no billable slot remains.
     */
    public static List<Color> getRecolorOptions(Player player, WildpropertyCard wild) {
        WildpropertyCard owned = findOwnedWild(player, wild);
        if (owned == null || !isRecolorable(owned)) {
            return List.of();
        }
        Color current = owned.getChosenColor();
        if (current == null) {
            return List.of();
        }

        List<Color> options = new ArrayList<>();
        for (Color color : owned.getAvailableColors()) {
            if (color != current && PropertyRules.canAddBillableProperty(player, color)) {
                options.add(color);
            }
        }
        return options;
    }

    /** True when {@code newColor} is a valid recolor target for the owned wild. */
    public static boolean canRecolorTo(Player player, WildpropertyCard wild, Color newColor) {
        return getRecolorOptions(player, wild).contains(newColor);
    }

    /**
     * Moves the owned wild to {@code newColor} on the player's board.
     * Rolls back if the new color group cannot accept the card.
     */
    public static boolean recolor(Player player, WildpropertyCard wild, Color newColor) {
        WildpropertyCard owned = findOwnedWild(player, wild);
        if (owned == null || !canRecolorTo(player, owned, newColor)) {
            return false;
        }
        Color previous = owned.getChosenColor();
        player.removeProperty(owned);
        owned.setChosenColor(newColor);
        if (!player.addProperty(owned)) {
            owned.setChosenColor(previous);
            player.addProperty(owned);
            return false;
        }
        return true;
    }
}
