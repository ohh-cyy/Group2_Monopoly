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
 * 从服务端权威 {@link GameEngine} 构建各客户端游戏快照。
 * <p>
 * 公开字段对所有客户端可见；{@code myHand} 与 {@code myBank} 仅填充请求的 {@code seat}，
 * 对手不会收到完整手牌内容。
 */
public final class GameStateMapper {
    private GameStateMapper() {
    }

    /**
     * 为一名已连接客户端创建 {@link GameStateDto}。
     *
     * @param engine   服务端权威游戏引擎
     * @param seat     接收客户端的座位索引（从 0 开始）
     * @param logLines 由 {@link network.server.GameSession} 维护的会话日志行
     * @return 可安全发送给 {@code seat} 客户端的快照
     */
    public static GameStateDto buildForSeat(GameEngine engine, int seat, List<String> logLines) {
        GameStateDto state = new GameStateDto();

        // 全局回合与牌堆信息——各客户端相同。
        state.currentPlayerIndex = engine.getCurrentPlayerIndex();
        state.hasDrawnThisTurn = engine.hasDrawnThisTurn();
        state.remainingPlays = engine.getRemainingPlays();
        state.gameOver = engine.isGameOver();
        state.drawPileSize = engine.getDeck().size();
        state.discardPileSize = engine.getDiscardPile().size();
        state.logLines = List.copyOf(logLines);

        // 所有玩家的公开视图（仅手牌数量，不含卡牌身份）。
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

        // 仅接收座位可见的私有数据。
        if (seat >= 0 && seat < engine.getPlayers().size()) {
            Player me = engine.getPlayers().get(seat);
            state.myHand = CardMapper.toDtos(me.getHand());
            state.myBank = CardMapper.toDtos(me.getBank());
        }
        return state;
    }
}
