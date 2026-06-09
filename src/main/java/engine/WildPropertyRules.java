package engine;

import model.card.PropertyCard;
import model.card.WildpropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Rules for recoloring wild property cards already on the board. */
public final class WildPropertyRules {
    private WildPropertyRules() {
    }

    public static boolean isRecolorable(WildpropertyCard wild) {
        return wild != null && wild.getAvailableColors().size() > 1;
    }

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

    public static boolean canRecolorTo(Player player, WildpropertyCard wild, Color newColor) {
        return getRecolorOptions(player, wild).contains(newColor);
    }

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
