package model.card.actionCard;

import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static model.card.actionCard.ActionCardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/** 测试 Debt Collector 向单人收 5M 的 collectFrom 逻辑。 */
class DebtCollectorTest {

    @Test
    void collectFromTakesFiveMillionFromChosenTargetBank() {
        Player collector = player("Collector");
        Player target = player("Target");
        target.addBank(money(2));
        target.addBank(money(3));
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);

        int paid = debtCollector.collectFrom(collector, target);

        assertEquals(DebtCollector.DEBT_AMOUNT, paid);
        assertEquals(5, collector.getBankTotalValue());
        assertEquals(0, target.getBankTotalValue());
    }

    @Test
    void collectFromUsesPropertyWhenBankCannotCoverDebt() {
        Player collector = player("Collector");
        Player target = player("Target");
        PropertyCard property = property("Pall Mall", Color.PINK, 2);
        target.addBank(money(3));
        target.addProperty(property);
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);

        int paid = debtCollector.collectFrom(collector, target);

        assertEquals(5, paid);
        assertEquals(3, collector.getBankTotalValue());
        assertTrue(collector.getAllProperties().contains(property));
        assertFalse(target.getAllProperties().contains(property));
    }

    @Test
    void collectFromReturnsZeroForInvalidTarget() {
        Player collector = player("Collector");
        Player target = player("Target");
        target.addBank(money(5));
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);

        assertEquals(0, debtCollector.collectFrom(collector, collector));
        assertEquals(0, debtCollector.collectFrom(null, target));
        assertEquals(0, debtCollector.collectFrom(collector, null));
        assertEquals(5, target.getBankTotalValue());
    }

    @Test
    void proactiveUseHasNoEffectBecauseControllerSelectsTheTarget() {
        Player collector = player("Collector");
        Player target = player("Target");
        target.addBank(money(5));
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);

        debtCollector.use(collector, game(collector, target));

        assertEquals(0, collector.getBankTotalValue());
        assertEquals(5, target.getBankTotalValue());
    }
}
