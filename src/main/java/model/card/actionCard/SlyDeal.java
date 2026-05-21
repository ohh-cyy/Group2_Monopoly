package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.player.Player;

import java.util.List;

public class SlyDeal extends ActionCard {

    public SlyDeal(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    @Override
    public void use(Player player, GameEngine game) {
        stealOneProperty(player, game.getDefaultDefender(player));
    }

    public boolean stealOneProperty(Player player, Player target) {
        if (target == null || target.equals(player)) {
            return false;
        }
        List<PropertyCard> props = target.getAllProperties();
        if (props.isEmpty()) {
            return false;
        }
        PropertyCard stolen = props.get(0);
        target.removeProperty(stolen);
        player.addProperty(stolen);
        return true;
    }
}
