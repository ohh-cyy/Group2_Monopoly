package network.protocol;

public final class MessageTypes {
    private MessageTypes() {
    }

    // client -> server
    public static final String JOIN = "JOIN";
    public static final String START_GAME = "START_GAME";
    public static final String DRAW = "DRAW";
    public static final String PLAY_CARD = "PLAY_CARD";
    public static final String DISCARD_CARD = "DISCARD_CARD";
    public static final String END_TURN = "END_TURN";
    public static final String SYNC = "SYNC";
    public static final String LEAVE = "LEAVE";
    public static final String RESPOND = "RESPOND";
    public static final String REMATCH_VOTE = "REMATCH_VOTE";
    public static final String SEND_EMOJI = "SEND_EMOJI";

    // server -> client
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String JOINED = "JOINED";
    public static final String LOBBY = "LOBBY";
    public static final String GAME_STARTED = "GAME_STARTED";
    public static final String STATE = "STATE";
    public static final String PROMPT = "PROMPT";
}
