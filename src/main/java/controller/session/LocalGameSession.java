package controller.session;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import model.card.Card;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/** Encapsulates local hot-seat game engine lifecycle and turn operations. */
public final class LocalGameSession {
    private GameEngine gameEngine;
    private List<Player> players = List.of();

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

    public GameEngine getEngine() {
        return gameEngine;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return gameEngine != null ? gameEngine.getCurrentPlayer() : null;
    }

    public boolean isActive() {
        return gameEngine != null && !gameEngine.isGameOver();
    }

    public boolean drawForCurrentPlayer() {
        return gameEngine != null && gameEngine.drawCardsForCurrentPlayer();
    }

    public boolean discardFromHand(Player player, Card card) {
        return gameEngine != null && gameEngine.discardFromHand(player, card);
    }

    public boolean canEndTurn(Player player) {
        return gameEngine != null && gameEngine.canEndTurn(player);
    }

    public void nextTurn() {
        if (gameEngine != null) {
            gameEngine.nextTurn();
        }
    }

    public boolean checkWin(Player player) {
        return gameEngine != null && gameEngine.checkWin(player);
    }

    public void setGameOver(boolean gameOver) {
        if (gameEngine != null) {
            gameEngine.setGameOver(gameOver);
        }
    }

    public void recordCardPlayed() {
        if (gameEngine != null) {
            gameEngine.recordCardPlayed();
        }
    }
}
