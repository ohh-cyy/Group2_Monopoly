package network.protocol;

public class ClientMessage {
    public String type;
    public String playerName;
    public boolean host;
    public String cardId;
    /** PLAY, BANK, EFFECT, PROPERTY */
    public String mode;
    public String color;
    public Integer targetSeat;
    public String targetCardId;
    public String secondCardId;
    /** RESPOND: id from InteractionPromptDto */
    public String promptId;
    /** RESPOND for JUST_SAY_NO: true = play Just Say No */
    public Boolean useJustSayNo;
    /** RESPOND for PAYMENT: card instance id to pay with */
    public String paymentCardId;
    /** REMATCH_VOTE: true = play again, false = decline */
    public Boolean acceptRematch;
}
