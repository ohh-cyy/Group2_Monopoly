package model.card.actionCard;

import engine.Deck;
import engine.GameEngine;
import model.card.Card;
import model.card.MoneyCard;
import model.enums.CardType;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static model.card.actionCard.ActionCardTestSupport.player;
import static org.junit.jupiter.api.Assertions.*;

class PassGoCardTest {

    @Test
    void useDrawsTwoCardsFromDeck() {
        Player player = player("Player");
        Player opponent = player("Opponent");
        Deck deck = new Deck(List.of(
                new MoneyCard("1M", "Money", 1),
                new MoneyCard("2M", "Money", 2),
                new MoneyCard("3M", "Money", 3)
        ));
        GameEngine game = new GameEngine(List.of(player, opponent), deck);
        PassGoCard passGo = new PassGoCard("Pass Go", "Draw two cards", CardType.ACTION);

        passGo.use(player, game);

        assertEquals(2, player.getHandSize());
        assertEquals(1, deck.size());
    }

    @Test
    void useStopsEarlyWhenDeckRunsOut() {
        Player player = player("Player");
        Player opponent = player("Opponent");
        Deck deck = new Deck(List.of(new MoneyCard("1M", "Money", 1)));
        GameEngine game = new GameEngine(List.of(player, opponent), deck);
        PassGoCard passGo = new PassGoCard("Pass Go", "Draw two cards", CardType.ACTION);

        passGo.use(player, game);

        assertEquals(1, player.getHandSize());
        assertTrue(deck.isEmpty());
    }

    @Test
    void useIgnoresNullPlayerOrGame() {
        Player player = player("Player");
        Player opponent = player("Opponent");
        Deck deck = new Deck(List.of(new MoneyCard("1M", "Money", 1)));
        GameEngine game = new GameEngine(List.of(player, opponent), deck);
        PassGoCard passGo = new PassGoCard("Pass Go", "Draw two cards", CardType.ACTION);

        assertDoesNotThrow(() -> passGo.use(null, game));
        assertDoesNotThrow(() -> passGo.use(player, null));

        assertEquals(0, player.getHandSize());
        assertEquals(1, deck.size());
    }

    @Test
    void constructorWithInstanceIdKeepsTheId() {
        Card passGo = new PassGoCard("id-pass-go", "Pass Go", "Draw two cards", CardType.ACTION);

        assertEquals("id-pass-go", passGo.getInstanceId());
    }
}
