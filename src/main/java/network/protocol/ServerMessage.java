package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Inbound message from the server to a game client.
 * {@link #type} determines which optional fields are populated.
 */
public class ServerMessage {
    /** Event or response name; see {@link MessageTypes}. */
    public String type;

    /** Human-readable text for OK, ERROR, JOINED, LOBBY, etc. */
    public String text;

    /** Seat index for JOINED, EMOJI, and similar seat-specific events. */
    public int seat = -1;

    /** Whether the referenced player is the room host. */
    public boolean host;

    /** Whether the receiving client is the room host. */
    public boolean youAreHost;

    /** EMOJI: reaction text broadcast to all players. */
    public String emoji;

    /** STATE, GAME_STARTED, PROMPT: authoritative game snapshot. */
    public GameStateDto state;

    /** LOBBY: current players waiting in the room. */
    public List<LobbyPlayerDto> lobbyPlayers = new ArrayList<>();

    /** PROMPT: server-driven interaction requiring a RESPOND from one client. */
    public InteractionPromptDto prompt;
}
