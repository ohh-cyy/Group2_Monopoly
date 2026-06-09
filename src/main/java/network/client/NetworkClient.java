package network.client;

import network.JsonUtil;
import network.protocol.ClientMessage;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient implements AutoCloseable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Thread readerThread;
    private Consumer<ServerMessage> listener;
    private volatile boolean connected;

    public void setListener(Consumer<ServerMessage> listener) {
        this.listener = listener;
    }

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

    public void send(ClientMessage message) {
        if (writer != null && connected) {
            writer.println(JsonUtil.toJson(message));
        }
    }

    public void join(String playerName, boolean host) {
        ClientMessage msg = new ClientMessage();
        msg.type = "JOIN";
        msg.playerName = playerName;
        msg.host = host;
        send(msg);
    }

    public void startGame() {
        ClientMessage msg = new ClientMessage();
        msg.type = "START_GAME";
        send(msg);
    }

    public void draw() {
        ClientMessage msg = new ClientMessage();
        msg.type = "DRAW";
        send(msg);
    }

    public void discardCard(String cardId) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.DISCARD_CARD;
        msg.cardId = cardId;
        send(msg);
    }

    public void endTurn() {
        ClientMessage msg = new ClientMessage();
        msg.type = "END_TURN";
        send(msg);
    }

    public void playCard(ClientMessage playMessage) {
        playMessage.type = "PLAY_CARD";
        send(playMessage);
    }

    public void recolorWildProperty(String cardId, String color) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.RECOLOR_WILD;
        msg.cardId = cardId;
        msg.color = color;
        send(msg);
    }

    public void requestSync() {
        ClientMessage msg = new ClientMessage();
        msg.type = "SYNC";
        send(msg);
    }

    public void respond(String promptId, Boolean useJustSayNo, String paymentCardId) {
        ClientMessage msg = new ClientMessage();
        msg.type = "RESPOND";
        msg.promptId = promptId;
        msg.useJustSayNo = useJustSayNo;
        msg.paymentCardId = paymentCardId;
        send(msg);
    }

    public void voteRematch(boolean accept) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.REMATCH_VOTE;
        msg.acceptRematch = accept;
        send(msg);
    }

    public void sendEmoji(String emoji) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.SEND_EMOJI;
        msg.emoji = emoji;
        send(msg);
    }

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
