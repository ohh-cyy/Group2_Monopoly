package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.player.Player;

import java.util.List;

public class ForcedDeal extends ActionCard {

    public ForcedDeal(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {
        Player target = game.getDefaultDefender(player);
        List<PropertyCard> targetProperties = target.getAllProperties();
        
        if (!targetProperties.isEmpty()) {
            PropertyCard targetProperty = targetProperties.get(0);
            target.removeProperty(targetProperty);
            player.addProperty(targetProperty);
            
            System.out.println(player.getName() + " used " + getName() +
                " to exchange " + targetProperty.getName() + " with " + target.getName());
        } else {
            System.out.println(target.getName() + " no available property to exchange with");
        }
    }
}
