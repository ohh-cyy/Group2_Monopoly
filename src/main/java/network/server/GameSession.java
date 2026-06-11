package network.server;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import model.card.Card;
import model.player.Player;
import network.GameStateMapper;
import network.protocol.ClientMessage;
import network.protocol.InteractionPromptDto;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 权威联机对局：持有服务端 {@link GameEngine}，
 * 校验命令、广播按座位的 STATE 快照，并运行计时器/再来一局逻辑。
 */
public class GameSession {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;
    public static final int TURN_TIME_SECONDS = 60;

    /** 各座位的网络连接；未使用时为 null。 */
    private final ClientHandler[] seats = new ClientHandler[MAX_PLAYERS];
    private final String[] names = new String[MAX_PLAYERS];
    private final List<String> logLines = new ArrayList<>();
    private final GameSessionActions actions = new GameSessionActions();
    private final SessionTurnClock turnClock = new SessionTurnClock(this::handleTurnTimeout);
    private final SessionRematchManager rematch = new SessionRematchManager(MAX_PLAYERS);

    private int playerCount;
    private GameEngine engine;

    /** 等待 PROMPT/RESPOND 交互（租金、JSN、支付）时非 null。 */
    private PendingActionResolution pendingResolution;

    /** Double the Rent 组合应消耗两次出牌时为 true。 */
    private boolean pendingUsesTwoPlays;

    /** 比赛开始前将大厅座位绑定到活跃连接。 */
    public synchronized void bindPlayer(int seat, ClientHandler handler, String name) {
        if (seat < 0 || seat >= MAX_PLAYERS) {
            return;
        }
        seats[seat] = handler;
        names[seat] = name;
        playerCount = Math.max(playerCount, seat + 1);
    }

    /** 开始或重新开始对局：新引擎、新牌堆、回合计时器、初始 STATE 广播。 */
    public synchronized void startGame(int activePlayers) {
        List<Player> players = new ArrayList<>();
        playerCount = activePlayers;
        for (int i = 0; i < activePlayers; i++) {
            players.add(new Player(names[i]));
        }
        engine = new GameEngine(players, new Deck(DeckFactory.createFullDeck()));
        engine.startGame();
        pendingResolution = null;
        pendingUsesTwoPlays = false;
        logLines.clear();
        rematch.clear();
        appendLog("=== Game started with " + activePlayers + " players ===");
        startTurnClockLocked();
        broadcastState();
    }

    /** 将对局内客户端命令分发给 {@link GameSessionActions}。 */
    public synchronized ServerMessage handleMessage(int seat, ClientMessage message) {
        if (message == null || message.type == null) {
            return error("Invalid message");
        }
        return switch (message.type) {
            case MessageTypes.DRAW -> actions.handleDraw(this, seat);
            case MessageTypes.PLAY_CARD -> actions.handlePlayCard(this, seat, message);
            case MessageTypes.RECOLOR_WILD -> actions.handleRecolorWild(this, seat, message);
            case MessageTypes.DISCARD_CARD -> actions.handleDiscardCard(this, seat, message);
            case MessageTypes.END_TURN -> actions.handleEndTurn(this, seat);
            case MessageTypes.SYNC -> buildStateMessage(seat);
            case MessageTypes.RESPOND -> actions.handleRespond(this, seat, message);
            case MessageTypes.REMATCH_VOTE -> handleRematchVote(seat, message);
            case MessageTypes.SEND_EMOJI -> actions.handleEmoji(this, seat, message);
            case MessageTypes.PAUSE_GAME -> handlePauseGame();
            case MessageTypes.RESUME_GAME -> handleResumeGame();
            default -> error("Unknown command: " + message.type);
        };
    }

