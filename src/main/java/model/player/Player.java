package model.player;

import model.card.Card;
import model.card.PropertyCard;
import model.card.RentCard;
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
        if(card == null) return;

        Color color = card.getColor();

        properties.putIfAbsent(color, new ArrayList<>());

        properties.get(color).add(card);
    }

    public void addBank(Card card) {
        if (card != null) {
            bank.add(card);
        }
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

    // use for dealbreaker to judge whether this player has complete set
    public boolean hasCompleteSet(Color color) {
        int size =
                getPropertiesByColor(color).size();

        switch(color) {
            case BROWN:
            case DARK_BLUE:
            case WHITE:
                return size>=2;
            case BLACK:
                return size>=4;
            default:
                return size>=3;
        }
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
}
