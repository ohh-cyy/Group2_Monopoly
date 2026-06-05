import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import model.player.Player;
import network.CardMapper;
import network.GameStateMapper;
import network.JsonUtil;
import network.protocol.ServerMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GsonRoundTripTest {
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
}
