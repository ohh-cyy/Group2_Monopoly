package engine;

import model.card.Card;
import java.util.ArrayList;
import java.util.List;

public class DiscardPile {
    private List<Card> discardedCards;

    public DiscardPile() {
        this.discardedCards = new ArrayList<>();
    }

    public void addCard(Card card) {
        discardedCards.add(card);
    }

    public Card getTopCard() {
        if (discardedCards.isEmpty()) {
            return null;
        }
        return discardedCards.get(discardedCards.size() - 1);
    }

    public int size() {
        return discardedCards.size();
    }

    public boolean isEmpty() {
        return discardedCards.isEmpty();
    }

    public List<Card> getAllCards() {
        return new ArrayList<>(discardedCards);
    }

    public void clear() {
        discardedCards.clear();
    }
}
