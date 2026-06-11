package model.card.actionCard;

import engine.GameEngine;
import model.card.RentCard;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static model.card.actionCard.ActionCardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/** 测试租金卡 {@link RentCard} 的可打出条件、算租与收租逻辑。 */
class RentCardTest {

    @Test
    void dualRentCardCanPlayOnlyWhenPlayerOwnsMatchingColor() {
        Player player = player("Player");
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        assertFalse(rentCard.canPlay(player));

        player.addProperty(property("Pall Mall", Color.PINK, 2));
        assertFalse(rentCard.canPlay(player));

        player.addProperty(property("Old Kent Road", Color.BROWN, 1));
        assertTrue(rentCard.canPlay(player));
    }

    @Test
    void allColorRentCardCanAlwaysPlayButOnlyListsOwnedColors() {
        Player player = player("Player");
        RentCard rentCard = RentCard.allColors();

        assertTrue(rentCard.canPlay(player));
        assertTrue(rentCard.getChargeableColors(player).isEmpty());

        player.addProperty(property("Old Kent Road", Color.BROWN, 1));
        assertEquals(List.of(Color.BROWN), rentCard.getChargeableColors(player));
    }

    @Test
    void applicableColorsAreReturnedAsDefensiveCopy() {
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        Color[] colors = rentCard.getApplicableColors();
        colors[0] = Color.RED;

        assertArrayEquals(new Color[]{Color.BROWN, Color.LIGHT_BLUE}, rentCard.getApplicableColors());
    }

    @Test
    void calculateRentUsesOwnedPropertyCountAndRentTable() {
        Player player = player("Player");
        player.addProperty(property("Old Kent Road", Color.BROWN, 1));
        player.addProperty(property("Whitechapel Road", Color.BROWN, 1));
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        assertEquals(2, rentCard.countProperties(player, Color.BROWN));
        assertEquals(2, rentCard.calculateRent(player, Color.BROWN));
        assertEquals(List.of(Color.BROWN), rentCard.getChargeableColors(player));
    }

    @Test
    void collectFromAllChargesEveryOpponentAndConsumesDoubleRentFlag() {
        Player collector = player("Collector");
        Player firstOpponent = player("First Opponent");
        Player secondOpponent = player("Second Opponent");
        firstOpponent.addBank(money(2));
        secondOpponent.addBank(money(2));
        GameEngine game = game(collector, firstOpponent, secondOpponent);
        game.setRentDoubled(true);
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        int collected = rentCard.collectFromAll(collector, game, Color.BROWN, 1);

        assertEquals(4, collected);
        assertEquals(4, collector.getBankTotalValue());
        assertEquals(0, firstOpponent.getBankTotalValue());
        assertEquals(0, secondOpponent.getBankTotalValue());
        assertFalse(game.isRentDoubled());
    }

    @Test
    void collectFromAllReturnsZeroForNonPositiveRent() {
        Player collector = player("Collector");
        Player opponent = player("Opponent");
        opponent.addBank(money(2));
        GameEngine game = game(collector, opponent);
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        assertEquals(0, rentCard.collectFromAll(collector, game, Color.BROWN, 0));
        assertEquals(0, collector.getBankTotalValue());
        assertEquals(2, opponent.getBankTotalValue());
    }

    @Test
    void useChoosesBestOwnedColorCollectsRentAndDiscardsCard() {
        Player collector = player("Collector");
        Player opponent = player("Opponent");
        collector.addProperty(property("Old Kent Road", Color.BROWN, 1));
        collector.addProperty(property("Park Lane", Color.DARK_BLUE, 4));
        collector.addProperty(property("Mayfair", Color.DARK_BLUE, 4));
        opponent.addBank(money(3));
        opponent.addBank(money(5));
        GameEngine game = game(collector, opponent);
        RentCard rentCard = RentCard.allColors();

        rentCard.use(collector, game);

        assertEquals(8, collector.getBankTotalValue());
        assertEquals(0, opponent.getBankTotalValue());
        assertEquals(1, game.getDiscardPile().size());
        assertEquals(rentCard, game.getDiscardPile().peekTop());
    }

    @Test
    void useDoesNothingWhenDualCardHasNoChargeableColor() {
        Player collector = player("Collector");
        Player opponent = player("Opponent");
        opponent.addBank(money(2));
        GameEngine game = game(collector, opponent);
        RentCard rentCard = RentCard.dual(Color.BROWN, Color.LIGHT_BLUE);

        rentCard.use(collector, game);

        assertEquals(0, collector.getBankTotalValue());
        assertEquals(2, opponent.getBankTotalValue());
        assertTrue(game.getDiscardPile().isEmpty());
    }
}
