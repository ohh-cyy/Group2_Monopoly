package network.server;

import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/** 联机房间服务端 */
public class GameServer {
    private final int port;
    private final List<ClientHandler> lobby = new ArrayList<>();
    private GameSession session;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public GameServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Monopoly Deal 服务器已启动，端口 " + port + "，等待 " + GameSession.MAX_PLAYERS + " 名玩家...");
        while (running) {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(this, socket);
            synchronized (lobby) {
                if (session != null) {
                    socket.close();
                    continue;
                }
                if (lobby.size() >= GameSession.MAX_PLAYERS) {
                    socket.close();
                    continue;
                }
                lobby.add(handler);
            }
            new Thread(handler, "client-" + socket.getPort()).start();
        }
    }

    public synchronized void handleJoin(ClientHandler handler, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            handler.send(error("请输入昵称"));
            return;
        }
        if (session != null) {
            handler.send(error("游戏已开始，无法加入"));
            return;
        }

        int seat = lobby.indexOf(handler);
        if (seat < 0) {
            handler.send(error("连接异常"));
            return;
        }

        handler.assignSeat(seat, playerName.trim());
        ServerMessage joined = new ServerMessage();
        joined.type = MessageTypes.JOINED;
        joined.yourSeat = seat;
        joined.message = "已加入，座位 " + (seat + 1);
        joined.waitingCount = lobby.size();
        handler.send(joined);

        broadcastWaiting();

        if (lobby.size() == GameSession.MAX_PLAYERS) {
            startGame();
        }
    }

    private void startGame() {
        List<String> names = new ArrayList<>();
        for (ClientHandler handler : lobby) {
            names.add(handler.getPlayerName());
        }
        session = new GameSession(names);
        for (int i = 0; i < lobby.size(); i++) {
            session.bindSeat(i, lobby.get(i));
        }

        ServerMessage started = new ServerMessage();
        started.type = MessageTypes.GAME_STARTED;
        started.message = GameSession.MAX_PLAYERS + " 人到齐，游戏开始！";
        for (ClientHandler handler : lobby) {
            handler.send(started);
        }
        session.broadcastState();
        System.out.println("Game started with players: " + names);
    }

    private void broadcastWaiting() {
        ServerMessage waiting = new ServerMessage();
        waiting.type = MessageTypes.WAITING;
        waiting.waitingCount = lobby.size();
        waiting.message = "等待玩家 " + lobby.size() + "/" + GameSession.MAX_PLAYERS;
        for (ClientHandler handler : lobby) {
            if (handler.getSeat() >= 0) {
                handler.send(waiting);
            }
        }
    }

    public synchronized void removeClient(ClientHandler handler) {
        lobby.remove(handler);
        if (session == null) {
            broadcastWaiting();
        }
    }

    public GameSession getSession() {
        return session;
    }

    public void stop() throws IOException {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }

    private ServerMessage error(String message) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.ERROR;
        msg.message = message;
        return msg;
    }
}
