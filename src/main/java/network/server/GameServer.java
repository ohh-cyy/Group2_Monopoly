package network.server;

import network.protocol.ClientMessage;
import network.protocol.LobbyPlayerDto;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameServer implements AutoCloseable {
    private final int port;
    private ServerSocket serverSocket;
    private final List<ClientHandler> connections = new ArrayList<>();
    private GameSession session;
    private volatile boolean running;

    public GameServer(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "game-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        System.out.println("Monopoly Deal server started on port " + port);
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(this, socket);
                synchronized (this) {
                    connections.add(handler);
                }
                Thread thread = new Thread(handler, "client-" + socket.getPort());
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Accept failed: " + e.getMessage());
                }
                break;
            }
        }
    }

    public synchronized void handleClientMessage(ClientHandler handler, ClientMessage message) {
        if (message.type == null) {
            handler.send(error("Missing message type"));
            return;
        }
        switch (message.type) {
            case MessageTypes.JOIN -> handleJoin(handler, message);
            case MessageTypes.START_GAME -> handleStartGame(handler);
            case MessageTypes.LEAVE -> handler.disconnect();
            default -> {
                if (session == null) {
                    handler.send(error("Game has not started"));
                    return;
                }
                ServerMessage response = session.handleMessage(handler.getSeat(), message);
                if (response != null && MessageTypes.ERROR.equals(response.type)) {
                    handler.send(response);
                }
            }
        }
    }

    private void handleJoin(ClientHandler handler, ClientMessage message) {
        if (session != null) {
            handler.send(error("Game already started"));
            return;
        }
        String name = message.playerName != null ? message.playerName.trim() : "";
        if (name.isEmpty()) {
            handler.send(error("Please enter a name"));
            return;
        }

        int joinedCount = joinedCount();
        if (joinedCount >= GameSession.MAX_PLAYERS) {
            handler.send(error("Room is full (" + GameSession.MAX_PLAYERS + " players max)"));
            return;
        }

        int seat = nextSeat();
        boolean host = message.host || joinedCount == 0;
        handler.assignSeat(seat, name, host);

        ServerMessage joined = new ServerMessage();
        joined.type = MessageTypes.JOINED;
        joined.seat = seat;
        joined.host = host;
        joined.youAreHost = host;
        joined.text = "Joined as seat " + seat;
        handler.send(joined);
        broadcastLobby();
    }

    private void handleStartGame(ClientHandler requester) {
        if (session != null) {
            requester.send(error("Game already started"));
            return;
        }
        if (!requester.isHost()) {
            requester.send(error("Only the host can start the game"));
            return;
        }
        int count = joinedCount();
        if (count < GameSession.MIN_PLAYERS) {
            requester.send(error("Need at least " + GameSession.MIN_PLAYERS + " players"));
            return;
        }
        if (count > GameSession.MAX_PLAYERS) {
            requester.send(error("Too many players"));
            return;
        }

        session = new GameSession();
        List<ClientHandler> joinedHandlers = joinedHandlers();
        for (ClientHandler handler : joinedHandlers) {
            session.bindPlayer(handler.getSeat(), handler, handler.getPlayerName());
        }
        session.startGame(count);

        for (ClientHandler handler : joinedHandlers) {
            ServerMessage started = new ServerMessage();
            started.type = MessageTypes.GAME_STARTED;
            started.seat = handler.getSeat();
            started.youAreHost = handler.isHost();
            started.state = session.buildStateMessage(handler.getSeat()).state;
            handler.send(started);
        }
    }

    public synchronized void onClientDisconnected(ClientHandler handler) {
        connections.remove(handler);
        if (session == null) {
            broadcastLobby();
        }
    }

    private int joinedCount() {
        int count = 0;
        for (ClientHandler handler : connections) {
            if (handler.isJoined()) {
                count++;
            }
        }
        return count;
    }

    private List<ClientHandler> joinedHandlers() {
        List<ClientHandler> list = new ArrayList<>();
        for (ClientHandler handler : connections) {
            if (handler.isJoined()) {
                list.add(handler);
            }
        }
        list.sort((a, b) -> Integer.compare(a.getSeat(), b.getSeat()));
        return list;
    }

    private int nextSeat() {
        boolean[] used = new boolean[GameSession.MAX_PLAYERS];
        for (ClientHandler handler : connections) {
            if (handler.isJoined() && handler.getSeat() >= 0 && handler.getSeat() < used.length) {
                used[handler.getSeat()] = true;
            }
        }
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                return i;
            }
        }
        return joinedCount();
    }

    private void broadcastLobby() {
        ServerMessage lobby = buildLobbyMessage();
        for (ClientHandler handler : connections) {
            if (handler.isConnected()) {
                lobby.youAreHost = handler.isHost();
                handler.send(lobby);
            }
        }
    }

    private ServerMessage buildLobbyMessage() {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.LOBBY;
        for (ClientHandler handler : connections) {
            if (handler.isJoined()) {
                LobbyPlayerDto dto = new LobbyPlayerDto();
                dto.seat = handler.getSeat();
                dto.name = handler.getPlayerName();
                dto.host = handler.isHost();
                dto.joined = true;
                msg.lobbyPlayers.add(dto);
            }
        }
        msg.text = "Players: " + msg.lobbyPlayers.size() + " / " + GameSession.MAX_PLAYERS;
        return msg;
    }

    private ServerMessage error(String text) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.ERROR;
        msg.text = text;
        return msg;
    }

    @Override
    public void close() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        synchronized (this) {
            for (ClientHandler handler : new ArrayList<>(connections)) {
                handler.disconnect();
            }
            connections.clear();
        }
    }
}
