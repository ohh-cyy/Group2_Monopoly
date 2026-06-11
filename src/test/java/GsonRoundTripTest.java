import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import model.player.Player;
import network.CardMapper;
import network.GameStateMapper;
import network.JsonUtil;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;
import network.server.GameSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 测试联机 JSON 消息序列化/反序列化 {@link network.JsonUtil}。 */
class GsonRoundTripTest {
    /** GAME_STARTED 消息往返后，手牌与玩家信息应完整保留。 */
    @Test
    void gameStartedRoundTrip() {
        List<Player> players = List.of(new Player("A"), new Player("B"));
        GameEngine engine = new GameEngine(new ArrayList<>(players), new Deck(DeckFactory.createFullDeck()));
        engine.startGame();
        var state = GameStateMapper.buildForSeat(engine, 0, List.of("[00:00:00] start"));
        ServerMessage msg = new ServerMessage();
        msg.type = "GAME_STARTED";
        msg.state = state;

        ServerMessage parsed = JsonUtil.parseServer(JsonUtil.toJson(msg));
        assertNotNull(parsed.state);
        assertEquals(2, parsed.state.players.size());
        assertFalse(parsed.state.myHand.isEmpty());
        assertNotNull(parsed.state.myHand.get(0).type);
        assertNotNull(CardMapper.fromDto(parsed.state.myHand.get(0)));
    }

    @Test
    void emojiEventRoundTripPreservesReactionAndSeat() {
        ServerMessage message = new ServerMessage();
        message.type = MessageTypes.EMOJI;
        message.seat = 4;
        message.emoji = "🎲";

        ServerMessage parsed = JsonUtil.parseServer(JsonUtil.toJson(message));

        assertEquals(MessageTypes.EMOJI, parsed.type);
        assertEquals(4, parsed.seat);
        assertEquals("🎲", parsed.emoji);
        assertEquals(5, GameSession.MAX_PLAYERS);
    }
}
