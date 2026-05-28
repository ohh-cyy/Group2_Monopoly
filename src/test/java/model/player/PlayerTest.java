package model.player;

import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.actionCard.JustSayNo;
import model.enums.CardType;
import model.enums.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void drawAddsCardToHandAndRemoveFromHandRemovesIt() {
        Player player = new Player("Player");
        MoneyCard card = new MoneyCard("1M", "Money", 1);

        player.draw(card);
        assertEquals(1, player.getHandSize());

        player.removeFromHand(card);
        assertEquals(0, player.getHandSize());
    }

    @Test
    void addPropertyStoresPropertyByColor() {
        Player player = new Player("Player");
        PropertyCard property = new PropertyCard("Pall Mall", "Pink property", Color.PINK, 2);

        player.addProperty(property);

        assertEquals(1, player.getPropertiesByColor(Color.PINK).size());
        assertTrue(player.getAllProperties().contains(property));
    }

    @Test
    void hasCompleteSetUsesColorSetSize() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Park Lane", "Dark blue", Color.DARK_BLUE, 4));
        assertFalse(player.hasCompleteSet(Color.DARK_BLUE));

        player.addProperty(new PropertyCard("Mayfair", "Dark blue", Color.DARK_BLUE, 4));
        assertTrue(player.hasCompleteSet(Color.DARK_BLUE));
    }

    @Test
    void bankTotalIncludesMoneyCardsAndActionCards() {
        Player player = new Player("Player");
        player.addBank(new MoneyCard("5M", "Money", 5));
        player.addBank(new JustSayNo("Just Say No", "Cancel action", CardType.ACTION));

        assertEquals(9, player.getBankTotalValue());
    }

    @Test
    void findInHandByIdReturnsMatchingCard() {
        Player player = new Player("Player");
        MoneyCard card = new MoneyCard("1M", "Money", 1);
        player.draw(card);

        assertEquals(card, player.findInHandById(card.getInstanceId()));
        assertNull(player.findInHandById("missing-id"));
    }
}
