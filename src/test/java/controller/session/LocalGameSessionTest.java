package controller.session;

import engine.GameEngine;
import model.card.Card;
import model.card.MoneyCard;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 测试本地热座会话 {@link LocalGameSession} 对 {@link GameEngine} 的封装。 */
class LocalGameSessionTest {

    /** 开局应创建玩家、发 5 张起手牌，并标记对局为进行中。 */
    @Test
    void startNewGameCreatesPlayersAndDealsHands() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob", "Carol"));

        assertEquals(3, session.getPlayers().size());
        assertEquals("Alice", session.getCurrentPlayer().getName());
        for (Player player : session.getPlayers()) {
            assertEquals(GameEngine.START_HAND_SIZE, player.getHandSize());
        }
        assertTrue(session.isActive());
    }

    /** 当前玩家抽牌应增加 2 张手牌，且同回合不能重复抽。 */
    @Test
    void drawForCurrentPlayerAddsTwoCards() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player current = session.getCurrentPlayer();
        int before = current.getHandSize();

        assertTrue(session.drawForCurrentPlayer());
        assertEquals(before + 2, current.getHandSize());
        assertFalse(session.drawForCurrentPlayer());
    }

    /** 弃牌后手牌减少，且卡牌进入弃牌堆。 */
    @Test
    void discardFromHandRemovesSelectedCard() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player current = session.getCurrentPlayer();
        Card card = current.getHand().getFirst();

        assertTrue(session.discardFromHand(current, card));
        assertFalse(current.getHand().contains(card));
        assertTrue(session.getEngine().getDiscardPile().size() > 0);
    }

    /** 记录 3 次出牌后，本回合不能再出牌。 */
    @Test
    void recordCardPlayedTracksTurnLimit() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        session.drawForCurrentPlayer();

        session.recordCardPlayed();
        session.recordCardPlayed();
        session.recordCardPlayed();

        assertFalse(session.getEngine().canPlayCard());
        assertTrue(session.getEngine().isTurnOver());
    }

    /** nextTurn 应切换到下一个玩家。 */
    @Test
    void nextTurnSwitchesCurrentPlayer() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player first = session.getCurrentPlayer();

        session.nextTurn();

        assertNotEquals(first, session.getCurrentPlayer());
        assertEquals("Bob", session.getCurrentPlayer().getName());
    }

    /** 未满 3 套完整地产时不应判胜。 */
    @Test
    void checkWinIsFalseUntilThreeCompleteSets() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player player = session.getPlayers().getFirst();

        player.addProperty(new PropertyCard("Brown 1", "Brown", Color.BROWN, 1));
        player.addProperty(new PropertyCard("Brown 2", "Brown", Color.BROWN, 1));

        assertFalse(session.checkWin(player));
    }

    /** setGameOver 后 isActive 应为 false。 */
    @Test
    void setGameOverStopsActiveSession() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));

        session.setGameOver(true);

        assertFalse(session.isActive());
        assertTrue(session.getEngine().isGameOver());
    }

    /** 手牌 ≤7 才能结束回合。 */
    @Test
    void canEndTurnRequiresHandSizeWithinLimit() {
        LocalGameSession session = new LocalGameSession();
        session.startNewGame(List.of("Alice", "Bob"));
        Player current = session.getCurrentPlayer();

        assertTrue(session.canEndTurn(current));

        while (current.getHandSize() <= GameEngine.MAX_HAND_SIZE) {
            current.draw(new MoneyCard("1M", "Money", 1));
        }
        assertFalse(session.canEndTurn(current));
    }
}
