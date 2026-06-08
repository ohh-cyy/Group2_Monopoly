package controller.gameplay;

import model.card.WildpropertyCard;
import model.card.actionCard.DebtCollector;
import model.enums.CardType;
import model.enums.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testsupport.StubGameDialogService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StandardCardPlayPromptsTest {
    private StubGameDialogService dialogs;
    private StandardCardPlayPrompts prompts;

    @BeforeAll
    static void initJavaFx() {
        StubGameDialogService.initJavaFxOnce();
    }

    @BeforeEach
    void setUp() {
        dialogs = new StubGameDialogService();
        prompts = new StandardCardPlayPrompts(dialogs);
    }

    @Test
    void promptActionCardChoiceReturnsUseEffect() {
        DebtCollector card = new DebtCollector("Debt Collector", "Collect 5M", CardType.ACTION);
        dialogs.enqueueButtonText("Use Effect");

        Optional<ActionPlayChoice> choice = prompts.promptActionCardChoice(card);

        assertTrue(choice.isPresent());
        assertEquals(ActionPlayChoice.USE_EFFECT, choice.get());
    }

    @Test
    void promptWildPropertyChoiceReturnsDepositBank() {
        WildpropertyCard wild = new WildpropertyCard(
                "Dual", "Wild", 2, List.of(Color.ORANGE, Color.PINK), true);
        dialogs.enqueueButtonText("Deposit to Bank (2M)");

        Optional<ActionPlayChoice> choice = prompts.promptWildPropertyChoice(wild);

        assertTrue(choice.isPresent());
        assertEquals(ActionPlayChoice.DEPOSIT_BANK, choice.get());
    }
}
