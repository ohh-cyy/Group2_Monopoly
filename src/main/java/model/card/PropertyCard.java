package model.card;

import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

public class PropertyCard extends Card {
    private final Color color;
    private final int price;
    private final int baseRent;
    private Player owner;

    public PropertyCard(String name, String description, Color color, int price, int baseRent) {
        super(name, description, CardType.PROPERTY);
        this.color = color;
        this.price = price;
        this.baseRent = baseRent;
        this.owner = null;
    }

    // 专属Getter/Setter
    public Color getColor() { return color; }
    public int getPrice() { return price; }
    public int getBaseRent() { return baseRent; }
    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }

    @Override
    public void execute(Player player) {
        // 后续action包实现效果逻辑
    }
}