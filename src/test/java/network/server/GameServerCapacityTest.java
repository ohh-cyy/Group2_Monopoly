package network.server;

import network.JsonUtil;
import network.protocol.ClientMessage;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 测试联机服务器 {@link GameServer} 最大 5 人连接上限。 */
class GameServerCapacityTest {

    /** 前 5 人可加入，第 6 人应收到 Room is full 错误。 */
    @Test
    void acceptsFivePlayersAndRejectsTheSixth() throws Exception {
        List<Socket> sockets = new ArrayList<>();
        try (GameServer server = new GameServer(0)) {
            server.start();

            for (int i = 0; i < GameSession.MAX_PLAYERS; i++) {
                Socket socket = connectAndJoin(server.getPort(), "Player " + (i + 1), i == 0);
                sockets.add(socket);
                ServerMessage joined = readMessage(socket);
                assertEquals(MessageTypes.JOINED, joined.type);
                assertEquals(i, joined.seat);
            }

            Socket rejectedSocket = connectAndJoin(server.getPort(), "Player 6", false);
            sockets.add(rejectedSocket);
            ServerMessage rejected = readMessage(rejectedSocket);
            assertEquals(MessageTypes.ERROR, rejected.type);
            assertEquals("Room is full (5 players max)", rejected.text);
        } finally {
            for (Socket socket : sockets) {
                socket.close();
            }
        }
    }

    private Socket connectAndJoin(int port, String name, boolean host) throws Exception {
        Socket socket = new Socket("127.0.0.1", port);
        socket.setSoTimeout(3_000);
        ClientMessage join = new ClientMessage();
        join.type = MessageTypes.JOIN;
        join.playerName = name;
        join.host = host;
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        writer.println(JsonUtil.toJson(join));
        return socket;
    }

    private ServerMessage readMessage(Socket socket) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return JsonUtil.parseServer(reader.readLine());
    }
}
