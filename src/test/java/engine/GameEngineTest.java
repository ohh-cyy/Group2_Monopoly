package engine;

import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    @Test
    void startGameDealsFiveCardsToEachPlayer() {
        Player first = new Player("Player 1");
        Player second = new Player("Player 2");
        GameEngine game = new GameEngine(List.of(first, second), createDeck(20));

        game.startGame();

        assertEquals(GameEngine.START_HAND_SIZE, first.getHandSize());
        assertEquals(GameEngine.START_HAND_SIZE, second.getHandSize());
        assertEquals(10, game.getDeck().size());
    }

    @Test
    void currentPlayerCanOnlyDrawOnceEachTurn() {
        Player first = new Player("Player 1");
        Player second = new Player("Player 2");
        GameEngine game = new GameEngine(List.of(first, second), createDeck(10));

        assertTrue(game.drawCardsForCurrentPlayer());
        assertEquals(2, first.getHandSize());
        assertFalse(game.drawCardsForCurrentPlayer());
        assertEquals(2, first.getHandSize());
    }

    @Test
    void nextTurnChangesCurrentPlayerAndResetsTurnState() {
        Player first = new Player("Player 1");
        Player second = new Player("Player 2");
        GameEngine game = new GameEngine(List.of(first, second), createDeck(10));

        game.drawCardsForCurrentPlayer();
        game.recordCardPlayed();
        game.nextTurn();

        assertEquals(second, game.getCurrentPlayer());
        assertFalse(game.hasDrawnThisTurn());
        assertEquals(0, game.getPlaysThisTurn());
    }

    @Test
    void playerCannotPlayBeforeDrawing() {
        GameEngine game = new GameEngine(List.of(new Player("Player 1"), new Player("Player 2")), createDeck(10));

        assertFalse(game.canPlayCard());
        assertTrue(game.drawCardsForCurrentPlayer());
        assertTrue(game.canPlayCard());
    }

    @Test
    void playerCannotPlayMoreThanThreeCardsInOneTurn() {
        GameEngine game = new GameEngine(List.of(new Player("Player 1"), new Player("Player 2")), createDeck(10));
        game.drawCardsForCurrentPlayer();

        assertTrue(game.canPlayCard());
        game.recordCardPlayed();
        game.recordCardPlayed();
        game.recordCardPlayed();

        assertEquals(0, game.getRemainingPlays());
        assertTrue(game.isTurnOver());
        assertFalse(game.canPlayCard());
    }

    @Test
    void checkWinReturnsTrueWhenPlayerHasThreeCompleteSets() {
        Player player = new Player("Winner");
        Player opponent = new Player("Opponent");
        GameEngine game = new GameEngine(List.of(player, opponent), createDeck(10));

        player.addProperty(new PropertyCard("Brown 1", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Brown 2", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Blue 1", "Dark blue", Color.DARK_BLUE, 4));
        player.addProperty(new PropertyCard("Blue 2", "Dark blue", Color.DARK_BLUE, 4));
        player.addProperty(new PropertyCard("Red 1", "Red", Color.RED, 3));
        player.addProperty(new PropertyCard("Red 2", "Red", Color.RED, 3));
        player.addProperty(new PropertyCard("Red 3", "Red", Color.RED, 3));

        assertTrue(game.checkWin(player));
    }

    @Test
    void cannotEndTurnWithMoreThanSevenCards() {
        Player player = new Player("Player 1");
        player.draw(new MoneyCard("1M", "Money", 1));
        player.draw(new MoneyCard("2M", "Money", 1));
        player.draw(new MoneyCard("3M", "Money", 1));
        player.draw(new MoneyCard("4M", "Money", 1));
        player.draw(new MoneyCard("5M", "Money", 1));
        player.draw(new MoneyCard("6M", "Money", 1));
        player.draw(new MoneyCard("7M", "Money", 1));
        player.draw(new MoneyCard("8M", "Money", 1));
        GameEngine game = new GameEngine(List.of(player, new Player("Player 2")), createDeck(10));

        assertFalse(game.canEndTurn(player));
        assertTrue(game.discardFromHand(player, player.getHand().get(0)));
        assertTrue(game.canEndTurn(player));
    }

    @Test
    void discardFromHandMovesCardToDiscardPile() {
        Player player = new Player("Player 1");
        MoneyCard card = new MoneyCard("1M", "Money", 1);
        player.draw(card);
        GameEngine game = new GameEngine(List.of(player, new Player("Player 2")), createDeck(10));

        assertTrue(game.discardFromHand(player, card));
        assertEquals(0, player.getHandSize());
        assertEquals(1, game.getDiscardPile().size());
    }

    private Deck createDeck(int size) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            cards.add(new MoneyCard(i + "M", "Money", 1));
        }
        return new Deck(cards);
    }
}
