package model.player;

import engine.PropertyRules;
import model.card.Card;
import model.card.PayableAsset;
import model.card.PropertyCard;
import model.enums.Color;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a player in a Monopoly Deal game.
 * Tracks hand cards, property sets grouped by color, bank assets, and cash.
 */
public class Player {
    /** Display name shown in the UI and game log. */
    private String name;
    /** Cards currently held and available to play. */
    private List<Card> hand;
    /** Property cards grouped by color; each color maps to one or more cards. */
    private final Map<Color, List<PropertyCard>> properties;
    /** Action and money cards deposited in the bank as payment assets. */
    private List<Card> bank;
    /** Cash balance in millions (M). */
    private int money;

    /**
     * Creates a new player with the given display name and empty collections.
     *
     * @param name player name
     */
    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.properties = new EnumMap<>(Color.class);
        this.bank = new ArrayList<>();
        this.money = 0;
    }

    /** @return the player's display name */
    public String getName() {
        return name;
    }

    /** @return the player's display name (same as {@link #getName()}) */
    @Override
    public String toString() {
        return name;
    }

    /**
     * 添加手牌
     * 如果卡片不为空，则添加到手牌中
     */
    public void draw(Card card) {
        if (card != null) {
            hand.add(card);
        }
    }

    /** @return a defensive copy of the hand */
    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    /**
     * Removes a card from the hand by reference equality.
     *
     * @param card card to remove
     */
    public void removeFromHand(Card card) {
        hand.remove(card);
    }

    /**
     * Returns all property cards of the given color.
     *
     * @param color property color
     * @return list of properties for that color, or empty if none
     */
    public List<PropertyCard> getPropertiesByColor(Color color) {
        return properties.getOrDefault(color, new ArrayList<>());
    }

    /**
     * 添加property卡
     * 如果颜色组已满，且卡片不是set improvement，则返回false
     *
     * @param card property to add
     * @return {@code true} if the property was added
     */
    public boolean addProperty(PropertyCard card) {
        if (card == null) {
            return false;
        }

        Color color = card.getColor();
        if (color == null) {
            color = Color.BROWN;
        }

        if (!PropertyRules.isSetImprovement(card) && !PropertyRules.canAddBillableProperty(this, color)) {
            return false;
        }

        properties.putIfAbsent(color, new ArrayList<>());
        properties.get(color).add(card);
        return true;
    }

    /**
     * 添加到银行
     */
    public void addBank(Card card) {
        if (card != null) {
            bank.add(card);
        }
    }

    /** @return a defensive copy of bank cards */
    public List<Card> getBank() {
        return new ArrayList<>(bank);
    }

    /**
     * 计算银行总价值
     */
    public int getBankTotalValue() {
        int total = money;
        for (Card card : bank) {
            if (card instanceof PayableAsset payableAsset) {
                total += payableAsset.getPaymentValueM();
            }
        }
        return total;
    }

    /**
     * Removes a card from the bank by reference equality.
     *
     * @param card card to remove
     */
    public void removeFromBank(Card card) {
        bank.remove(card);
    }

    /**
     * Removes a single property card from its color group.
     *
     * @param card property to remove; ignored if {@code null}
     */
    public void removeProperty(PropertyCard card) {
        if(card == null) return;

        Color color = card.getColor();

        if(properties.containsKey(color)) {

            properties.get(color).remove(card);
        }
    }

    /** 检查是否拥有完整的property set */
    public boolean hasCompleteSet(Color color) {
        return PropertyRules.isCompleteSet(this, color);
    }

    /**
     * 移除并返回完整的property set
     */
    public List<PropertyCard> removePropertySet(Color color) {
        List<PropertyCard> set =
                new ArrayList<>(getPropertiesByColor(color));
        properties.remove(color);
        return set;
    }

    /** @return current cash balance in millions (M) */
    public int getMoney() {
        return money;
    }

    /**
     * Increases cash balance by the given amount.
     *
     * @param amount amount to add in millions (M)
     */
    public void addMoney(int amount) {
        this.money += amount;
    }

    /**
     * Decreases cash balance, floored at zero.
     *
     * @param amount amount to subtract in millions (M)
     */
    public void removeMoney(int amount) {
        this.money = Math.max(0, this.money - amount);
    }

    /**
     * @param amount required amount in millions (M)
     * @return {@code true} if cash balance is at least the required amount
     */
    public boolean hasEnoughMoney(int amount) {
        return this.money >= amount;
    }

    /**
     * 查找手牌，通过实例ID
     */
    public Card findInHandById(String instanceId) {
        if (instanceId == null) {
            return null;
        }
        for (Card card : hand) {
            if (instanceId.equals(card.getInstanceId())) {
                return card;
            }
        }
        return null;
    }

    /**
     * 移除手牌，通过实例ID
     */
    public boolean removeFromHandById(String instanceId) {
        Card card = findInHandById(instanceId);
        if (card == null) {
            return false;
        }
        hand.remove(card);
        return true;
    }

    /** @return number of cards in hand */
    public int getHandSize() {
        return hand.size();
    }

    /** @return number of distinct color groups that contain at least one property */
    public int getPropertyCount() {
        return properties.size();
    }

    /** @return a flat list of all property cards across every color */
    public List<PropertyCard> getAllProperties() {
        List<PropertyCard> allProperties = new ArrayList<>();
        for (List<PropertyCard> propertyList : properties.values()) {
            allProperties.addAll(propertyList);
        }
        return allProperties;
    }
}
