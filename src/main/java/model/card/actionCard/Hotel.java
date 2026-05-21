package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class Hotel extends ActionCard {

    public Hotel(String name, String description, CardType type) {
        super(name, description, type, 4);
    }

    @Override
    public void use(Player player, GameEngine game) {
        addHotelToSet(player, Color.RED);
    }

    public boolean addHotelToSet(Player player, Color color) {
        if (!player.hasCompleteSet(color)) {
            return false;
        }
        player.addProperty(new PropertyCard("Hotel+" + color, "Hotel on " + color, color, 4));
        return true;
    }
}
