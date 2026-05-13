package engine;

import model.card.Card;
import java.util.*;

public class Deck {
    private Stack<Card> cards = new Stack<>();

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