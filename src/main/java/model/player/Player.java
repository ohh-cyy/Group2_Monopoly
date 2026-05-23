package model.player;

import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.WildpropertyCard;
import model.card.actionCard.ActionCard;
import model.enums.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private String name;
    private List<Card> hand;
    private Map<Object, List<PropertyCard>> properties;
    private List<Card> bank;
    private int money;

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.properties = new HashMap<Object, List<PropertyCard>>();
        this.bank = new ArrayList<>();
        this.money = 0;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public void draw(Card card) {
        if (card != null) {
            hand.add(card);
        }
    }

    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    public void removeFromHand(Card card) {
        hand.remove(card);
    }

    public List<PropertyCard> getPropertiesByColor(Color color) {
        return properties.getOrDefault(color, new ArrayList<>());
    }

    public void addProperty(PropertyCard card) {
        if (card == null) {
            return;
        }

        Color color = card.getColor();
        if (color == null) {
            color = Color.BROWN;
        }

        properties.putIfAbsent(color, new ArrayList<>());
        properties.get(color).add(card);
    }

    public void addBank(Card card) {
        if (card != null) {
            bank.add(card);
        }
    }

    public List<Card> getBank() {
        return new ArrayList<>(bank);
    }

    public int getBankTotalValue() {
        int total = money;
        for (Card card : bank) {
            if (card instanceof MoneyCard moneyCard) {
                total += moneyCard.getMoney();
            } else if (card instanceof ActionCard actionCard) {
                total += actionCard.getBankValueM();
            } else if (card instanceof WildpropertyCard wildCard) {
                total += wildCard.getBankValueM();
            }
        }
        return total;
    }

    public void removeFromBank(Card card) {
        bank.remove(card);
    }

    public void removeProperty(PropertyCard card) {
        if(card == null) return;

        Color color = card.getColor();

        if(properties.containsKey(color)) {

            properties.get(color).remove(card);
        }
    }

    /** 该颜色地产是否已凑齐完整套组 */
    public boolean hasCompleteSet(Color color) {
        if (color == null) {
            return false;
        }
        return getPropertiesByColor(color).size() >= color.getSetSize();
    }

    //use for dealbreaker to remove one player's set
    public List<PropertyCard> removePropertySet(Color color) {
        List<PropertyCard> set =
                new ArrayList<>(getPropertiesByColor(color));
        properties.remove(color);
        return set;
    }


    public int getMoney() {
        return money;
    }

    public void addMoney(int amount) {
        this.money += amount;
    }

    public void removeMoney(int amount) {
        this.money = Math.max(0, this.money - amount);
    }

    public boolean hasEnoughMoney(int amount) {
        return this.money >= amount;
    }

    public int getHandSize() {
        return hand.size();
    }

    public int getPropertyCount() {
        return properties.size();
    }

    public List<PropertyCard> getAllProperties() {
        List<PropertyCard> allProperties = new ArrayList<>();
        for (List<PropertyCard> propertyList : properties.values()) {
            allProperties.addAll(propertyList);
        }
        return allProperties;
    }
}
