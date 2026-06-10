package network.protocol;

/**
 * Outbound message from a game client to the server.
 * Only fields relevant to {@link #type} need to be set.
 */
public class ClientMessage {
    /** Command name; see {@link MessageTypes}. */
    public String type;

    /** JOIN: display name entered in the lobby. */
    public String playerName;

    /** JOIN: true when this client started the embedded host server. */
    public boolean host;

    /** Card instance id for play, discard, or wild recolor commands. */
    public String cardId;

    /** PLAY_CARD mode: PLAY, BANK, EFFECT, PROPERTY, or DOUBLE_RENT. */
    public String mode;

    /** Property or rent color name (enum {@code Color} as string). */
    public String color;

    /** Target opponent seat for steal, debt, deal-breaker, etc. */
    public Integer targetSeat;

    /** Target property card id for steal or forced-deal commands. */
    public String targetCardId;

    /** Second card id, e.g. rent card paired with Double the Rent. */
    public String secondCardId;

    /** RESPOND: id copied from {@link InteractionPromptDto#promptId}. */
    public String promptId;

    /** RESPOND (JUST_SAY_NO): true to play a Just Say No card. */
    public Boolean useJustSayNo;

    /** RESPOND (PAYMENT): instance id of the bank card or property used to pay. */
    public String paymentCardId;

    /** REMATCH_VOTE: true to play again, false to decline. */
    public Boolean acceptRematch;

    /** SEND_EMOJI: emoji text; must be in {@link EmojiCatalog}. */
    public String emoji;
}
