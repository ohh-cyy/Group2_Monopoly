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

    // Required for the checkWin method in GameEngine
    @Override
    public Color getColor() {
        return color;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public void use(Player player, GameEngine game) {
        System.out.println(player.getName() + " obtained property card: " + getName());
        System.out.println("Tile color: " + color + " | Price: " + price);

        // Use the existing addProperty method of the player
        player.addProperty(this);
    }
}