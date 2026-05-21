package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class PropertyCard extends Card {
    private final Color color;
    private final int price;

    public PropertyCard(String name, String description, Color color, int price) {
        super(name, description, CardType.PROPERTY);
        this.color = color;
        this.price = price;
    }

    @Override
    public Color getColor() {
        return color;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public void use(Player player, GameEngine game) {
        player.addProperty(this);
    }
}