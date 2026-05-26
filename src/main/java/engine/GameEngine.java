package engine;

import model.card.Card;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.*;

public class GameEngine {
    public static final int START_HAND_SIZE = 5;
    public static final int CARDS_DRAWN_PER_TURN = 2;
    public static final int MAX_PLAYS_PER_TURN = 3;

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

    /** 本回合是否还能抽牌（仅允许抽一次，共 2 张） */
    public boolean canDrawCards() {
        return !gameOver && !hasDrawnThisTurn;
    }

    /** 本回合是否还能出牌（最多 3 张） */
    public boolean canPlayCard() {
        return !gameOver && playsThisTurn < MAX_PLAYS_PER_TURN;
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
     * 当前玩家抽牌：每回合仅可调用一次，一次抽 2 张。
     * @return 是否成功抽牌
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

    /** 记录打出一张牌；满 3 张后 {@link #isTurnOver()} 为 true */
    public void recordCardPlayed() {
        if (playsThisTurn < MAX_PLAYS_PER_TURN) {
            playsThisTurn++;
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
        Map<Color, Integer> colorCount = new HashMap<>();

        for (PropertyCard card : player.getAllProperties()) {
            Color color = card.getColor();
            if (color != null) {
                colorCount.put(color, colorCount.getOrDefault(color, 0) + 1);
            }
        }

        int completeSets = 0;
        for (Map.Entry<Color, Integer> entry : colorCount.entrySet()) {
            if (entry.getValue() >= getRequiredCount(entry.getKey())) {
                completeSets++;
            }
        }

        return completeSets >= 3;
    }

    private int getRequiredCount(Color color) {
        switch (color) {
            case BROWN:
            case DARK_BLUE:
            case LIGHT_GREEN:
                return 2;
            case BLACK:
                return 4;
            default:
                return 3;
        }
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
}
