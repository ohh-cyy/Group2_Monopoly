package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.player.Player;

import java.util.List;

public class ForcedDeal extends ActionCard {

    public ForcedDeal(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    @Override
    public void use(Player player, GameEngine game) {
        swapOneProperty(player, game.getDefaultDefender(player));
    }

    public boolean swapOneProperty(Player player, Player target) {
        if (target == null || target.equals(player)) {
            return false;
        }
        List<PropertyCard> targetProps = target.getAllProperties();
        List<PropertyCard> playerProps = player.getAllProperties();
        if (targetProps.isEmpty() || playerProps.isEmpty()) {
            return false;
        }
        PropertyCard fromTarget = targetProps.get(0);
        PropertyCard fromPlayer = playerProps.get(0);
        target.removeProperty(fromTarget);
        player.removeProperty(fromPlayer);
        player.addProperty(fromTarget);
        target.addProperty(fromPlayer);
        return true;
    }
}
