package model.card.actionCard;

import engine.Deck;
import engine.GameEngine;
import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.RentCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all ActionCard subclasses.
 * Covers happy-path behaviour, edge cases, and integration between
 * DoubleTheRent + RentCard.
 */
class ActionCardTest {

    // ---------- PassGoCard ----------

    @Test
    void passGoDrawsTwoCardsWhenDeckHasEnoughCards() {
        Player player = new Player("Player 1");
        GameEngine game = createGame(player, new Player("Player 2"));
        PassGoCard passGo = new PassGoCard("Pass Go", "Draw two cards", CardType.ACTION);

        passGo.use(player, game);

        assertEquals(2, player.getHandSize());
        assertEquals(1, game.getDeck().size());
    }

    // ---------- DebtCollector ----------

    @Test
    void debtCollectorCollectsFiveMillionFromTarget() {
        Player collector = new Player("Collector");
        Player target = new Player("Target");
        target.addBank(new MoneyCard("5M", "Money", 5));
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);

        int paid = debtCollector.collectFrom(collector, target);

        assertEquals(5, paid);
        assertEquals(5, collector.getBankTotalValue());
        assertEquals(0, target.getBankTotalValue());
    }

    // ---------- MyBirthday ----------

    @Test
    void myBirthdayCollectsTwoMillionFromEachOtherPlayer() {
        Player birthdayPlayer = new Player("Birthday Player");
        Player playerTwo = new Player("Player 2");
        Player playerThree = new Player("Player 3");
        playerTwo.addBank(new MoneyCard("2M", "Money", 2));
        playerThree.addBank(new MoneyCard("2M", "Money", 2));
        GameEngine game = createGame(birthdayPlayer, playerTwo, playerThree);
        MyBirthday myBirthday = new MyBirthday("My Birthday", "Collect 2M from everyone", CardType.ACTION);

        int total = myBirthday.collectFromEveryone(birthdayPlayer, game);

        assertEquals(4, total);
        assertEquals(4, birthdayPlayer.getBankTotalValue());
        assertEquals(0, playerTwo.getBankTotalValue());
        assertEquals(0, playerThree.getBankTotalValue());
    }

    // ---------- DoubleTheRent ----------

    @Test
    void doubleTheRentCanOnlyBeActivatedOnceBeforeRentCard() {
        Player player = new Player("Player 1");
        GameEngine game = createGame(player, new Player("Player 2"));
        DoubleTheRent doubleTheRent = new DoubleTheRent("Double The Rent", "Double rent", CardType.ACTION);

        assertTrue(doubleTheRent.activateForNextRent(game));
        assertTrue(game.isRentDoubled());
        assertFalse(doubleTheRent.activateForNextRent(game));
    }

    // ---------- SlyDeal ----------

    @Test
    void slyDealStealsPropertyThatIsNotInCompleteSet() {
        Player thief = new Player("Thief");
        Player target = new Player("Target");
        PropertyCard property = new PropertyCard("Old Kent Road", "Brown property", Color.BROWN, 1);
        target.addProperty(property);
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal one property", CardType.ACTION);

        boolean result = slyDeal.stealProperty(thief, target, property);

        assertTrue(result);
        assertTrue(thief.getAllProperties().contains(property));
        assertFalse(target.getAllProperties().contains(property));
    }

    @Test
    void slyDealCannotStealPropertyFromCompleteSet() {
        Player thief = new Player("Thief");
        Player target = new Player("Target");
        PropertyCard first = new PropertyCard("Old Kent Road", "Brown property", Color.BROWN, 1);
        PropertyCard second = new PropertyCard("Whitechapel", "Brown property", Color.BROWN, 1);
        target.addProperty(first);
        target.addProperty(second);
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal one property", CardType.ACTION);

        boolean result = slyDeal.stealProperty(thief, target, first);

        assertFalse(result);
        assertFalse(thief.getAllProperties().contains(first));
        assertTrue(target.getAllProperties().contains(first));
    }

    // ---------- ForcedDeal ----------

    @Test
    void forcedDealSwapsPropertiesWhenTargetPropertyIsNotCompleteSet() {
        Player player = new Player("Player");
        Player target = new Player("Target");
        PropertyCard playerProperty = new PropertyCard("Pall Mall", "Pink property", Color.PINK, 2);
        PropertyCard targetProperty = new PropertyCard("Bow Street", "Orange property", Color.ORANGE, 2);
        player.addProperty(playerProperty);
        target.addProperty(targetProperty);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        boolean result = forcedDeal.swapProperties(player, playerProperty, target, targetProperty);

        assertTrue(result);
        assertTrue(player.getAllProperties().contains(targetProperty));
        assertTrue(target.getAllProperties().contains(playerProperty));
        assertFalse(player.getAllProperties().contains(playerProperty));
        assertFalse(target.getAllProperties().contains(targetProperty));
    }

    // ---------- DealBreaker ----------

    @Test
    void dealBreakerStealsCompleteSet() {
        Player player = new Player("Player");
        Player target = new Player("Target");
        PropertyCard first = new PropertyCard("Park Lane", "Dark blue property", Color.DARK_BLUE, 4);
        PropertyCard second = new PropertyCard("Mayfair", "Dark blue property", Color.DARK_BLUE, 4);
        target.addProperty(first);
        target.addProperty(second);
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal a complete set", CardType.ACTION);

        boolean result = dealBreaker.useOnTarget(player, target, Color.DARK_BLUE);

        assertTrue(result);
        assertEquals(2, player.getPropertiesByColor(Color.DARK_BLUE).size());
        assertEquals(0, target.getPropertiesByColor(Color.DARK_BLUE).size());
    }

    // ---------- House / Hotel ----------

    @Test
    void houseCanOnlyBeAddedOnceToACompleteSet() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Old Kent Road", "Brown property", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Whitechapel", "Brown property", Color.BROWN, 1));
        House house = new House("House", "Add house", CardType.ACTION);

        assertTrue(house.addHouseToSet(player, Color.BROWN));
        assertFalse(house.addHouseToSet(player, Color.BROWN));
        assertTrue(house.hasHouse(player, Color.BROWN));
    }

    @Test
    void hotelRequiresAHouseOnTheCompleteSet() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Old Kent Road", "Brown property", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Whitechapel", "Brown property", Color.BROWN, 1));
        Hotel hotel = new Hotel("Hotel", "Add hotel", CardType.ACTION);
        House house = new House("House", "Add house", CardType.ACTION);

        assertFalse(hotel.addHotelToSet(player, Color.BROWN));
        assertTrue(house.addHouseToSet(player, Color.BROWN));
        assertTrue(hotel.addHotelToSet(player, Color.BROWN));
        assertFalse(hotel.addHotelToSet(player, Color.BROWN));
        assertTrue(hotel.hasHotel(player, Color.BROWN));
    }

    // ---------- Deposit to bank ----------

    @Test
    void actionCardCanBeDepositedToBank() {
        Player player = new Player("Player");
        ActionCard card = new JustSayNo("Just Say No", "Cancel action", CardType.ACTION);

        card.depositToBank(player);

        assertEquals(1, player.getBank().size());
        assertEquals(4, player.getBankTotalValue());
    }

    private GameEngine createGame(Player... players) {
        List<Card> deckCards = new ArrayList<>();
        deckCards.add(new MoneyCard("1M", "Money", 1));
        deckCards.add(new MoneyCard("2M", "Money", 2));
        deckCards.add(new MoneyCard("3M", "Money", 3));
        return new GameEngine(List.of(players), new Deck(deckCards));
    }

    private GameEngine createGameWithDeck(Player[] players, List<Card> deckCards) {
        return new GameEngine(List.of(players), new Deck(deckCards));
    }

    // ---------- PassGoCard edge cases ----------

    @Test
    void passGoDrawsZeroCardsWhenDeckIsEmpty() {
        Player player = new Player("Player 1");
        GameEngine game = createGameWithDeck(
                new Player[]{player, new Player("Player 2")}, List.of());
        PassGoCard passGo = new PassGoCard("Pass Go", "Draw two cards", CardType.ACTION);

        passGo.use(player, game);

        assertEquals(0, player.getHandSize());
    }

    @Test
    void passGoDrawsOneCardWhenDeckHasOnlyOneCard() {
        Player player = new Player("Player 1");
        List<Card> deckCards = List.of(new MoneyCard("5M", "Money", 5));
        GameEngine game = createGameWithDeck(
                new Player[]{player, new Player("Player 2")}, deckCards);
        PassGoCard passGo = new PassGoCard("Pass Go", "Draw two cards", CardType.ACTION);

        passGo.use(player, game);

        assertEquals(1, player.getHandSize());
        assertTrue(game.getDeck().isEmpty());
    }

    // ---------- JustSayNo ----------

    @Test
    void justSayNoCanBeDepositedToBankWithCorrectValue() {
        Player player = new Player("Player");
        JustSayNo jsn = new JustSayNo("Just Say No", "Cancel action", CardType.ACTION);

        jsn.depositToBank(player);

        assertEquals(1, player.getBank().size());
        assertEquals(4, player.getBankTotalValue());
    }

    // ---------- DebtCollector edge cases ----------

    @Test
    void debtCollectorCollectsPartialPaymentFromPropertyWhenBankInsufficient() {
        Player collector = new Player("Collector");
        Player target = new Player("Target");
        target.addBank(new MoneyCard("2M", "Money", 2));
        PropertyCard property = new PropertyCard("Pall Mall", "Pink", Color.PINK, 2);
        target.addProperty(property);
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);

        int paid = debtCollector.collectFrom(collector, target);

        assertEquals(4, paid);
        assertEquals(2, collector.getBankTotalValue());
        assertTrue(collector.getAllProperties().contains(property));
        assertFalse(target.getAllProperties().contains(property));
    }

    @Test
    void debtCollectorCollectsNothingWhenTargetHasNoAssets() {
        Player collector = new Player("Collector");
        Player target = new Player("Target");
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);

        int paid = debtCollector.collectFrom(collector, target);

        assertEquals(0, paid);
        assertEquals(0, collector.getBankTotalValue());
    }

    // ---------- MyBirthday edge cases ----------

    @Test
    void myBirthdayCollectsPartialPaymentIncludingProperty() {
        Player birthdayPlayer = new Player("Birthday");
        Player other = new Player("Other");
        other.addBank(new MoneyCard("1M", "Money", 1));
        PropertyCard property = new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1);
        other.addProperty(property);
        GameEngine game = createGameWithDeck(
                new Player[]{birthdayPlayer, other},
                List.of(new MoneyCard("1M", "Money", 1)));
        MyBirthday myBirthday = new MyBirthday("My Birthday", "Collect 2M", CardType.ACTION);

        int total = myBirthday.collectFromEveryone(birthdayPlayer, game);

        assertEquals(2, total);
        assertEquals(1, birthdayPlayer.getBankTotalValue());
        assertTrue(birthdayPlayer.getAllProperties().contains(property));
    }

    // ---------- SlyDeal edge cases ----------

    @Test
    void slyDealFailsWhenTargetHasNoProperties() {
        Player thief = new Player("Thief");
        Player target = new Player("Target");
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal property", CardType.ACTION);

        boolean result = slyDeal.stealProperty(thief, target,
                new PropertyCard("Fake", "Not owned", Color.BROWN, 1));

        assertFalse(result);
        assertTrue(thief.getAllProperties().isEmpty());
    }

    @Test
    void slyDealFailsWhenTargetDoesNotOwnTheProperty() {
        Player thief = new Player("Thief");
        Player target = new Player("Target");
        PropertyCard ownedByTarget = new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1);
        PropertyCard notOwned = new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1);
        target.addProperty(ownedByTarget);
        SlyDeal slyDeal = new SlyDeal("Sly Deal", "Steal property", CardType.ACTION);

        boolean result = slyDeal.stealProperty(thief, target, notOwned);

        assertFalse(result);
        assertTrue(target.getAllProperties().contains(ownedByTarget));
        assertFalse(thief.getAllProperties().contains(notOwned));
    }

    // ---------- ForcedDeal edge cases ----------

    @Test
    void forcedDealFailsWhenTargetPropertyIsInCompleteSet() {
        Player player = new Player("Player");
        Player target = new Player("Target");
        PropertyCard myProp = new PropertyCard("Bow Street", "Orange", Color.ORANGE, 2);
        PropertyCard targetProp1 = new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1);
        PropertyCard targetProp2 = new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1);
        player.addProperty(myProp);
        target.addProperty(targetProp1);
        target.addProperty(targetProp2);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        boolean result = forcedDeal.swapProperties(player, myProp, target, targetProp1);

        assertFalse(result);
        assertTrue(player.getAllProperties().contains(myProp));
        assertTrue(target.getAllProperties().contains(targetProp1));
    }

    @Test
    void forcedDealFailsWhenPlayerDoesNotOwnGivenProperty() {
        Player player = new Player("Player");
        Player target = new Player("Target");
        PropertyCard notMine = new PropertyCard("Strand", "Red", Color.RED, 3);
        PropertyCard targetProp = new PropertyCard("Bow Street", "Orange", Color.ORANGE, 2);
        target.addProperty(targetProp);
        ForcedDeal forcedDeal = new ForcedDeal("Forced Deal", "Swap properties", CardType.ACTION);

        boolean result = forcedDeal.swapProperties(player, notMine, target, targetProp);

        assertFalse(result);
        assertTrue(target.getAllProperties().contains(targetProp));
    }

    // ---------- DealBreaker edge cases ----------

    @Test
    void dealBreakerFailsWhenTargetHasNoCompleteSet() {
        Player player = new Player("Player");
        Player target = new Player("Target");
        target.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal set", CardType.ACTION);

        boolean result = dealBreaker.useOnTarget(player, target, Color.BROWN);

        assertFalse(result);
        assertTrue(player.getAllProperties().isEmpty());
        assertEquals(1, target.getPropertiesByColor(Color.BROWN).size());
    }

    @Test
    void dealBreakerFailsWithNullTarget() {
        Player player = new Player("Player");
        DealBreaker dealBreaker = new DealBreaker("Deal Breaker", "Steal set", CardType.ACTION);

        assertFalse(dealBreaker.useOnTarget(player, null, Color.BROWN));
        assertFalse(dealBreaker.useOnTarget(player, player, Color.BROWN));
    }

    // ---------- House / Hotel edge cases ----------

    @Test
    void houseFailsWhenSetIsNotComplete() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        House house = new House("House", "Add house", CardType.ACTION);

        assertFalse(house.addHouseToSet(player, Color.BROWN));
    }

    @Test
    void hotelFailsWhenNoHouseOnSet() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Park Lane", "Dark blue", Color.DARK_BLUE, 4));
        player.addProperty(new PropertyCard("Mayfair", "Dark blue", Color.DARK_BLUE, 4));
        Hotel hotel = new Hotel("Hotel", "Add hotel", CardType.ACTION);

        assertFalse(hotel.addHotelToSet(player, Color.DARK_BLUE));
        assertFalse(hotel.hasHotel(player, Color.DARK_BLUE));
    }

    // ---------- DoubleTheRent + RentCard integration ----------

    @Test
    void doubleTheRentDoublesNextRentCardCollection() {
        Player collector = new Player("Collector");
        Player target = new Player("Target");
        collector.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        collector.addProperty(new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1));
        target.addBank(new MoneyCard("4M", "Money", 4));
        GameEngine game = createGameWithDeck(
                new Player[]{collector, target},
                List.of(new MoneyCard("1M", "Money", 1)));

        DoubleTheRent doubleRent = new DoubleTheRent("Double The Rent", "Double rent", CardType.ACTION);
        assertTrue(doubleRent.activateForNextRent(game));

        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);
        int total = rentCard.collectFromAll(collector, game, Color.BROWN,
                rentCard.calculateRent(collector, Color.BROWN));

        assertEquals(4, total);
        assertEquals(4, collector.getBankTotalValue());
        assertEquals(0, target.getBankTotalValue());
        assertFalse(game.isRentDoubled());
    }

    // ---------- RentCard ----------

    @Test
    void rentCardCannotPlayWhenPlayerHasNoMatchingProperties() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Strand", "Red", Color.RED, 3));
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        assertFalse(rentCard.canPlay(player));
        assertTrue(rentCard.getChargeableColors(player).isEmpty());
    }

    @Test
    void rentCardCanBeDepositedToBankWithCorrectValue() {
        Player player = new Player("Player");
        RentCard dualRent = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);
        RentCard allRent = RentCard.allColors();

        dualRent.depositToBank(player);
        allRent.depositToBank(player);

        assertEquals(2, player.getBank().size());
        assertEquals(4, player.getBankTotalValue());
    }

    @Test
    void rentCardAllColorsCanChargeAnyOwnedColor() {
        Player player = new Player("Player");
        player.addProperty(new PropertyCard("Strand", "Red", Color.RED, 3));
        RentCard rentCard = RentCard.allColors();

        assertTrue(rentCard.canPlay(player));
        assertEquals(1, rentCard.getChargeableColors(player).size());
        assertEquals(Color.RED, rentCard.getChargeableColors(player).get(0));
    }

    @Test
    void rentCardReturnsZeroRentForColorWithNoProperties() {
        Player player = new Player("Player");
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        assertEquals(0, rentCard.calculateRent(player, Color.BROWN));
    }

    // ---------- ActionCard base behaviour ----------

    @Test
    void actionCardStoresBankValueM() {
        ActionCard debtCollector = new DebtCollector("DC", "Collect 5M", CardType.ACTION);
        ActionCard passGo = new PassGoCard("PG", "Draw 2", CardType.ACTION);
        ActionCard dealBreaker = new DealBreaker("DB", "Steal set", CardType.ACTION);

        assertEquals(3, debtCollector.getBankValueM());
        assertEquals(1, passGo.getBankValueM());
        assertEquals(5, dealBreaker.getBankValueM());
    }

    @Test
    void multipleActionCardsCanBeDepositedToBank() {
        Player player = new Player("Player");
        ActionCard jsn = new JustSayNo("Just Say No", "Cancel", CardType.ACTION);
        ActionCard debt = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);
        ActionCard passGo = new PassGoCard("Pass Go", "Draw 2", CardType.ACTION);

        jsn.depositToBank(player);
        debt.depositToBank(player);
        passGo.depositToBank(player);

        assertEquals(3, player.getBank().size());
        assertEquals(8, player.getBankTotalValue());
    }
}
