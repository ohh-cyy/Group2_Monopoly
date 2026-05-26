package network.protocol;

import java.util.ArrayList;
import java.util.List;

/** 服务端发送的 JSON 消息 */
public class ServerMessage {
    public String type;
    public String message;
    public int yourSeat = -1;
    public int waitingCount;
    public GameStateDto state;
    public List<String> events = new ArrayList<>();
}
