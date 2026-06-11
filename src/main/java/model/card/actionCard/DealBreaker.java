package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.util.List;

/**
 * Action card that steals an entire complete property set from another player.
 * Target selection is handled by the controller via {@link #useOnTarget}.
 */
public class DealBreaker extends ActionCard {

    /**
     * Creates a Deal Breaker card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public DealBreaker(String name, String description, CardType type) {
        super(name, description, type, 5);
    }

    /**
     * Creates a Deal Breaker card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
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
     * @param player  player performing the steal
     * @param target  opponent to steal from
     * @param color   color of the complete set to take
     * @param game    game engine whose discard pile receives overflow properties
     * @return {@code true} if the full set was moved successfully
     */
    public boolean useOnTarget(Player player, Player target, Color color, GameEngine game) {
        if (player == null || target == null || color == null || player.equals(target) || game == null) {
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
            if (!player.addProperty(property)) {
                game.getDiscardPile().addCard(property);
            }
        }
        return true;
    }
}
