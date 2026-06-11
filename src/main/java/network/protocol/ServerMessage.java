package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端发往游戏客户端的入站消息。
 * {@link #type} 决定哪些可选字段会被填充。
 */
public class ServerMessage {
    /** 事件或响应名称；见 {@link MessageTypes}。 */
    public String type;

    /** OK、ERROR、JOINED、LOBBY 等消息的人类可读文本。 */
    public String text;

    /** JOINED、EMOJI 等按座位区分的事件的座位索引。 */
    public int seat = -1;

    /** 被引用玩家是否为房间主机。 */
    public boolean host;

    /** 接收客户端是否为房间主机。 */
    public boolean youAreHost;

    /** EMOJI：广播给所有玩家的表情文本。 */
    public String emoji;

    /** STATE、GAME_STARTED、PROMPT：权威游戏快照。 */
    public GameStateDto state;

    /** LOBBY：房间内等待中的当前玩家。 */
    public List<LobbyPlayerDto> lobbyPlayers = new ArrayList<>();

    /** PROMPT：需一名客户端以 RESPOND 应答的服务端驱动交互。 */
    public InteractionPromptDto prompt;
}
