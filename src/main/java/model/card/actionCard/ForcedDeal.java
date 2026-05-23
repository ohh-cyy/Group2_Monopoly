package model.card.actionCard;

import engine.GameEngine;
import engine.PropertyRules;
import model.card.PropertyCard;
import model.enums.CardType;
import model.player.Player;

public class ForcedDeal extends ActionCard {

    public ForcedDeal(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    @Override
    public void use(Player player, GameEngine game) {
        // 由 GameController 选择双方地产并交换
    }

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
        if (PropertyRules.isCompleteSet(target, targetGives.getColor())) {
            return false;
        }

        player.removeProperty(playerGives);
        target.removeProperty(targetGives);
        player.addProperty(targetGives);
        target.addProperty(playerGives);
        return true;
    }
}
