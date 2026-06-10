package network;

import engine.GameEngine;
import model.card.Card;
import model.card.PropertyCard;
import model.player.Player;
import network.protocol.CardDto;
import network.protocol.GameStateDto;
import network.protocol.PlayerViewDto;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds per-client game snapshots from the authoritative server {@link GameEngine}.
 * <p>
 * Public fields are visible to every client; {@code myHand} and {@code myBank} are filled
 * only for the requested {@code seat} so opponents never receive full hand contents.
 */
public final class GameStateMapper {
    private GameStateMapper() {
    }

    /**
     * Creates a {@link GameStateDto} tailored to one connected client.
     *
     * @param engine   authoritative game engine on the server
     * @param seat     receiving client's seat index (0-based)
     * @param logLines session log lines maintained by {@link network.server.GameSession}
     * @return snapshot safe to send to the client at {@code seat}
     */
    public static GameStateDto buildForSeat(GameEngine engine, int seat, List<String> logLines) {
        GameStateDto state = new GameStateDto();

        // Global turn and pile info — identical for every client.
        state.currentPlayerIndex = engine.getCurrentPlayerIndex();
        state.hasDrawnThisTurn = engine.hasDrawnThisTurn();
        state.remainingPlays = engine.getRemainingPlays();
        state.gameOver = engine.isGameOver();
        state.drawPileSize = engine.getDeck().size();
        state.discardPileSize = engine.getDiscardPile().size();
        state.logLines = List.copyOf(logLines);

        // Public view of every player (hand count only, not card identities).
        for (int i = 0; i < engine.getPlayers().size(); i++) {
            Player player = engine.getPlayers().get(i);
            PlayerViewDto view = new PlayerViewDto();
            view.seat = i;
            view.name = player.getName();
            view.handSize = player.getHandSize();
            view.bankTotal = player.getBankTotalValue();
            for (PropertyCard property : player.getAllProperties()) {
                view.properties.add(CardMapper.toDto(property));
            }
            state.players.add(view);

            if (state.gameOver && state.winnerName == null && engine.checkWin(player)) {
                state.winnerName = player.getName();
            }
        }

        // Private data visible only to the receiving seat.
        if (seat >= 0 && seat < engine.getPlayers().size()) {
            Player me = engine.getPlayers().get(seat);
            state.myHand = CardMapper.toDtos(me.getHand());
            state.myBank = CardMapper.toDtos(me.getBank());
        }
        return state;
    }
}
