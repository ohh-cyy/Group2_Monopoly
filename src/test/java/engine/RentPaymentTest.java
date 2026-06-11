package engine;

import model.card.MoneyCard;
import model.card.PropertyCard;
import model.player.Player;
import model.enums.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 测试自动强制扣款 {@link RentPayment}：先扣最小银行卡，再扣地产（非交互路径用）。 */
class RentPaymentTest {

    /** 优先从银行扣款，按面值从小到大自动选取。 */
    @Test
    void collectUpToTakesMoneyFromBankFirst() {
        Player collector = new Player("Collector");
        Player debtor = new Player("Debtor");
        debtor.addBank(new MoneyCard("2M", "Money", 2));
        debtor.addBank(new MoneyCard("3M", "Money", 3));

        int paid = RentPayment.collectUpTo(collector, debtor, 5);

        assertEquals(5, paid);
        assertEquals(5, collector.getBankTotalValue());
        assertEquals(0, debtor.getBankTotalValue());
    }

    @Test
    void collectUpToUsesPropertyWhenBankIsNotEnough() {
        Player collector = new Player("Collector");
        Player debtor = new Player("Debtor");
        PropertyCard property = new PropertyCard("Pall Mall", "Pink property", Color.PINK, 2);
        debtor.addBank(new MoneyCard("1M", "Money", 1));
        debtor.addProperty(property);

        int paid = RentPayment.collectUpTo(collector, debtor, 3);

        assertEquals(3, paid);
        assertEquals(1, collector.getBankTotalValue());
        assertTrue(collector.getAllProperties().contains(property));
        assertFalse(debtor.getAllProperties().contains(property));
    }

    @Test
    void collectUpToCannotTakePropertyFromCompleteSet() {
        Player collector = new Player("Collector");
        Player debtor = new Player("Debtor");
        PropertyCard pink = new PropertyCard("Pall Mall", "Pink property", Color.PINK, 2);
        debtor.addProperty(new PropertyCard("Old Kent Road", "Brown", Color.BROWN, 1));
        debtor.addProperty(new PropertyCard("Whitechapel", "Brown", Color.BROWN, 1));
        debtor.addProperty(pink);

        int paid = RentPayment.collectUpTo(collector, debtor, 5);

        assertEquals(2, paid);
        assertTrue(debtor.hasCompleteSet(Color.BROWN));
        assertEquals(2, debtor.getPropertiesByColor(Color.BROWN).size());
        assertFalse(debtor.getAllProperties().contains(pink));
        assertTrue(collector.getAllProperties().contains(pink));
    }

    @Test
    void collectUpToReturnsZeroForInvalidAmount() {
        Player collector = new Player("Collector");
        Player debtor = new Player("Debtor");
        debtor.addBank(new MoneyCard("5M", "Money", 5));

        assertEquals(0, RentPayment.collectUpTo(collector, debtor, 0));
        assertEquals(5, debtor.getBankTotalValue());
    }
}