    /**
     * 向一名玩家发送 PROMPT（含 state + prompt 载荷），
     * 并向其余玩家发送 STATE 更新。
     */
    public synchronized void sendPrompt(int seat, InteractionPromptDto prompt) {
        if (seat < 0 || seat >= playerCount || seats[seat] == null) {
            return;
        }
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.PROMPT;
        msg.prompt = prompt;
        msg.state = GameStateMapper.buildForSeat(engine, seat, logLines);
        enrichTurnClockState(msg.state);
        rematch.enrichState(msg.state, seat, playerCount);
        seats[seat].send(msg);
        broadcastStateExceptPrompt(seat);
    }

    private void broadcastStateExceptPrompt(int promptedSeat) {
        for (int i = 0; i < playerCount; i++) {
            if (i == promptedSeat) {
                continue;
            }
            ClientHandler handler = seats[i];
            if (handler != null && handler.isConnected()) {
                handler.send(buildStateMessage(i));
            }
        }
    }

    /** 多步行动（租金/JSN/支付）完成时调用；记录出牌次数并广播。 */
    public synchronized void onActionResolutionComplete(boolean success) {
        pendingResolution = null;
        if (engine == null) {
            return;
        }
        Player player = engine.getCurrentPlayer();
        engine.recordCardsPlayed(pendingUsesTwoPlays ? 2 : 1);
        pendingUsesTwoPlays = false;
        if (engine.checkWin(player)) {
            markGameWon(player);
        } else if (engine.isTurnOver()) {
            notifyPlaysExhausted(player);
        }
        broadcastState();
    }

    /** 未进入 pending 解析的出牌成功后，检查获胜或三次出牌已用完。 */
    void afterSuccessfulPlay(Player player) {
        if (engine.checkWin(player)) {
            markGameWon(player);
        } else if (engine.isTurnOver()) {
            notifyPlaysExhausted(player);
        }
    }

    private void notifyPlaysExhausted(Player player) {
        if (player.getHandSize() > GameEngine.MAX_HAND_SIZE) {
            appendLog(player.getName() + " must discard down to "
                    + GameEngine.MAX_HAND_SIZE + " cards before ending turn");
            return;
        }
        appendLog(player.getName() + " used all 3 plays — end turn when ready");
    }

    /** 向每名已连接玩家推送按座位的 STATE 消息。 */
    public synchronized void broadcastState() {
        for (int i = 0; i < playerCount; i++) {
            ClientHandler handler = seats[i];
            if (handler != null && handler.isConnected()) {
                handler.send(buildStateMessage(i));
            }
        }
    }

