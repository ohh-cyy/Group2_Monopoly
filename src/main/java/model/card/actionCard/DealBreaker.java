package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.util.List;

public class DealBreaker extends ActionCard {

    public DealBreaker(String name, String description, CardType type) {
        super(name, description, type, 5);
    }

    public DealBreaker(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 5);
    }

    /**
     * Empty by design — the controller prompts the player to pick a target
     * and color, then calls {@link #useOnTarget}.
     */
    @Override
    public void use(Player player, GameEngine game) {
    }

    /**
     * Steals one complete property set of the selected color from another player.
     * The target must own a full set of that color.
     *
     * @return true if the full set was moved successfully
     */
    public boolean useOnTarget(Player player, Player target, Color color) {
        if (player == null || target == null || color == null || player.equals(target)) {
            return false;
        }
        if (!target.hasCompleteSet(color)) {
            return false;
        }

        List<PropertyCard> stolen = target.removePropertySet(color);
        if (stolen.isEmpty()) {
            return false;
        }

        for (PropertyCard property : stolen) {
            player.addProperty(property);
        }
        return true;
    }
}
