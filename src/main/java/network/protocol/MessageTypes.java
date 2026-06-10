package network.protocol;

/**
 * String constants for the {@code type} field on client and server messages.
 * The wire protocol is plain JSON objects, one per line.
 */
public final class MessageTypes {
    private MessageTypes() {
    }

    // --- Client -> server commands ---

    /** Enter the lobby with a display name. */
    public static final String JOIN = "JOIN";
    /** Host requests the match to begin (lobby phase only). */
    public static final String START_GAME = "START_GAME";
    /** Current player draws two cards at turn start. */
    public static final String DRAW = "DRAW";
    /** Play, bank, or apply an action/property card. */
    public static final String PLAY_CARD = "PLAY_CARD";
    /** Change the color of a wild property already on the board. */
    public static final String RECOLOR_WILD = "RECOLOR_WILD";
    /** Discard one card from hand (e.g. hand-size limit). */
    public static final String DISCARD_CARD = "DISCARD_CARD";
    /** End the current player's turn. */
    public static final String END_TURN = "END_TURN";
    /** Request a fresh per-seat STATE snapshot. */
    public static final String SYNC = "SYNC";
    /** Disconnect from the server. */
    public static final String LEAVE = "LEAVE";
    /** Answer a server PROMPT (Just Say No or payment). */
    public static final String RESPOND = "RESPOND";
    /** Vote yes/no on playing another round after a win. */
    public static final String REMATCH_VOTE = "REMATCH_VOTE";
    /** Broadcast an emoji reaction to all players. */
    public static final String SEND_EMOJI = "SEND_EMOJI";
    /** Pause the shared turn timer for all players. */
    public static final String PAUSE_GAME = "PAUSE_GAME";
    /** Resume the shared turn timer after a pause. */
    public static final String RESUME_GAME = "RESUME_GAME";

    // --- Server -> client responses and events ---

    /** Command succeeded (often paired with a STATE broadcast). */
    public static final String OK = "OK";
    /** Command rejected; {@link ServerMessage#text} explains why. */
    public static final String ERROR = "ERROR";
    /** Lobby join succeeded; includes assigned seat. */
    public static final String JOINED = "JOINED";
    /** Updated lobby player list broadcast. */
    public static final String LOBBY = "LOBBY";
    /** Match started; includes initial {@link GameStateDto}. */
    public static final String GAME_STARTED = "GAME_STARTED";
    /** Authoritative game snapshot for this client's seat. */
    public static final String STATE = "STATE";
    /** Server needs input from one client (JSN or payment). */
    public static final String PROMPT = "PROMPT";
    /** Emoji reaction from another player. */
    public static final String EMOJI = "EMOJI";
}
