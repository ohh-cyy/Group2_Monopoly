package network;

import engine.GameEngine;
import model.card.Card;
import model.player.Player;
import network.protocol.CardDto;
import network.protocol.GameStateDto;
import network.protocol.PlayerViewDto;

import java.util.ArrayList;
import java.util.List;

/** 将服务端 GameEngine 转为可广播的 JSON 快照 */
public final class GameStateMapper {

    private GameStateMapper() {
    }

    public static GameStateDto build(GameEngine engine, int viewerSeat) {
        GameStateDto state = new GameStateDto();
        state.yourSeat = viewerSeat;
        state.currentPlayerIndex = engine.getCurrentPlayerIndex();
        state.hasDrawnThisTurn = engine.hasDrawnThisTurn();
        state.remainingPlays = engine.getRemainingPlays();
        state.gameOver = engine.isGameOver();
        state.drawPileSize = engine.getDeck().size();
        state.discardPileSize = engine.getDiscardPile().size();

        List<Player> players = engine.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            state.players.add(toPlayerView(players.get(i), i, i == viewerSeat));
        }

        if (state.gameOver) {
            for (Player player : players) {
                if (engine.checkWin(player)) {
                    state.winnerName = player.getName();
                    break;
                }
            }
        }
        return state;
    }

    private static PlayerViewDto toPlayerView(Player player, int seat, boolean you) {
        PlayerViewDto view = new PlayerViewDto();
        view.seat = seat;
        view.name = player.getName();
        view.handSize = player.getHandSize();
        view.bankTotal = player.getBankTotalValue();
        view.propertyCount = player.getAllProperties().size();
        view.you = you;

        view.bank = cardsToDto(player.getBank());
        view.properties = cardsToDto(new ArrayList<>(player.getAllProperties()));

        if (you) {
            view.hand = cardsToDto(player.getHand());
        }
        return view;
    }

    private static List<CardDto> cardsToDto(List<Card> cards) {
        List<CardDto> dtos = new ArrayList<>();
        for (Card card : cards) {
            dtos.add(CardMapper.toDto(card));
        }
        return dtos;
    }
}
