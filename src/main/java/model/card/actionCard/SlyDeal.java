package model.card.actionCard;

import engine.GameEngine;
import engine.PropertyRules;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class SlyDeal extends ActionCard {

    public SlyDeal(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

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
     * Steals one property from another player. Properties that are part of a
     * complete set are protected and cannot be stolen.
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
