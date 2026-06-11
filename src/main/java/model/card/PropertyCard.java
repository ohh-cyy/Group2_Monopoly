package model.card;

import engine.GameEngine;
import engine.RentTable;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

/**
 * A property card belonging to a color group.
 * When played, it is added to the player's property table for that color.
 */
public class PropertyCard extends Card implements PayableAsset {
    /** Color group this property belongs to. */
    private final Color color;
    /** Purchase price shown on the card, also used as minimum payment value. */
    private final int price;

    /**
     * Creates a property card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param color       property color group
     * @param price       purchase price in millions (M)
     */
    public PropertyCard(String name, String description, Color color, int price) {
        super(name, description, CardType.PROPERTY);
        this.color = color;
        this.price = price;
    }

    /**
     * Creates a property card with an explicit instance id.
     */
    public PropertyCard(String instanceId, String name, String description, Color color, int price) {
        super(instanceId, name, description, CardType.PROPERTY);
        this.color = color;
        this.price = price;
    }

    /** @return the color group this property belongs to */
    @Override
    public Color getColor() {
        return color;
    }

    /** @return purchase price in millions (M) */
    public int getPrice() {
        return price;
    }

    /** @return payment value in millions (M), at least 1M based on purchase price */
    @Override
    public int getPaymentValueM() {
        return Math.max(1, price);
    }

    /** 获取租金显示 */
    public String getRentDisplay() {
        return RentTable.formatRentTiers(color);
    }

    /** 添加到玩家property表 */
    @Override
    public void use(Player player, GameEngine game) {
        player.addProperty(this);
    }
}
