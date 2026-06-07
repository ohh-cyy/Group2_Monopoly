package engine;

import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.RentCard;
import model.card.actionCard.DealBreaker;
import model.card.actionCard.Hotel;
import model.card.actionCard.House;
import model.card.actionCard.PassGoCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckAndRulesTest {

    @Test
    void deckFactoryCreatesLargeDeckWithMainCardTypes() {
        List<Card> cards = DeckFactory.createFullDeck();

        assertTrue(cards.size() >= 100);
        assertTrue(cards.stream().anyMatch(card -> card instanceof PropertyCard));
        assertTrue(cards.stream().anyMatch(card -> card instanceof MoneyCard));
        assertTrue(cards.stream().anyMatch(card -> card instanceof RentCard));
        assertTrue(cards.stream().anyMatch(card -> card instanceof PassGoCard));
        assertTrue(cards.stream().anyMatch(card -> card instanceof DealBreaker));
    }

    @Test
    void deckDrawRemovesOneCardFromDeck() {
        Deck deck = new Deck(List.of(
                new MoneyCard("1M", "Money", 1),
                new MoneyCard("2M", "Money", 2)
        ));

        Card drawn = deck.draw();

        assertNotNull(drawn);
        assertEquals(1, deck.size());
    }

    @Test
    void discardPileStoresTopCardAndCanBeCleared() {
        DiscardPile discardPile = new DiscardPile();
        Card first = new MoneyCard("1M", "Money", 1);
        Card second = new PassGoCard("Pass Go", "Draw two cards", CardType.ACTION);

        discardPile.addCard(first);
        discardPile.addCard(second);

        assertEquals(second, discardPile.peekTop());
        assertEquals(2, discardPile.size());
        discardPile.clear();
        assertTrue(discardPile.isEmpty());
    }

    @Test
    void propertyRulesDetectCompleteSet() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1));

        assertTrue(PropertyRules.isCompleteSet(player, Color.BROWN));
        assertFalse(PropertyRules.isCompleteSet(player, Color.RED));
    }

    @Test
    void rentTableReturnsRentBasedOnPropertyCount() {
        assertEquals(1, RentTable.getRent(Color.BROWN, 1));
        assertEquals(2, RentTable.getRent(Color.BROWN, 2));
        assertEquals(6, RentTable.getRent(Color.RED, 3));
        assertEquals(0, RentTable.getRent(null, 2));
    }

    @Test
    void propertyRulesAddsHouseAndHotelRentBonuses() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1));
        House house = new House("House", "Add house", CardType.ACTION);
        Hotel hotel = new Hotel("Hotel", "Add hotel", CardType.ACTION);

        assertEquals(2, PropertyRules.calculateRent(player, Color.BROWN));

        assertTrue(house.addHouseToSet(player, Color.BROWN));
        assertEquals(5, PropertyRules.calculateRent(player, Color.BROWN));

        assertTrue(hotel.addHotelToSet(player, Color.BROWN));
        assertEquals(9, PropertyRules.calculateRent(player, Color.BROWN));
    }

    @Test
    void completeSetRejectsAdditionalBillablePropertiesButAllowsHouse() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1));
        assertTrue(PropertyRules.isCompleteSet(player, Color.BROWN));
        assertFalse(PropertyRules.canAddBillableProperty(player, Color.BROWN));
        assertFalse(player.addProperty(new PropertyCard("Extra", "Brown", Color.BROWN, 1)));

        House house = new House("House", "Add house", CardType.ACTION);
        assertTrue(house.addHouseToSet(player, Color.BROWN));
        assertFalse(player.addProperty(new PropertyCard("Extra 2", "Brown", Color.BROWN, 1)));
        assertTrue(PropertyRules.hasHouse(player, Color.BROWN));
    }
}
