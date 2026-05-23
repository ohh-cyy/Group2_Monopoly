package model.card.actionCard;

import engine.GameEngine;
import engine.PropertyRules;
import model.card.PropertyCard;
import model.enums.CardType;
import model.player.Player;

public class SlyDeal extends ActionCard {

    public SlyDeal(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // 由 GameController 选择目标与地产
    }

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
        target.removeProperty(property);
        thief.addProperty(property);
        return true;
    }
}
