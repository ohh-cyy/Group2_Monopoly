package network.protocol;

/**
 * One entry in the pre-game lobby player list.
 */
public class LobbyPlayerDto {
    public int seat;
    public String name;

    /** True if this player may start the match. */
    public boolean host;

    /** True once JOIN has completed successfully. */
    public boolean joined;
}
