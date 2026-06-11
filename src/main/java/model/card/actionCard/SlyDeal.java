package model.card.actionCard;

import engine.GameEngine;
import engine.PropertyRules;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

/**
 * Action card that steals a single property from another player.
 * Properties belonging to a complete set are protected from theft.
 * Target selection is handled by the controller via {@link #stealProperty}.
 */
public class SlyDeal extends ActionCard {

    /**
     * Creates a Sly Deal card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public SlyDeal(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    /**
     * Creates a Sly Deal card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public SlyDeal(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 3);
    }

    /**
     * Empty by design — the controller picks the target and property via UI,
     * then calls {@link #stealProperty}.
     */
    @Override
    public void use(Player player, GameEngine game) {
    }

    /**
     * 从一个人身上偷一张property卡，完整的套不能偷，也不能偷自己的卡
     */
    public boolean stealProperty(Player thief, Player target, PropertyCard property) {
        if (thief == null || target == null || property == null || thief.equals(target)) {
            return false;
        }
        if (!target.getAllProperties().contains(property)) {
            return false;
        }
        if (PropertyRules.isCompleteSet(target, property.getColor())) {
            return false;
        }
        Color color = property.getColor();
        if (color != null && !PropertyRules.canAddBillableProperty(thief, color)) {
            return false;
        }
        target.removeProperty(property);
        if (!thief.addProperty(property)) {
            target.addProperty(property);
            return false;
        }
        return true;
    }
}
