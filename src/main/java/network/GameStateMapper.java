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

public final class GameStateMapper {
    private GameStateMapper() {
    }

    public static GameStateDto buildForSeat(GameEngine engine, int seat, List<String> logLines) {
        GameStateDto state = new GameStateDto();
        state.currentPlayerIndex = engine.getCurrentPlayerIndex();
        state.hasDrawnThisTurn = engine.hasDrawnThisTurn();
        state.remainingPlays = engine.getRemainingPlays();
        state.gameOver = engine.isGameOver();
        state.drawPileSize = engine.getDeck().size();
        state.discardPileSize = engine.getDiscardPile().size();
        state.logLines = List.copyOf(logLines);

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

        if (seat >= 0 && seat < engine.getPlayers().size()) {
            Player me = engine.getPlayers().get(seat);
            state.myHand = CardMapper.toDtos(me.getHand());
            state.myBank = CardMapper.toDtos(me.getBank());
        }
        return state;
    }
}
