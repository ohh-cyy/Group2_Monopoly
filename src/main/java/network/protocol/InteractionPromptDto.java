package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-driven prompt sent to one client inside a PROMPT message.
 * The client must reply with RESPOND and the same {@link #promptId}.
 */
public class InteractionPromptDto {
    /** Unique id for this prompt; required in the RESPOND message. */
    public String promptId;

    /** JUST_SAY_NO or PAYMENT; see {@link network.server.PendingActionResolution}. */
    public String promptType;

    /** Seat that must answer this prompt. */
    public int responderSeat = -1;

    /** Seat of the player who initiated the action. */
    public int attackerSeat = -1;

    /** Display name of the player who initiated the action. */
    public String attackerName;

    /** Short description shown in the client dialog. */
    public String actionName;

    /** Depth in a Just Say No counter chain (0 = first response). */
    public int responseDepth;

    /** Total amount owed for a payment prompt. */
    public int amountDue;

    /** Amount still owed after partial payments. */
    public int remainingDue;

    /** Cards the responder may choose to pay with. */
    public List<CardDto> payableCards = new ArrayList<>();
}
