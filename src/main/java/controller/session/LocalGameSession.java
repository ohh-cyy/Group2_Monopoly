package controller.session;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import model.card.Card;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Thin facade over {@link GameEngine} for local hot-seat play.
 * <p>
 * Owns player list and engine construction so {@link controller.GameController}
 * does not scatter deck setup and turn mutations across the UI layer.
 */
public final class LocalGameSession {
    private GameEngine gameEngine;
    private List<Player> players = List.of();

    /**
     * 创建玩家，洗牌，开始游戏
     */
    public void startNewGame(List<String> playerNames) {
        players = new ArrayList<>();
        for (String name : playerNames) {
            players.add(new Player(name));
        }
        List<Card> cardList = DeckFactory.createFullDeck();
        Deck deck = new Deck(cardList);
        gameEngine = new GameEngine(players, deck);
        gameEngine.startGame();
    }

    /** Returns the live engine, or {@code null} before {@link #startNewGame}. */
    public GameEngine getEngine() {
        return gameEngine;
    }

    /** Unmodifiable view of seats created at game start. */
    public List<Player> getPlayers() {
        return players;
    }

    /** Current turn holder, or {@code null} if no game is running. */
    public Player getCurrentPlayer() {
        return gameEngine != null ? gameEngine.getCurrentPlayer() : null;
    }

    /** {@code true} when a game exists and has not ended. */
    public boolean isActive() {
        return gameEngine != null && !gameEngine.isGameOver();
    }

    /** Draws two cards for the current player if the turn allows it. */
    public boolean drawForCurrentPlayer() {
        return gameEngine != null && gameEngine.drawCardsForCurrentPlayer();
    }

    /** Removes one card from the given player's hand into the discard pile. */
    public boolean discardFromHand(Player player, Card card) {
        return gameEngine != null && gameEngine.discardFromHand(player, card);
    }

    /** Whether the player may voluntarily end the turn (hand size within limit, etc.). */
    public boolean canEndTurn(Player player) {
        return gameEngine != null && gameEngine.canEndTurn(player);
    }

    /** Advances to the next seat and resets per-turn draw/play counters. */
    public void nextTurn() {
        if (gameEngine != null) {
            gameEngine.nextTurn();
        }
    }

    /** Checks the standard three-complete-sets win condition for the player. */
    public boolean checkWin(Player player) {
        return gameEngine != null && gameEngine.checkWin(player);
    }

    /** Forces or clears the game-over flag on the engine. */
    public void setGameOver(boolean gameOver) {
        if (gameEngine != null) {
            gameEngine.setGameOver(gameOver);
        }
    }

    /** Consumes one of the three plays allowed per turn. */
    public void recordCardPlayed() {
        if (gameEngine != null) {
            gameEngine.recordCardPlayed();
        }
    }
}
