package engine;

import model.card.Card;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class Deck {
    private final Deque<Card> cards = new ArrayDeque<>();

    public Deck(List<Card> cardList) {
        shuffleAndAdd(cardList);
    }

    public Card draw() {
        if (cards.isEmpty()) {
            throw new RuntimeException("Deck is empty! Need to reshuffle from discard pile.");
        }
        return cards.pop();
    }

    public void reshuffle(List<Card> cardsToShuffle) {
        if (cardsToShuffle != null && !cardsToShuffle.isEmpty()) {
            shuffleAndAdd(cardsToShuffle);
        }
    }

    private void shuffleAndAdd(List<Card> cardList) {
        List<Card> shuffled = new ArrayList<>(cardList);
        Collections.shuffle(shuffled);
        cards.addAll(shuffled);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int size() {
        return cards.size();
    }
}
