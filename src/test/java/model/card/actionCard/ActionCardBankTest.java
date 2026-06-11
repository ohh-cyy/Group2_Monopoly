package model.card.actionCard;

import model.card.RentCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static model.card.actionCard.ActionCardTestSupport.player;
import static org.junit.jupiter.api.Assertions.*;

/** 测试行动卡存入银行及银行面值。 */
class ActionCardBankTest {

    @Test
    void depositToBankAddsActionCardAndCountsItsBankValue() {
        Player player = player("Player");
        ActionCard card = new SimpleActionCard("Pass Go", "Draw two cards", 1);

        card.depositToBank(player);

        assertEquals(1, player.getBank().size());
        assertTrue(player.getBank().contains(card));
        assertEquals(1, player.getBankTotalValue());
    }

    @Test
    void depositToBankIgnoresNullPlayer() {
        ActionCard card = new SimpleActionCard("Action", "No target", 2);

        assertDoesNotThrow(() -> card.depositToBank(null));
    }

    @Test
    void actionCardConstructorKeepsIdentityAndMetadata() {
        ActionCard card = new SimpleActionCard("fixed-id", "Action", "Description", CardType.ACTION, 2);

        assertEquals("fixed-id", card.getInstanceId());
        assertEquals("Action", card.getName());
        assertEquals("Description", card.getDescription());
        assertEquals(CardType.ACTION, card.getType());
        assertEquals(2, card.getBankValueM());
    }

    @Test
    void concreteActionCardsExposeExpectedBankValues() {
        assertEquals(1, new PassGoCard("Pass Go", "Draw two", CardType.ACTION).getBankValueM());
        assertEquals(1, new DoubleTheRent("Double The Rent", "Double next rent", CardType.ACTION).getBankValueM());
        assertEquals(2, new MyBirthday("It's My Birthday", "Collect gifts", CardType.ACTION).getBankValueM());
        assertEquals(3, new DebtCollector("Debt Collector", "Collect debt", CardType.ACTION).getBankValueM());
        assertEquals(3, new SlyDeal("Sly Deal", "Steal property", CardType.ACTION).getBankValueM());
        assertEquals(3, new ForcedDeal("Forced Deal", "Swap property", CardType.ACTION).getBankValueM());
        assertEquals(3, new House("House", "Add house", CardType.ACTION).getBankValueM());
        assertEquals(4, new Hotel("Hotel", "Add hotel", CardType.ACTION).getBankValueM());
        assertEquals(4, new JustSayNo("Just Say No", "Cancel action", CardType.ACTION).getBankValueM());
        assertEquals(5, new DealBreaker("Deal Breaker", "Steal set", CardType.ACTION).getBankValueM());
        assertEquals(1, RentCard.dual(Color.BROWN, Color.LIGHT_BLUE).getBankValueM());
        assertEquals(3, RentCard.allColors().getBankValueM());
    }
}
