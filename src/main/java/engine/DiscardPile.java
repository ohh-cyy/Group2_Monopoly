package engine;
import model.card.Card;

import java.util.ArrayList;
import java.util.List;


public class DiscardPile {
    private List<Card> cards;

    public DiscardPile() {
        cards = new ArrayList<>();
    }

    // Adds one card to the discard pile.
    public void addCard(Card card) {
        cards.add(card);
    }

    // Returns the top card without removing it.
    public Card peekTop() {

        if (cards.isEmpty()) {
            return null;
        }

        return cards.get(cards.size() - 1);
    }

    // Checks whether the discard pile is empty.
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    // Returns the number of discarded cards.
    public int size() {
        return cards.size();
    }

    // Prints the discard pile for simple debugging.
    public void showDiscardPile() {

        System.out.println("===== Discard Pile =====");

        for (Card card : cards) {
            System.out.println(card.getName());
        }
    }

    // Returns all discarded cards, usually before shuffling them into the deck.
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public List<Card> getAllCards() {
        return getCards();
    }

    public void clear() {
        cards.clear();
    }

}
