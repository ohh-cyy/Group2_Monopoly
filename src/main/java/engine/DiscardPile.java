package engine;
import model.card.Card;

import java.util.ArrayList;
import java.util.List;

/**
 * Face-up discard stack for played and discarded cards.
 * Contents are returned to the {@link Deck} when the draw pile runs out.
 */
public class DiscardPile {
    /** Cards in discard order; last element is the top of the pile. */
    private List<Card> cards;

    /** Creates an empty discard pile. */
    public DiscardPile() {
        cards = new ArrayList<>();
    }

    /** Adds one card to the top of the discard pile. */
    public void addCard(Card card) {
        cards.add(card);
    }

    /** Returns the top card without removing it, or {@code null} if empty. */
    public Card peekTop() {

        if (cards.isEmpty()) {
            return null;
        }

        return cards.get(cards.size() - 1);
    }

    /** True when no cards have been discarded yet. */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /** Number of cards currently in the discard pile. */
    public int size() {
        return cards.size();
    }

    /** Prints discard contents to stdout for simple debugging. */
    public void showDiscardPile() {

        System.out.println("===== Discard Pile =====");

        for (Card card : cards) {
            System.out.println(card.getName());
        }
    }

    /** Returns a copy of all discarded cards, usually before reshuffling into the deck. */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    /** Alias for {@link #getCards()}. */
    public List<Card> getAllCards() {
        return getCards();
    }

    /** Removes every card from the pile after they are moved back to the deck. */
    public void clear() {
        cards.clear();
    }

}
