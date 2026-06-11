package network.client;

import network.JsonUtil;
import network.protocol.ClientMessage;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * 联机 Monopoly Deal 的 TCP 客户端。
 * <p>
 * 协议：每行一个 JSON 对象（{@code println} / {@code readLine}）。
 * 入站消息由后台线程通过 {@link #setListener} 投递。
 */
public class NetworkClient implements AutoCloseable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    /** 后台线程，在断开前阻塞于 {@code readLine()}。 */
    private Thread readerThread;
    /** 在读取线程上为每条入站消息调用。 */
    private Consumer<ServerMessage> listener;
    private volatile boolean connected;

    /** 注册每条已解析 {@link ServerMessage} 的回调。 */
    public void setListener(Consumer<ServerMessage> listener) {
        this.listener = listener;
    }

    /**
     * 建立新连接并启动后台读取循环。
     * 会先关闭已有连接。
     */
    public void connect(String host, int port, Consumer<ServerMessage> listener) throws IOException {
        close();
        this.listener = listener;
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        connected = true;
        readerThread = new Thread(this::readLoop, "network-client-read");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /** 读取行直至断开；解析 JSON 并转发给监听器。 */
    private void readLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                ServerMessage message = JsonUtil.parseServer(line);
                if (message != null && listener != null) {
                    listener.accept(message);
                }
            }
        } catch (IOException ignored) {
        } finally {
            connected = false;
        }
    }

    /** 套接字仍连接时发送命令。 */
    public void send(ClientMessage message) {
        if (writer != null && connected) {
            writer.println(JsonUtil.toJson(message));
        }
    }

    /** JOIN：以显示名称进入大厅。 */
    public void join(String playerName, boolean host) {
        ClientMessage msg = new ClientMessage();
        msg.type = "JOIN";
        msg.playerName = playerName;
        msg.host = host;
        send(msg);
    }

    /** START_GAME：仅主机可请求开始比赛。 */
    public void startGame() {
        ClientMessage msg = new ClientMessage();
        msg.type = "START_GAME";
        send(msg);
    }

    /** DRAW：当前玩家抽两张牌。 */
    public void draw() {
        ClientMessage msg = new ClientMessage();
        msg.type = "DRAW";
        send(msg);
    }

    /** DISCARD_CARD：按实例 id 从手牌弃掉一张卡。 */
    public void discardCard(String cardId) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.DISCARD_CARD;
        msg.cardId = cardId;
        send(msg);
    }

    /** END_TURN：结束当前玩家回合。 */
    public void endTurn() {
        ClientMessage msg = new ClientMessage();
        msg.type = "END_TURN";
        send(msg);
    }

    /**
     * PLAY_CARD：打出或存入银行。
     * 发送前须填充 mode、目标、颜色等字段。
     */
    public void playCard(ClientMessage playMessage) {
        playMessage.type = "PLAY_CARD";
        send(playMessage);
    }

    /** RECOLOR_WILD：更改牌面上万能地产的颜色。 */
    public void recolorWildProperty(String cardId, String color) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.RECOLOR_WILD;
        msg.cardId = cardId;
        msg.color = color;
        send(msg);
    }

    /** SYNC：请求本客户端的最新 STATE 快照。 */
    public void requestSync() {
        ClientMessage msg = new ClientMessage();
        msg.type = "SYNC";
        send(msg);
    }

    /**
     * RESPOND：应答服务端 PROMPT。
     *
     * @param useJustSayNo  用于 JUST_SAY_NO 提示；支付提示时为 null
     * @param paymentCardId 用于 PAYMENT 提示；JSN 提示时为 null
     */
    public void respond(String promptId, Boolean useJustSayNo, String paymentCardId) {
        ClientMessage msg = new ClientMessage();
        msg.type = "RESPOND";
        msg.promptId = promptId;
        msg.useJustSayNo = useJustSayNo;
        msg.paymentCardId = paymentCardId;
        send(msg);
    }

    /** REMATCH_VOTE：获胜后投票是否再来一局。 */
    public void voteRematch(boolean accept) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.REMATCH_VOTE;
        msg.acceptRematch = accept;
        send(msg);
    }

    /** SEND_EMOJI：向所有玩家广播表情反应。 */
    public void sendEmoji(String emoji) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.SEND_EMOJI;
        msg.emoji = emoji;
        send(msg);
    }

    /** PAUSE_GAME：冻结所有玩家共享的回合计时器。 */
    public void pauseGame() {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.PAUSE_GAME;
        send(msg);
    }

    /** RESUME_GAME：暂停后恢复共享回合计时器。 */
    public void resumeGame() {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.RESUME_GAME;
        send(msg);
    }

    /** 停止读取线程并关闭底层套接字。 */
    @Override
    public void close() {
        connected = false;
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
        reader = null;
        writer = null;
    }
}
