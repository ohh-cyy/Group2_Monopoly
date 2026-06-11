package network.server;

import network.JsonUtil;
import network.protocol.ClientMessage;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.io.*;
import java.net.Socket;

/**
 * 处理服务端上一个已连接的 TCP 客户端。
 * 每个连接在独立线程中运行，并将解析后的消息转发给 {@link GameServer}。
 */
public class ClientHandler implements Runnable {
    private final GameServer server;
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    /** JOIN 成功时分配；大厅注册前为 -1。 */
    private int seat = -1;
    private String playerName;
    private boolean joined;
    private boolean host;
    private volatile boolean connected = true;

    public ClientHandler(GameServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    /** 读取循环：每个客户端消息对应一行 JSON。 */
    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            String line;
            while (connected && (line = reader.readLine()) != null) {
                ClientMessage message = JsonUtil.parseClient(line);
                if (message != null) {
                    server.handleClientMessage(this, message);
                }
            }
        } catch (IOException ignored) {
        } finally {
            disconnect();
        }
    }

    /** 将一条服务端消息写成单行 JSON。 */
    public void send(ServerMessage message) {
        if (writer != null && connected) {
            writer.println(JsonUtil.toJson(message));
        }
    }

    /** 由 GameServer 在分配座位后调用。 */
    public void assignSeat(int seat, String name, boolean host) {
        this.seat = seat;
        this.playerName = name;
        this.joined = true;
        this.host = host;
    }

    public int getSeat() {
        return seat;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isJoined() {
        return joined;
    }

    public boolean isHost() {
        return host;
    }

    public boolean isConnected() {
        return connected;
    }

    /** 关闭套接字并通知服务端更新大厅/会话状态。 */
    public void disconnect() {
        if (!connected) {
            return;
        }
        connected = false;
        server.onClientDisconnected(this);
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
