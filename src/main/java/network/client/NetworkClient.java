package network.client;

import network.JsonUtil;
import network.protocol.ClientMessage;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * TCP client for online Monopoly Deal.
 * <p>
 * Protocol: one JSON object per line ({@code println} / {@code readLine}).
 * Incoming messages are delivered on a background thread via {@link #setListener}.
 */
public class NetworkClient implements AutoCloseable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    /** Background thread that blocks on {@code readLine()} until disconnect. */
    private Thread readerThread;
    /** Invoked on the reader thread for each inbound message. */
    private Consumer<ServerMessage> listener;
    private volatile boolean connected;

    /** Registers the callback invoked for each parsed {@link ServerMessage}. */
    public void setListener(Consumer<ServerMessage> listener) {
        this.listener = listener;
    }

    /**
     * Opens a new connection and starts the background read loop.
     * Closes any previous connection first.
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

    /** Reads lines until disconnect; parses JSON and forwards to the listener. */
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

    /** Sends a command if the socket is still connected. */
    public void send(ClientMessage message) {
        if (writer != null && connected) {
            writer.println(JsonUtil.toJson(message));
        }
    }

    /** JOIN: enter the lobby with a display name. */
    public void join(String playerName, boolean host) {
        ClientMessage msg = new ClientMessage();
        msg.type = "JOIN";
        msg.playerName = playerName;
        msg.host = host;
        send(msg);
    }

    /** START_GAME: host-only request to begin the match. */
    public void startGame() {
        ClientMessage msg = new ClientMessage();
        msg.type = "START_GAME";
        send(msg);
    }

    /** DRAW: current player draws two cards. */
    public void draw() {
        ClientMessage msg = new ClientMessage();
        msg.type = "DRAW";
        send(msg);
    }

    /** DISCARD_CARD: discard one card from hand by instance id. */
    public void discardCard(String cardId) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.DISCARD_CARD;
        msg.cardId = cardId;
        send(msg);
    }

    /** END_TURN: end the current player's turn. */
    public void endTurn() {
        ClientMessage msg = new ClientMessage();
        msg.type = "END_TURN";
        send(msg);
    }

    /**
     * PLAY_CARD: play or bank a card.
     * Caller must populate mode, targets, colors, etc. before sending.
     */
    public void playCard(ClientMessage playMessage) {
        playMessage.type = "PLAY_CARD";
        send(playMessage);
    }

    /** RECOLOR_WILD: change the color of a wild property on the board. */
    public void recolorWildProperty(String cardId, String color) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.RECOLOR_WILD;
        msg.cardId = cardId;
        msg.color = color;
        send(msg);
    }

    /** SYNC: request a fresh STATE snapshot for this client. */
    public void requestSync() {
        ClientMessage msg = new ClientMessage();
        msg.type = "SYNC";
        send(msg);
    }

    /**
     * RESPOND: answer a server PROMPT.
     *
     * @param useJustSayNo  for JUST_SAY_NO prompts; null for payment prompts
     * @param paymentCardId for PAYMENT prompts; null for JSN prompts
     */
    public void respond(String promptId, Boolean useJustSayNo, String paymentCardId) {
        ClientMessage msg = new ClientMessage();
        msg.type = "RESPOND";
        msg.promptId = promptId;
        msg.useJustSayNo = useJustSayNo;
        msg.paymentCardId = paymentCardId;
        send(msg);
    }

    /** REMATCH_VOTE: accept or decline playing another round after a win. */
    public void voteRematch(boolean accept) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.REMATCH_VOTE;
        msg.acceptRematch = accept;
        send(msg);
    }

    /** SEND_EMOJI: broadcast a reaction emoji to all players. */
    public void sendEmoji(String emoji) {
        ClientMessage msg = new ClientMessage();
        msg.type = MessageTypes.SEND_EMOJI;
        msg.emoji = emoji;
        send(msg);
    }

    /** Stops the reader thread and closes the underlying socket. */
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
