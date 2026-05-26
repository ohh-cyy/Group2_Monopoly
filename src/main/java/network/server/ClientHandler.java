package network.server;

import network.protocol.ClientMessage;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.io.*;
import java.net.Socket;

/** 单个客户端连接 */
public class ClientHandler implements Runnable {
    private final GameServer server;
    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private int seat = -1;
    private String playerName;
    private volatile boolean connected = true;

    public ClientHandler(GameServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String line;
            while (connected && (line = reader.readLine()) != null) {
                ClientMessage message = network.JsonUtil.fromJson(line, ClientMessage.class);
                if (message == null) {
                    continue;
                }
                if (MessageTypes.JOIN.equals(message.type)) {
                    server.handleJoin(this, message.playerName);
                } else if (seat >= 0 && server.getSession() != null) {
                    ServerMessage response = server.getSession().handle(seat, message);
                    if (MessageTypes.ERROR.equals(response.type)) {
                        send(response);
                    } else if (MessageTypes.STATE.equals(response.type)) {
                        send(response);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + playerName);
        } finally {
            connected = false;
            server.removeClient(this);
            closeQuietly();
        }
    }

    public void send(ServerMessage message) {
        if (writer != null && connected) {
            writer.println(network.JsonUtil.toJson(message));
        }
    }

    public void assignSeat(int seat, String playerName) {
        this.seat = seat;
        this.playerName = playerName;
    }

    public int getSeat() {
        return seat;
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean isConnected() {
        return connected;
    }

    private void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
