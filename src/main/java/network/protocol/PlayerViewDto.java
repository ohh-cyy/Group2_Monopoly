package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Public view of one player in an online match.
 * Opponents see hand size but not individual hand cards.
 */
public class PlayerViewDto {
    /** Zero-based seat index. */
    public int seat;

    /** Display name chosen in the lobby. */
    public String name;

    /** Number of cards in hand (contents are hidden). */
    public int handSize;

    /** Total value of cards in the player's bank. */
    public int bankTotal;

    /** Face-up property cards on the player's board. */
    public List<CardDto> properties = new ArrayList<>();
}
