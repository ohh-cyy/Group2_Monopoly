package network.server;

import engine.Deck;
import engine.GameEngine;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.enums.Color;
import model.player.Player;
import network.protocol.ClientMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionNetworkPlayTest {

    @Test
    void playToBankAllowsBankableWildPropertyOnline() {
        GameSession session = new GameSession();
        Player player = new Player("Player");
        WildpropertyCard wild = new WildpropertyCard(
                "Orange/Pink", "Wild property", 2, List.of(Color.ORANGE, Color.PINK), true);
        player.draw(wild);

        boolean played = session.playToBank(player, wild);

        assertTrue(played);
        assertFalse(player.getHand().contains(wild));
        assertTrue(player.getBank().contains(wild));
        assertEquals(2, player.getBankTotalValue());
    }

    @Test
    void playToBankRejectsNonBankableWildPropertyOnline() {
        GameSession session = new GameSession();
        Player player = new Player("Player");
        WildpropertyCard wild = new WildpropertyCard(
                "All Color", "Wild property", 0, List.of(Color.values()), false);
        player.draw(wild);

        boolean played = session.playToBank(player, wild);

        assertFalse(played);
        assertTrue(player.getHand().contains(wild));
        assertFalse(player.getBank().contains(wild));
    }

    @Test
    void serverRentRejectsColorOutsideRentCardEvenWhenPlayerOwnsThatColor() {
        Player collector = new Player("Collector");
        Player opponent = new Player("Opponent");
        collector.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        collector.addProperty(new PropertyCard("Strand", "Red", Color.RED, 3));
        opponent.addBank(new MoneyCard("2M", "Money", 2));
        GameEngine game = createGame(collector, opponent);
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);
        ClientMessage message = new ClientMessage();
        message.color = Color.RED.name();

        boolean applied = new ServerPlayHandler().applyEffect(game, collector, rentCard, message, new ArrayList<>());

        assertFalse(applied);
        assertEquals(0, collector.getBankTotalValue());
        assertEquals(2, opponent.getBankTotalValue());
    }

    @Test
    void serverRentAcceptsChargeableRentColor() {
        Player collector = new Player("Collector");
        Player opponent = new Player("Opponent");
        collector.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        opponent.addBank(new MoneyCard("2M", "Money", 2));
        GameEngine game = createGame(collector, opponent);
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);
        ClientMessage message = new ClientMessage();
        message.color = Color.BROWN.name();

        boolean applied = new ServerPlayHandler().applyEffect(game, collector, rentCard, message, new ArrayList<>());

        assertTrue(applied);
        assertEquals(2, collector.getBankTotalValue());
        assertEquals(0, opponent.getBankTotalValue());
    }

    private GameEngine createGame(Player... players) {
        return new GameEngine(List.of(players), new Deck(List.of(
                new MoneyCard("1M", "Money", 1),
                new MoneyCard("2M", "Money", 2),
                new MoneyCard("3M", "Money", 3)
        )));
    }
}
