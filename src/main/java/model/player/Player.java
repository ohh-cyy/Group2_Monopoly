package model.player;

import model.card.Card;
import model.card.PropertyCard;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private List<Card> hand;
    private List<PropertyCard> properties;

    public Player(String name) {
        this.name = name;
        this.hand = new ArrayList<>();
        this.properties = new ArrayList<>();
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
}
