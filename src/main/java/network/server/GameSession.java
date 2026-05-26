package network.server;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import model.card.*;
import model.card.actionCard.ActionCard;
import model.enums.Color;
import model.player.Player;
import network.CardMapper;
import network.GameStateMapper;
import network.protocol.ClientMessage;
import network.protocol.GameStateDto;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.util.ArrayList;
import java.util.List;

/** 一局联机游戏：权威状态与命令处理 */
public class GameSession {
    /** 调试阶段：2 人即可开局；改回 4 即可恢复四人联机 */
    public static final int MAX_PLAYERS = 2;

    private final GameEngine engine;
    private final ClientHandler[] seats = new ClientHandler[MAX_PLAYERS];
    private final List<String> pendingEvents = new ArrayList<>();

    public GameSession(List<String> playerNames) {
        List<Player> players = new ArrayList<>();
        for (String name : playerNames) {
            players.add(new Player(name));
        }
        Deck deck = new Deck(DeckFactory.createFullDeck());
        engine = new GameEngine(players, deck);
        engine.startGame();
        pendingEvents.add("游戏开始！每人 5 张起手牌。");
        pendingEvents.add("当前回合: " + engine.getCurrentPlayer().getName());
    }

    public synchronized void bindSeat(int seat, ClientHandler handler) {
        seats[seat] = handler;
    }

    public synchronized ServerMessage handle(int seat, ClientMessage message) {
        if (message == null || message.type == null) {
            return error("无效消息");
        }
        return switch (message.type) {
            case MessageTypes.DRAW -> handleDraw(seat);
            case MessageTypes.PLAY_CARD -> handlePlayCard(seat, message);
            case MessageTypes.END_TURN -> handleEndTurn(seat);
            case MessageTypes.SYNC -> buildStateMessage(seat);
            default -> error("未知命令: " + message.type);
        };
    }

    public synchronized void broadcastState() {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            ClientHandler handler = seats[i];
            if (handler != null && handler.isConnected()) {
                handler.send(buildStateMessage(i));
            }
        }
        pendingEvents.clear();
    }

    public synchronized ServerMessage buildStateMessage(int seat) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.STATE;
        msg.yourSeat = seat;
        msg.state = GameStateMapper.build(engine, seat);
        msg.events.addAll(pendingEvents);
        return msg;
    }

    private ServerMessage handleDraw(int seat) {
        if (engine.isGameOver()) {
            return error("游戏已结束");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("还没轮到你");
        }
        if (!engine.canDrawCards()) {
            return error("本回合已经抽过牌");
        }
        Player player = engine.getCurrentPlayer();
        if (!engine.drawCardsForCurrentPlayer()) {
            return error("抽牌失败");
        }
        pendingEvents.add(player.getName() + " 抽了 2 张牌");
        broadcastState();
        return ok("已抽牌");
    }

    private ServerMessage handleEndTurn(int seat) {
        if (engine.isGameOver()) {
            return error("游戏已结束");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("还没轮到你");
        }
        Player player = engine.getCurrentPlayer();
        engine.nextTurn();
        pendingEvents.add(player.getName() + " 结束回合 → " + engine.getCurrentPlayer().getName());
        broadcastState();
        return ok("回合已结束");
    }

    private ServerMessage handlePlayCard(int seat, ClientMessage message) {
        if (engine.isGameOver()) {
            return error("游戏已结束");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("还没轮到你");
        }
        if (!engine.canPlayCard()) {
            return error("本回合已出满 3 张牌");
        }
        if (message.cardId == null || message.cardId.isBlank()) {
            return error("未指定卡牌");
        }

        Player player = engine.getCurrentPlayer();
        Card card = player.findInHandById(message.cardId);
        if (card == null) {
            return error("手牌中没有这张牌");
        }

        String mode = message.mode != null ? message.mode.toUpperCase() : "PLAY";

        try {
            if (card instanceof MoneyCard money) {
                money.use(player, engine);
                player.removeFromHand(money);
                pendingEvents.add(player.getName() + " 打出金钱牌 " + money.getName());
            } else if (card instanceof PropertyCard property && !(card instanceof WildpropertyCard)) {
                property.use(player, engine);
                player.removeFromHand(property);
                pendingEvents.add(player.getName() + " 打出地产 " + property.getName());
            } else if (card instanceof WildpropertyCard wild) {
                if ("BANK".equals(mode)) {
                    if (!wild.isBankable()) {
                        return error("这张万能卡不能存银行");
                    }
                    player.removeFromHand(wild);
                    wild.depositToBank(player);
                    pendingEvents.add(player.getName() + " 将万能地产存入银行");
                } else {
                    Color color = CardMapper.parseColor(message.color);
                    if (color == null || !wild.getAvailableColors().contains(color)) {
                        return error("请选择有效的万能地产颜色");
                    }
                    wild.setChosenColor(color);
                    player.removeFromHand(wild);
                    wild.use(player, engine);
                    pendingEvents.add(player.getName() + " 打出万能地产作为 " + color);
                }
            } else if (card instanceof ActionCard action) {
                if ("BANK".equals(mode)) {
                    player.removeFromHand(action);
                    action.depositToBank(player);
                    pendingEvents.add(player.getName() + " 将「" + action.getName() + "」存入银行");
                } else {
                    return error("联机 MVP：行动牌效果暂未支持，请选择「存入银行」");
                }
            } else {
                return error("不支持的卡牌类型");
            }
        } catch (Exception ex) {
            return error("出牌失败: " + ex.getMessage());
        }

        finishPlayStep(player);
        return ok("出牌成功");
    }

    private void finishPlayStep(Player player) {
        engine.recordCardPlayed();
        if (engine.checkWin(player)) {
            engine.setGameOver(true);
            pendingEvents.add(player.getName() + " 获胜！");
        } else if (engine.isTurnOver()) {
            engine.nextTurn();
            pendingEvents.add("已出 3 张牌，自动换到 " + engine.getCurrentPlayer().getName());
        }
        broadcastState();
    }

    private ServerMessage ok(String message) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.STATE;
        msg.message = message;
        return msg;
    }

    private ServerMessage error(String message) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.ERROR;
        msg.message = message;
        return msg;
    }
}
