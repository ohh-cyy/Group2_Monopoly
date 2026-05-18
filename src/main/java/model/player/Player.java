package model.player;

import model.card.Card;
import model.card.PropertyCard;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private List<Card> hand;
    private List<PropertyCard> properties;
    private List<Card> bank;
    private int money;

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.properties = new ArrayList<>();
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

    public List<PropertyCard> getProperties() {
        return new ArrayList<>(properties);
    }

    public void addProperty(PropertyCard card) {
        if (card != null) {
            properties.add(card);
        }
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
        properties.remove(card);
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
