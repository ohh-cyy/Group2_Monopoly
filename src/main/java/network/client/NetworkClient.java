package network.client;

import network.JsonUtil;
import network.protocol.ClientMessage;
import network.protocol.ServerMessage;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/** TCP + JSON 客户端 */
public class NetworkClient {
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private Thread readerThread;
    private Consumer<ServerMessage> messageListener;
    private volatile boolean connected;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        readerThread = new Thread(this::readLoop, "network-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void setMessageListener(Consumer<ServerMessage> listener) {
        this.messageListener = listener;
    }

    private void readLoop() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                ServerMessage message = JsonUtil.fromJson(line, ServerMessage.class);
                if (message != null && messageListener != null) {
                    messageListener.accept(message);
                }
            }
        } catch (IOException e) {
            if (connected) {
                ServerMessage error = new ServerMessage();
                error.type = "ERROR";
                error.message = "与服务器断开连接";
                if (messageListener != null) {
                    messageListener.accept(error);
                }
            }
        }
    }

    public void send(ClientMessage message) {
        if (writer != null && connected) {
            writer.println(JsonUtil.toJson(message));
        }
    }

    public void join(String playerName) {
        ClientMessage msg = new ClientMessage();
        msg.type = "JOIN";
        msg.playerName = playerName;
        send(msg);
    }

    public void draw() {
        ClientMessage msg = new ClientMessage();
        msg.type = "DRAW";
        send(msg);
    }

    public void endTurn() {
        ClientMessage msg = new ClientMessage();
        msg.type = "END_TURN";
        send(msg);
    }

    public void playCard(String cardId, String mode, String color) {
        ClientMessage msg = new ClientMessage();
        msg.type = "PLAY_CARD";
        msg.cardId = cardId;
        msg.mode = mode;
        msg.color = color;
        send(msg);
    }

    public void requestSync() {
        ClientMessage msg = new ClientMessage();
        msg.type = "SYNC";
        send(msg);
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
