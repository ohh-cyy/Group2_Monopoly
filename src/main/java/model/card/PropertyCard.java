package model.card;

import engine.GameEngine;
import engine.RentTable;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class PropertyCard extends Card implements PayableAsset {
    private final Color color;
    private final int price;

    public PropertyCard(String name, String description, Color color, int price) {
        super(name, description, CardType.PROPERTY);
        this.color = color;
        this.price = price;
    }

    public PropertyCard(String instanceId, String name, String description, Color color, int price) {
        super(instanceId, name, description, CardType.PROPERTY);
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
    public int getPaymentValueM() {
        return Math.max(1, price);
    }

    /** Rent tiers shown on the card, such as "1M / 2M / 3M". */
    public String getRentDisplay() {
        return RentTable.formatRentTiers(color);
    }

    @Override
    public void use(Player player, GameEngine game) {
        player.addProperty(this);
    }
}
