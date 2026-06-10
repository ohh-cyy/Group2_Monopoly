package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializable snapshot of an online match sent inside STATE / GAME_STARTED / PROMPT.
 * {@link #myHand} and {@link #myBank} are seat-specific; other fields are shared.
 */
public class GameStateDto {
    /** Index of the player whose turn it is. */
    public int currentPlayerIndex;

    /** Whether the current player has drawn this turn. */
    public boolean hasDrawnThisTurn;

    /** Remaining card plays allowed this turn (max 3). */
    public int remainingPlays;

    /** True when a winner has been decided. */
    public boolean gameOver;

    /** Name of the winning player when {@link #gameOver} is true. */
    public String winnerName;

    /** True after a win while players may vote on a rematch. */
    public boolean rematchOpen;

    /** Number of players who voted yes for a rematch. */
    public int rematchYesCount;

    /** Total active players required for a unanimous rematch. */
    public int rematchRequired;

    /** True when at least one player declined a rematch. */
    public boolean rematchDeclined;

    /** This client's rematch vote: null = not voted, true/false = choice. */
    public Boolean myRematchVote;

    /** Cards remaining in the draw pile. */
    public int drawPileSize;

    /** Cards in the discard pile. */
    public int discardPileSize;

    /** Epoch millis when the current turn ends; 0 if no timer is active. */
    public long turnDeadlineEpochMillis;

    /** True while the turn timer is frozen for all players. */
    public boolean gamePaused;

    /** Seconds left when {@link #gamePaused} is true. */
    public int pausedTurnSecondsRemaining;

    /** Full session log lines, newest appended at the end. */
    public List<String> logLines = new ArrayList<>();

    /** Public information for every seated player. */
    public List<PlayerViewDto> players = new ArrayList<>();

    /** Full hand of the receiving client only. */
    public List<CardDto> myHand = new ArrayList<>();

    /** Full bank of the receiving client only. */
    public List<CardDto> myBank = new ArrayList<>();
}
