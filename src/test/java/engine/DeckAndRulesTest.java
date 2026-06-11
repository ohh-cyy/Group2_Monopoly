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

/** 测试牌堆工厂、抽牌、弃牌堆，以及 {@link PropertyRules} / {@link RentTable} 规则。 */
class DeckAndRulesTest {

    /** 完整牌堆应包含 100+ 张牌，且涵盖地产、金钱、租金、Pass Go、Deal Breaker 等类型。 */
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

    /** 从牌堆抽牌后，牌堆数量应减 1。 */
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

    /** 弃牌堆应能记录顶牌、统计数量，并在洗回牌堆后清空。 */
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

    /** 地产数量达到该颜色套牌要求时应判定为完整套。 */
    @Test
    void propertyRulesDetectCompleteSet() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1));

        assertTrue(PropertyRules.isCompleteSet(player, Color.BROWN));
        assertFalse(PropertyRules.isCompleteSet(player, Color.RED));
    }

    /** 租金表应按颜色与地产数量返回正确租金。 */
    @Test
    void rentTableReturnsRentBasedOnPropertyCount() {
        assertEquals(1, RentTable.getRent(Color.BROWN, 1));
        assertEquals(2, RentTable.getRent(Color.BROWN, 2));
        assertEquals(6, RentTable.getRent(Color.RED, 3));
        assertEquals(0, RentTable.getRent(null, 2));
    }

    /** 完整套上加房屋 +3M、加酒店 +4M 租金加成。 */
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

    /** 完整套不能再加普通地产，但仍可加房屋。 */
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
