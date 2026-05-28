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

    @Override
    public void use(Player player, GameEngine game) {
        // The controller asks the player to choose a target and then calls useOnTarget.
    }

    /**
     * Steals one complete property set of the selected color from another player.
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
