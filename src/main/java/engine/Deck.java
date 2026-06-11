package engine;

import model.card.Card;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Shuffled draw pile backed by a stack ({@link ArrayDeque}).
 * Cards are drawn from the top; empty draws require reshuffling the discard pile.
 */
public class Deck {
    /** Draw stack; top of stack is the next card dealt. */
    private final Deque<Card> cards = new ArrayDeque<>();

    /** Builds a deck from {@code cardList} and shuffles before dealing. */
    public Deck(List<Card> cardList) {
        shuffleAndAdd(cardList);
    }

    /** 抽牌 */
    public Card draw() {
        if (cards.isEmpty()) {
            throw new RuntimeException("Deck is empty! Need to reshuffle from discard pile.");
        }
        return cards.pop();
    }

    /** 洗牌然后放到deck */
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

    /** True when no cards remain to draw. */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /** Number of cards still in the draw pile. */
    public int size() {
        return cards.size();
    }
}
