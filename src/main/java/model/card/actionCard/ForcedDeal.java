package model.card.actionCard;

import engine.GameEngine;
import engine.PropertyRules;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

/**
 * Action card that swaps one property between the current player and an opponent.
 * Neither side may give up a property from a complete set.
 * Property selection is handled by the controller via {@link #swapProperties}.
 */
public class ForcedDeal extends ActionCard {

    /**
     * Creates a Forced Deal card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public ForcedDeal(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    /**
     * Creates a Forced Deal card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public ForcedDeal(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 3);
    }

    /**
     * Empty by design — the controller prompts both players to pick
     * properties, then calls {@link #swapProperties}.
     */
    @Override
    public void use(Player player, GameEngine game) {
    }

    /**
     * 交换一张property卡
     */
    public boolean swapProperties(Player player, PropertyCard playerGives,
                                  Player target, PropertyCard targetGives) {
        if (player == null || target == null || player.equals(target)
                || playerGives == null || targetGives == null) {
            return false;
        }
        if (!player.getAllProperties().contains(playerGives)) {
            return false;
        }
        if (!target.getAllProperties().contains(targetGives)) {
            return false;
        }
        Color playerGivesColor = playerGives.getColor();
        if (playerGivesColor != null && PropertyRules.isCompleteSet(player, playerGivesColor)) {
            return false;
        }
        if (PropertyRules.isCompleteSet(target, targetGives.getColor())) {
            return false;
        }
        Color playerReceives = targetGives.getColor();
        Color targetReceives = playerGives.getColor();
        if (playerReceives != null && !PropertyRules.canAddBillableProperty(player, playerReceives)) {
            return false;
        }
        if (targetReceives != null && !PropertyRules.canAddBillableProperty(target, targetReceives)) {
            return false;
        }

        player.removeProperty(playerGives);
        target.removeProperty(targetGives);
        if (!player.addProperty(targetGives) || !target.addProperty(playerGives)) {
            player.addProperty(playerGives);
            target.addProperty(targetGives);
            return false;
        }
        return true;
    }
}
