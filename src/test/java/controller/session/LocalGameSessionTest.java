package controller.session;

import engine.GameEngine;
import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalGameSessionTest {

    @Test
    void startNewGameCreatesPlayersAndDealsHands() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob", "Carol"));

        assertEquals(3, session.getPlayers().size());
        assertEquals("Alice", session.getCurrentPlayer().getName());
        for (Player player : session.getPlayers()) {
            assertEquals(GameEngine.START_HAND_SIZE, player.getHandSize());
        }
        assertTrue(session.isActive());
    }

    @Test
    void drawForCurrentPlayerAddsTwoCards() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player current = session.getCurrentPlayer();
        int before = current.getHandSize();

        assertTrue(session.drawForCurrentPlayer());
        assertEquals(before + 2, current.getHandSize());
        assertFalse(session.drawForCurrentPlayer());
    }

    @Test
    void discardFromHandRemovesSelectedCard() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player current = session.getCurrentPlayer();
        Card card = current.getHand().getFirst();

        assertTrue(session.discardFromHand(current, card));
        assertFalse(current.getHand().contains(card));
        assertTrue(session.getEngine().getDiscardPile().size() > 0);
    }

    @Test
    void recordCardPlayedTracksTurnLimit() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        session.drawForCurrentPlayer();

        session.recordCardPlayed();
        session.recordCardPlayed();
        session.recordCardPlayed();

        assertFalse(session.getEngine().canPlayCard());
        assertTrue(session.getEngine().isTurnOver());
    }

    @Test
    void nextTurnSwitchesCurrentPlayer() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player first = session.getCurrentPlayer();

        session.nextTurn();

        assertNotEquals(first, session.getCurrentPlayer());
        assertEquals("Bob", session.getCurrentPlayer().getName());
    }

    @Test
    void checkWinIsFalseUntilThreeCompleteSets() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player player = session.getPlayers().getFirst();

        player.addProperty(new PropertyCard("Brown 1", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Brown 2", "Brown", Color.BROWN, 1));

        assertFalse(session.checkWin(player));
    }

    @Test
    void setGameOverStopsActiveSession() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));

        session.setGameOver(true);

        assertFalse(session.isActive());
        assertTrue(session.getEngine().isGameOver());
    }

    @Test
    void canEndTurnRequiresHandSizeWithinLimit() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player current = session.getCurrentPlayer();

        assertTrue(session.canEndTurn(current));

        while (current.getHandSize() <= GameEngine.MAX_HAND_SIZE) {
            current.draw(new MoneyCard("1M", "Money", 1));
        }
        assertFalse(session.canEndTurn(current));
    }
}
