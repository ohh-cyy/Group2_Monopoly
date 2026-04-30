package engine;

import model.card.Card;

import java.util.*;

public class Deck {
    private Stack<Card> cards = new Stack<>();

    public Deck(List<Card> cardList) {
        Collections.shuffle(cardList);
        cards.addAll(cardList);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            throw new RuntimeException("Deck is empty!");
        }
        return cards.pop();
    }
}