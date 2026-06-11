package controller.gameplay;

import controller.session.LocalGameSession;
import engine.GameEngine;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.actionCard.PassGoCard;
import model.card.actionCard.SimpleActionCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testsupport.StubGameDialogService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 测试本地交互式支付 {@link PaymentService}：玩家自选银行卡/地产付款。 */
class PaymentServiceTest {
    private StubGameDialogService dialogs;
    private PaymentService paymentService;
    private final List<String> logs = new ArrayList<>();
    private final List<String> statuses = new ArrayList<>();

    @BeforeAll
    static void initJavaFx() {
        StubGameDialogService.initJavaFxOnce();
    }

    @BeforeEach
    void setUp() {
        dialogs = new StubGameDialogService();
        logs.clear();
        statuses.clear();
        paymentService = new PaymentService(
                dialogs,
                logs::add,
                (message, error) -> statuses.add(message + "|" + error));
    }

    /** 非法参数（null、自己付自己、金额为 0）应返回 0。 */
    @Test
    void collectPaymentByChoiceReturnsZeroForInvalidInput() {
        Player collector = new Player("Collector");
        Player payer = new Player("Payer");
        payer.addBank(new MoneyCard("1M", "Money", 1));

        assertEquals(0, paymentService.collectPaymentByChoice(null, payer, 2, "Rent"));
        assertEquals(0, paymentService.collectPaymentByChoice(collector, null, 2, "Rent"));
        assertEquals(0, paymentService.collectPaymentByChoice(collector, collector, 2, "Rent"));
        assertEquals(0, paymentService.collectPaymentByChoice(collector, payer, 0, "Rent"));
    }

    /** 玩家选择银行卡支付时，应转移到收款方银行。 */
    @Test
    void collectPaymentByChoiceMovesChosenBankCard() {
        Player collector = new Player("Collector");
        Player payer = new Player("Payer");
        MoneyCard three = new MoneyCard("3M", "Money", 3);
        payer.addBank(three);
        dialogs.enqueueChoice(three);

        int paid = paymentService.collectPaymentByChoice(collector, payer, 3, "Rent");

        assertEquals(3, paid);
        assertEquals(3, collector.getBankTotalValue());
        assertEquals(0, payer.getBankTotalValue());
        assertFalse(logs.isEmpty());
    }

    /** 银行不够时，可继续选地产支付。 */
    @Test
    void collectPaymentByChoiceCanPayWithPropertyWhenBankIsInsufficient() {
        Player collector = new Player("Collector");
        Player payer = new Player("Payer");
        MoneyCard one = new MoneyCard("1M", "Money", 1);
        PropertyCard property = new PropertyCard("Pink 1", "Pink", Color.PINK, 2);
        payer.addBank(one);
        payer.addProperty(property);
        dialogs.enqueueChoice(one);
        dialogs.enqueueChoice(property);

        int paid = paymentService.collectPaymentByChoice(collector, payer, 3, "Debt");

        assertEquals(3, paid);
        assertEquals(1, collector.getBankTotalValue());
        assertTrue(collector.getAllProperties().contains(property));
    }

    /** 完整套里的地产不能作为支付选项。 */
    @Test
    void collectPaymentByChoiceDoesNotOfferCompleteSetProperty() {
        Player collector = new Player("Collector");
        Player payer = new Player("Payer");
        payer.addProperty(new PropertyCard("Brown 1", "Brown", Color.BROWN, 1));
        payer.addProperty(new PropertyCard("Brown 2", "Brown", Color.BROWN, 1));

        int paid = paymentService.collectPaymentByChoice(collector, payer, 5, "Rent");

        assertEquals(0, paid);
        assertTrue(payer.hasCompleteSet(Color.BROWN));
    }
}