    /** 通过 {@link GameStateMapper} 为一名座位构建 STATE，并附加计时器/再来一局元数据。 */
    public synchronized ServerMessage buildStateMessage(int seat) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.STATE;
        if (engine != null) {
            msg.state = GameStateMapper.buildForSeat(engine, seat, logLines);
            enrichTurnClockState(msg.state);
            rematch.enrichState(msg.state, seat, playerCount);
        }
        return msg;
    }

    private ServerMessage handlePauseGame() {
        if (engine == null || engine.isGameOver()) {
            return ok("Game paused");
        }
        turnClock.pause();
        broadcastState();
        return ok("Game paused");
    }

    private ServerMessage handleResumeGame() {
        if (engine == null || engine.isGameOver()) {
            return ok("Game resumed");
        }
        turnClock.resume();
        broadcastState();
        return ok("Game resumed");
    }

    private void enrichTurnClockState(network.protocol.GameStateDto state) {
        state.gamePaused = turnClock.isPaused();
        if (state.gamePaused) {
            state.pausedTurnSecondsRemaining = turnClock.frozenSecondsRemaining();
            state.turnDeadlineEpochMillis = 0;
        } else {
            state.turnDeadlineEpochMillis = turnClock.deadlineEpochMillis();
        }
    }

    private ServerMessage handleRematchVote(int seat, ClientMessage message) {
        if (!rematch.isRematchOpen() || engine == null || !engine.isGameOver()) {
            return error("Rematch not available");
        }
        SessionRematchManager.VoteOutcome outcome = rematch.recordVote(
                seat, playerCount, Boolean.TRUE.equals(message.acceptRematch), names[seat]);
        return switch (outcome.kind()) {
            case ERROR -> error(outcome.logLine());
            case DECLINED -> {
                appendLog(outcome.logLine());
                broadcastState();
                yield ok("Rematch declined");
            }
            case WAITING -> {
                appendLog(outcome.logLine());
                broadcastState();
                yield ok("Vote recorded");
            }
            case RESTART -> {
                appendLog(outcome.logLine());
                startGame(outcome.restartPlayerCount());
                yield ok("New game started");
            }
        };
    }

    private void markGameWon(Player winner) {
        engine.setGameOver(true);
        turnClock.cancel();
        appendLog("=== " + winner.getName() + " wins! ===");
        rematch.open();
    }

    void advanceTurnLocked() {
        if (engine == null || engine.isGameOver()) {
            return;
        }
        engine.nextTurn();
        startTurnClockLocked();
    }

    private void startTurnClockLocked() {
        if (engine == null || engine.isGameOver()) {
            turnClock.cancel();
            return;
        }
        turnClock.start(TURN_TIME_SECONDS);
    }

    /** 回合计时器触发时跳过当前玩家（除非仍有待处理提示）。 */
    private void handleTurnTimeout() {
        synchronized (this) {
            if (engine == null || engine.isGameOver()) {
                return;
            }
            if (pendingResolution != null) {
                startTurnClockLocked();
                broadcastState();
                return;
            }
            Player skipped = engine.getCurrentPlayer();
            List<Card> discarded = engine.enforceHandSizeLimit(skipped);
            for (Card card : discarded) {
                appendLog(skipped.getName() + " auto-discarded " + card.getName() + " (hand size limit)");
            }
            appendLog(skipped.getName() + " ran out of time and was skipped");
            engine.nextTurn();
            startTurnClockLocked();
            broadcastState();
        }
    }

    void broadcastEmoji(int seat, String emoji) {
        ServerMessage reaction = new ServerMessage();
        reaction.type = MessageTypes.EMOJI;
        reaction.seat = seat;
        reaction.emoji = emoji;
        for (int i = 0; i < playerCount; i++) {
            ClientHandler handler = seats[i];
            if (handler != null && handler.isConnected()) {
                handler.send(reaction);
            }
        }
    }

    public synchronized void shutdown() {
        turnClock.shutdown();
    }

    /** 向会话日志追加带时间戳的行，包含在每条 STATE 中。 */
    void appendLog(String line) {
        logLines.add("[" + java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + line);
    }

    boolean playToBank(Player player, Card card) {
        return actions.playToBank(this, player, card);
    }

    /** 权威引擎；仅在 {@link #startGame(int)} 之后有效。 */
    GameEngine engine() {
        return engine;
    }

    /** 本会话中活跃真人座位数。 */
    int playerCount() {
        return playerCount;
    }

    /** 与 {@link GameStateMapper} 共享的可变会话日志。 */
    List<String> logLines() {
        return logLines;
    }

    /** 等待客户端 PROMPT 应答时非 null。 */
    PendingActionResolution pendingResolution() {
        return pendingResolution;
    }

    void setPendingResolution(PendingActionResolution pendingResolution) {
        this.pendingResolution = pendingResolution;
    }

    /** 为 true 时，{@link #onActionResolutionComplete} 记录两次出牌（Double Rent）。 */
    boolean pendingUsesTwoPlays() {
        return pendingUsesTwoPlays;
    }

    void setPendingUsesTwoPlays(boolean pendingUsesTwoPlays) {
        this.pendingUsesTwoPlays = pendingUsesTwoPlays;
    }

    private ServerMessage ok(String text) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.OK;
        msg.text = text;
        return msg;
    }

    private ServerMessage error(String text) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.ERROR;
        msg.text = text;
        return msg;
    }
}
