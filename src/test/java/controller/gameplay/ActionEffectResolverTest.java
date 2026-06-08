package controller.gameplay;

import engine.Deck;
import engine.GameEngine;
import engine.PropertyRules;
import model.card.MoneyCard;
import model.card.actionCard.House;
import model.card.actionCard.JustSayNo;
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

class ActionEffectResolverTest {
    private StubGameDialogService dialogs;
    private ActionEffectResolver resolver;
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
        resolver = new ActionEffectResolver(dialogs, payments, justSayNo, logs::add, (msg, err) -> { });
    }

    @Test
    void resolveRejectsProactiveJustSayNo() {
        Player player = new Player("Player");
        GameEngine engine = engineWith(player);

        ActionEffectResult result = resolver.resolve(engine, player, new JustSayNo("JSN", "Block", CardType.ACTION));

        assertEquals(ActionEffectResult.CANCELLED, result);
    }

    @Test
    void resolvePassGoDrawsExtraCards() {
        Player player = new Player("Player");
        GameEngine engine = engineWith(player);
        PassGoCard passGo = new PassGoCard("Pass Go", "Draw 2", CardType.ACTION);

        ActionEffectResult result = resolver.resolve(engine, player, passGo);

        assertEquals(ActionEffectResult.SUCCESS, result);
        assertEquals(2, player.getHandSize());
    }

    @Test
    void resolveSimpleActionCardUsesDefaultBehaviour() {
        Player player = new Player("Player");
        GameEngine engine = engineWith(player);
        SimpleActionCard action = new SimpleActionCard("No-op", "Does nothing", 1);

        ActionEffectResult result = resolver.resolve(engine, player, action);

        assertEquals(ActionEffectResult.SUCCESS, result);
    }

    @Test
    void resolveHouseAddsImprovementToChosenCompleteSet() {
        Player player = new Player("Player");
        addCompleteSet(player, Color.BROWN);
        GameEngine engine = engineWith(player);
        House house = new House("House", "House bonus", CardType.ACTION);
        dialogs.enqueueChoice(Color.BROWN);

        ActionEffectResult result = resolver.resolve(engine, player, house);

        assertEquals(ActionEffectResult.SUCCESS, result);
        assertTrue(PropertyRules.hasHouse(player, Color.BROWN));
    }

    private static void addCompleteSet(Player player, Color color) {
        for (int i = 1; i <= color.getSetSize(); i++) {
            player.addProperty(new model.card.PropertyCard(
                    color + " " + i, color + " property", color, i));
        }
    }

    private static GameEngine engineWith(Player player) {
        Deck deck = new Deck(List.of(
                new MoneyCard("1M", "Money", 1),
                new MoneyCard("2M", "Money", 2),
                new MoneyCard("3M", "Money", 3)
        ));
        return new GameEngine(List.of(player, new Player("Opponent")), deck);
    }
}
