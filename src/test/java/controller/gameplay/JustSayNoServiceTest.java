package controller.gameplay;

import engine.Deck;
import engine.GameEngine;
import model.card.actionCard.JustSayNo;
import model.card.actionCard.SlyDeal;
import model.enums.CardType;
import model.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testsupport.StubGameDialogService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JustSayNoServiceTest {
    private StubGameDialogService dialogs;
    private JustSayNoService justSayNoService;
    private final List<String> logs = new ArrayList<>();

    @BeforeAll
    static void initJavaFx() {
        StubGameDialogService.initJavaFxOnce();
    }

    @BeforeEach
    void setUp() {
        dialogs = new StubGameDialogService();
        logs.clear();
        justSayNoService = new JustSayNoService(dialogs, logs::add, (msg, err) -> { });
    }

    @Test
    void respondReturnsFalseWhenDefenderHasNoJustSayNo() {
        Player defender = new Player("Defender");
        Player attacker = new Player("Attacker");
        GameEngine engine = engineWith(defender, attacker);

        boolean blocked = justSayNoService.respond(defender, attacker, "Sly Deal", engine);

        assertFalse(blocked);
    }

    @Test
    void respondBlocksActionWhenDefenderPlaysJustSayNo() {
        Player defender = new Player("Defender");
        Player attacker = new Player("Attacker");
        defender.draw(new JustSayNo("JSN", "Block", CardType.ACTION));
        GameEngine engine = engineWith(defender, attacker);
        dialogs.enqueueButtonText("Play Just Say No");

        boolean blocked = justSayNoService.respond(defender, attacker, "Sly Deal", engine);

        assertTrue(blocked);
        assertTrue(defender.getHand().stream().noneMatch(JustSayNo.class::isInstance));
        assertEquals(1, engine.getDiscardPile().size());
    }

    @Test
    void respondAllowsActionWhenDefenderDeclinesJustSayNo() {
        Player defender = new Player("Defender");
        Player attacker = new Player("Attacker");
        defender.draw(new JustSayNo("JSN", "Block", CardType.ACTION));
        GameEngine engine = engineWith(defender, attacker);
        dialogs.enqueueButtonText("Allow Effect");

        boolean blocked = justSayNoService.respond(defender, attacker, "Debt Collector", engine);

        assertFalse(blocked);
        assertEquals(1, defender.getHandSize());
    }

    private static GameEngine engineWith(Player first, Player second) {
        return new GameEngine(List.of(first, second), new Deck(List.of(
                new SlyDeal("Sly Deal", "Steal property", CardType.ACTION)
        )));
    }
}
