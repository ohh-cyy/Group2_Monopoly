package controller.gameplay;

import controller.session.LocalGameSession;
import engine.GameEngine;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.card.actionCard.DebtCollector;
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

/** 测试本地出牌服务 {@link LocalCardPlayService}。 */
class LocalCardPlayServiceTest {
    private StubGameDialogService dialogs;
    private LocalCardPlayService cardPlayService;
    private LocalGameSession session;
    private final List<String> logs = new ArrayList<>();

    @BeforeAll
    static void initJavaFx() {
        StubGameDialogService.initJavaFxOnce();
    }

    @BeforeEach
    void setUp() {
        dialogs = new StubGameDialogService();
        logs.clear();
        PaymentService payments = new PaymentService(dialogs, logs::add, (msg, err) -> { });
        JustSayNoService justSayNo = new JustSayNoService(dialogs, logs::add, (msg, err) -> { });
        ActionEffectResolver actionResolver = new ActionEffectResolver(
                dialogs, payments, justSayNo, logs::add, (msg, err) -> { });
        cardPlayService = new LocalCardPlayService(
                dialogs, actionResolver, logs::add, (msg, err) -> { });

        session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        session.drawForCurrentPlayer();
    }

    @Test
    void playSimpleCardAddsPropertyToPlayerBoard() {
        Player player = session.getCurrentPlayer();
        PropertyCard property = new PropertyCard("Pink 1", "Pink", Color.PINK, 1);
        player.draw(property);

        CardPlayOutcome outcome = cardPlayService.playSimpleCard(session, player, property);

        assertEquals(ActionEffectResult.SUCCESS, outcome.result);
        assertFalse(outcome.depositedToBank);
        assertTrue(player.getPropertiesByColor(Color.PINK).contains(property));
        assertFalse(player.getHand().contains(property));
    }

    @Test
    void playActionCardCanDepositToBank() {
        Player player = session.getCurrentPlayer();
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);
        player.draw(debtCollector);
        dialogs.enqueueButtonText("Deposit to Bank (3M)");

        CardPlayOutcome outcome = cardPlayService.play(session, player, debtCollector);

        assertEquals(ActionEffectResult.SUCCESS, outcome.result);
        assertTrue(outcome.depositedToBank);
        assertEquals(3, player.getBankTotalValue());
        assertFalse(player.getHand().contains(debtCollector));
    }

    @Test
    void playActionCardCancelledWhenUserDismissesPrompt() {
        Player player = session.getCurrentPlayer();
        DebtCollector debtCollector = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);
        player.draw(debtCollector);
        dialogs.enqueueButtonText("Cancel");

        CardPlayOutcome outcome = cardPlayService.play(session, player, debtCollector);

        assertEquals(ActionEffectResult.CANCELLED, outcome.result);
        assertTrue(player.getHand().contains(debtCollector));
    }

    @Test
    void playSimpleCardFailsWhenColorSetAlreadyComplete() {
        Player player = session.getCurrentPlayer();
        player.addProperty(new PropertyCard("Brown 1", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Brown 2", "Brown", Color.BROWN, 1));
        PropertyCard extraBrown = new PropertyCard("Brown 3", "Brown", Color.BROWN, 1);
        player.draw(extraBrown);

        CardPlayOutcome outcome = cardPlayService.playSimpleCard(session, player, extraBrown);

        assertEquals(ActionEffectResult.FAILED, outcome.result);
        assertTrue(player.getHand().contains(extraBrown));
    }
}
