package engine;

import model.card.MoneyCard;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTransferTest {

    @Test
    void listPayableAssetsIncludesBankCardsAndUnprotectedProperties() {
        Player player = new Player("Payer");
        MoneyCard money = new MoneyCard("2M", "Money", 2);
        PropertyCard openProperty = new PropertyCard("Pink 1", "Pink", Color.PINK, 1);
        player.addBank(money);
        player.addProperty(openProperty);

        var assets = PaymentTransfer.listPayableAssets(player);

        assertEquals(2, assets.size());
        assertTrue(assets.contains(money));
        assertTrue(assets.contains(openProperty));
    }

    @Test
    void listPayableAssetsExcludesPropertiesInCompleteSets() {
        Player player = new Player("Payer");
        player.addProperty(new PropertyCard("Brown 1", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Brown 2", "Brown", Color.BROWN, 1));
        PropertyCard sparePink = new PropertyCard("Pink 1", "Pink", Color.PINK, 1);
        player.addProperty(sparePink);

        var assets = PaymentTransfer.listPayableAssets(player);

        assertEquals(1, assets.size());
        assertTrue(assets.contains(sparePink));
    }

    @Test
    void payWithCardMovesBankCardToCollector() {
        Player collector = new Player("Collector");
        Player payer = new Player("Payer");
        MoneyCard five = new MoneyCard("5M", "Money", 5);
        payer.addBank(five);

        var paid = PaymentTransfer.payWithCard(collector, payer, five.getInstanceId());

        assertTrue(paid.isPresent());
        assertEquals(5, paid.getAsInt());
        assertEquals(5, collector.getBankTotalValue());
        assertEquals(0, payer.getBankTotalValue());
    }

    @Test
    void payWithCardMovesEligiblePropertyToCollector() {
        Player collector = new Player("Collector");
        Player payer = new Player("Payer");
        PropertyCard property = new PropertyCard("Orange 1", "Orange", Color.ORANGE, 2);
        payer.addProperty(property);

        var paid = PaymentTransfer.payWithCard(collector, payer, property.getInstanceId());

        assertTrue(paid.isPresent());
        assertEquals(2, paid.getAsInt());
        assertTrue(collector.getAllProperties().contains(property));
        assertFalse(payer.getAllProperties().contains(property));
    }

    @Test
    void payWithCardRejectsCompleteSetProperty() {
        Player collector = new Player("Collector");
        Player payer = new Player("Payer");
        payer.addProperty(new PropertyCard("Brown 1", "Brown", Color.BROWN, 1));
        payer.addProperty(new PropertyCard("Brown 2", "Brown", Color.BROWN, 1));

        var paid = PaymentTransfer.payWithCard(collector, payer,
                payer.getPropertiesByColor(Color.BROWN).getFirst().getInstanceId());

        assertTrue(paid.isEmpty());
        assertEquals(2, payer.getPropertiesByColor(Color.BROWN).size());
    }

    @Test
    void getPaymentValueUsesPayableAssetContract() {
        assertEquals(3, PaymentTransfer.getPaymentValue(new MoneyCard("3M", "Money", 3)));
        assertEquals(4, PaymentTransfer.getPaymentValue(
                new PropertyCard("Green 1", "Green", Color.GREEN, 4)));
    }
}
