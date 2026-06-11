package engine;

import model.card.Card;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Central game-state coordinator for a local match.
 * Manages turn order, draw/play limits, win checks, and discard-pile reshuffle.
 */
public class GameEngine {
    /** Cards dealt to each player at game start. */
    public static final int START_HAND_SIZE = 5;
    /** Cards drawn when the current player takes the mandatory draw action. */
    public static final int CARDS_DRAWN_PER_TURN = 2;
    /** Maximum action/property plays allowed after drawing each turn. */
    public static final int MAX_PLAYS_PER_TURN = 3;
    /** Hand size the current player must reach before ending a turn. */
    public static final int MAX_HAND_SIZE = 7;

    private final List<Player> players;
    private final Deck deck;
    /** Face-up pile reshuffled into the deck when the draw pile is empty. */
    private final DiscardPile discardPile;
    /** Zero-based index of the player whose turn is active. */
    private int currentPlayerIndex;
    /** When true, draw/play/end-turn actions are rejected. */
    private boolean gameOver;
    /** Action/property cards played by the current player this turn. */
    private int playsThisTurn;
    /** Whether the current player has taken the mandatory draw this turn. */
    private boolean hasDrawnThisTurn;
    /** Set when a Double The Rent card is active for the next rent charge. */
    private boolean rentDoubled;

    /** Creates an engine with the given players and shuffled deck; resets the first turn. */
    public GameEngine(List<Player> players, Deck deck) {
        this.players = players;
        this.deck = deck;
        this.currentPlayerIndex = 0;
        this.discardPile = new DiscardPile();
        this.gameOver = false;
        startNewTurn();
    }

    /** 每个人开始抽五张牌，如果deck没牌了就让弃牌堆洗牌然后放到deck */
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

    /** Returns the player whose turn it is. */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /** Zero-based seat index of the current player. */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    /** Resets per-turn counters when a new turn begins. */
    public void startNewTurn() {
        playsThisTurn = 0;
        hasDrawnThisTurn = false;
        rentDoubled = false;
    }

    /** Marks whether the next rent collection should be doubled. */
    public void setRentDoubled(boolean rentDoubled) {
        this.rentDoubled = rentDoubled;
    }

    /** Returns true when a Double The Rent effect is pending. */
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

    /** Whether the current player has already drawn this turn. */
    public boolean hasDrawnThisTurn() {
        return hasDrawnThisTurn;
    }

    /** Number of cards played by the current player this turn. */
    public int getPlaysThisTurn() {
        return playsThisTurn;
    }

    /** Plays still available before the turn auto-ends. */
    public int getRemainingPlays() {
        return Math.max(0, MAX_PLAYS_PER_TURN - playsThisTurn);
    }

    /** True when the current player has used all plays for this turn. */
    public boolean isTurnOver() {
        return playsThisTurn >= MAX_PLAYS_PER_TURN;
    }

    /** Advances to the next seated player and starts a fresh turn. */
    public void nextTurn() {
        if (gameOver) {
            return;
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        startNewTurn();
    }

    /**
     * 每回合抽两张牌
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

    /** 记录一个玩家手里的牌，限制每回合最多出三张 */
    public void recordCardPlayed() {
        recordCardsPlayed(1);
    }

    /** 限制每回合只能出三张 */
    public void recordCardsPlayed(int count) {
        for (int i = 0; i < count; i++) {
            if (playsThisTurn < MAX_PLAYS_PER_TURN) {
                playsThisTurn++;
            }
        }
    }

    // 洗弃牌堆的牌
    private void reshuffleDiscardPile() {
        List<Card> discardedCards = discardPile.getAllCards();
        if (!discardedCards.isEmpty()) {
            deck.reshuffle(discardedCards);
            discardPile.clear();
        }
    }

    /** Delegates win detection to {@link PropertyRules#hasWon(Player)}. */
    public boolean checkWin(Player player) {
        return PropertyRules.hasWon(player);
    }

    /** Stops further turn actions when the match has ended. */
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    /** Whether the match is finished. */
    public boolean isGameOver() {
        return gameOver;
    }

    /** Returns the next seated player after {@code attacker} (default action target). */
    public Player getDefaultDefender(Player attacker) {
        int index = players.indexOf(attacker);
        int defenderIndex = (index + 1) % players.size();
        return players.get(defenderIndex);
    }

    /** All seated players in turn order. */
    public List<Player> getPlayers() {
        return players;
    }

    /** Shared discard pile; reshuffled into the deck when it runs dry. */
    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    /** The draw pile for this match. */
    public Deck getDeck() {
        return deck;
    }

    /** 把一张牌从玩家手里拿出来放到弃牌堆 */
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

    /** 能不能结束游戏，当玩家手里牌少于或等于7张时候可以 */
    public boolean canEndTurn(Player player) {
        return player != null && player.getHandSize() <= MAX_HAND_SIZE;
    }

    /**
     *删除玩家手牌直到小于7
     */
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
