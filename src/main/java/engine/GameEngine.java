package engine;

import model.card.Card;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    public static final int START_HAND_SIZE = 5;
    public static final int CARDS_DRAWN_PER_TURN = 2;
    public static final int MAX_PLAYS_PER_TURN = 3;
    public static final int MAX_HAND_SIZE = 7;

    private final List<Player> players;
    private final Deck deck;
    private final DiscardPile discardPile;
    private int currentPlayerIndex;
    private boolean gameOver;
    private int playsThisTurn;
    private boolean hasDrawnThisTurn;
    private boolean rentDoubled;

    public GameEngine(List<Player> players, Deck deck) {
        this.players = players;
        this.deck = deck;
        this.currentPlayerIndex = 0;
        this.discardPile = new DiscardPile();
        this.gameOver = false;
        startNewTurn();
    }

    public void startGame() {
        for (Player p : players) {
            for (int i = 0; i < START_HAND_SIZE; i++) {
                if (deck.isEmpty()) {
                    reshuffleDiscardPile();
                }
                if (!deck.isEmpty()) {
                    p.draw(deck.draw());
                }
            }
        }
        startNewTurn();
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void startNewTurn() {
        playsThisTurn = 0;
        hasDrawnThisTurn = false;
        rentDoubled = false;
    }

    public void setRentDoubled(boolean rentDoubled) {
        this.rentDoubled = rentDoubled;
    }

    public boolean isRentDoubled() {
        return rentDoubled;
    }

    /** Checks if the current player can draw once this turn. */
    public boolean canDrawCards() {
        return !gameOver && !hasDrawnThisTurn;
    }

    /** Checks if the current player may play a card this turn (after drawing). */
    public boolean canPlayCard() {
        return !gameOver && hasDrawnThisTurn && playsThisTurn < MAX_PLAYS_PER_TURN;
    }

    public boolean hasDrawnThisTurn() {
        return hasDrawnThisTurn;
    }

    public int getPlaysThisTurn() {
        return playsThisTurn;
    }

    public int getRemainingPlays() {
        return Math.max(0, MAX_PLAYS_PER_TURN - playsThisTurn);
    }

    public boolean isTurnOver() {
        return playsThisTurn >= MAX_PLAYS_PER_TURN;
    }

    public void nextTurn() {
        if (gameOver) {
            return;
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        startNewTurn();
    }

    /**
     * Draws two cards for the current player once per turn.
     * @return true if the draw action was accepted
     */
    public boolean drawCardsForCurrentPlayer() {
        if (!canDrawCards()) {
            return false;
        }

        Player player = getCurrentPlayer();
        for (int i = 0; i < CARDS_DRAWN_PER_TURN; i++) {
            if (deck.isEmpty()) {
                reshuffleDiscardPile();
            }
            if (deck.isEmpty()) {
                break;
            }
            player.draw(deck.draw());
        }

        hasDrawnThisTurn = true;
        return true;
    }

    /** Records one played card and ends the turn after three plays. */
    public void recordCardPlayed() {
        recordCardsPlayed(1);
    }

    public void recordCardsPlayed(int count) {
        for (int i = 0; i < count; i++) {
            if (playsThisTurn < MAX_PLAYS_PER_TURN) {
                playsThisTurn++;
            }
        }
    }

    private void reshuffleDiscardPile() {
        List<Card> discardedCards = discardPile.getAllCards();
        if (!discardedCards.isEmpty()) {
            deck.reshuffle(discardedCards);
            discardPile.clear();
        }
    }

    public boolean checkWin(Player player) {
        return PropertyRules.hasWon(player);
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Player getDefaultDefender(Player attacker) {
        int index = players.indexOf(attacker);
        int defenderIndex = (index + 1) % players.size();
        return players.get(defenderIndex);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    public Deck getDeck() {
        return deck;
    }
    public boolean discardFromHand(Player player, Card card) {
        if (player == null || card == null) {
            return false;
        }
        if (player.findInHandById(card.getInstanceId()) == null) {
            return false;
        }
        player.removeFromHand(card);
        discardPile.addCard(card);
        return true;
    }

    public boolean canEndTurn(Player player) {
        return player != null && player.getHandSize() <= MAX_HAND_SIZE;
    }

    public List<Card> enforceHandSizeLimit(Player player) {
        List<Card> discardedCards = new ArrayList<>();
        while (player.getHandSize() > MAX_HAND_SIZE) {
            Card cardToDiscard = player.getHand().get(player.getHandSize() - 1);
            player.removeFromHand(cardToDiscard);
            discardPile.addCard(cardToDiscard);
            discardedCards.add(cardToDiscard);
        }
        return discardedCards;
    }
}
