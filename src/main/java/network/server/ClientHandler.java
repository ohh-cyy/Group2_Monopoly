package network.server;

import network.JsonUtil;
import network.protocol.ClientMessage;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.io.*;
import java.net.Socket;

/**
 * Handles one connected TCP client on the server.
 * Each connection runs in its own thread and forwards parsed messages to {@link GameServer}.
 */
public class ClientHandler implements Runnable {
    private final GameServer server;
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    /** Assigned on successful JOIN; -1 before lobby registration. */
    private int seat = -1;
    private String playerName;
    private boolean joined;
    private boolean host;
    private volatile boolean connected = true;

    public ClientHandler(GameServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    /** Read loop: one JSON line per client message. */
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

    /** Writes one server message as a single JSON line. */
    public void send(ServerMessage message) {
        if (writer != null && connected) {
            writer.println(JsonUtil.toJson(message));
        }
    }

    /** Called by GameServer after seat assignment. */
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

    /** Closes the socket and notifies the server to update lobby/session state. */
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
