package model.card;

import engine.Deck;
import engine.GameEngine;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void moneyCardUseAddsCardToPlayerBank() {
        Player player = new Player("Player");
        GameEngine game = createGame(player, new Player("Opponent"));
        MoneyCard moneyCard = new MoneyCard("5M", "Money", 5);

        moneyCard.use(player, game);

        assertEquals(1, player.getBank().size());
        assertEquals(5, player.getBankTotalValue());
    }

    @Test
    void propertyCardUseAddsPropertyToPlayerBoard() {
        Player player = new Player("Player");
        GameEngine game = createGame(player, new Player("Opponent"));
        PropertyCard propertyCard = new PropertyCard("Pall Mall", "Pink property", Color.PINK, 2);

        propertyCard.use(player, game);

        assertEquals(1, player.getPropertiesByColor(Color.PINK).size());
    }

    @Test
    void wildPropertyCanOnlyUseAfterChoosingAvailableColor() {
        Player player = new Player("Player");
        GameEngine game = createGame(player, new Player("Opponent"));
        WildpropertyCard wildCard = new WildpropertyCard(
                "Orange/Pink", "Wild property", 2, List.of(Color.ORANGE, Color.PINK), true);

        wildCard.use(player, game);
        assertEquals(0, player.getAllProperties().size());

        wildCard.setChosenColor(Color.PINK);
        wildCard.use(player, game);
        assertEquals(1, player.getPropertiesByColor(Color.PINK).size());
    }

    @Test
    void rentCardCalculatesRentFromOwnedProperties() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1));
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        assertTrue(rentCard.canPlay(player));
        assertEquals(2, rentCard.calculateRent(player, Color.BROWN));
        assertEquals(List.of(Color.BROWN), rentCard.getChargeableColors(player));
    }

    @Test
    void rentCardCollectsRentFromOtherPlayers() {
        Player collector = new Player("Collector");
        Player target = new Player("Target");
        collector.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        target.addBank(new MoneyCard("1M", "Money", 1));
        GameEngine game = createGame(collector, target);
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        int total = rentCard.collectFromAll(collector, game, Color.BROWN, 1);

        assertEquals(1, total);
        assertEquals(1, collector.getBankTotalValue());
        assertEquals(0, target.getBankTotalValue());
    }

    private GameEngine createGame(Player... players) {
        return new GameEngine(List.of(players), new Deck(List.of(
                new MoneyCard("1M", "Money", 1),
                new MoneyCard("2M", "Money", 2),
                new MoneyCard("3M", "Money", 3)
        )));
    }
}
