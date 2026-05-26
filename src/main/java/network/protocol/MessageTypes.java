package network.protocol;

/** 客户端 → 服务端 消息类型 */
public final class MessageTypes {
    public static final String JOIN = "JOIN";
    public static final String DRAW = "DRAW";
    public static final String PLAY_CARD = "PLAY_CARD";
    public static final String END_TURN = "END_TURN";
    public static final String SYNC = "SYNC";

    /** 服务端 → 客户端 */
    public static final String JOINED = "JOINED";
    public static final String WAITING = "WAITING";
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String STATE = "STATE";
    public static final String ERROR = "ERROR";

    private MessageTypes() {
    }
}
