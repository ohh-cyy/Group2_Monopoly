package network.protocol;

import java.util.ArrayList;
import java.util.List;

public class InteractionPromptDto {
    public String promptId;
    /** JUST_SAY_NO or PAYMENT */
    public String promptType;
    public int responderSeat = -1;
    public int attackerSeat = -1;
    public String attackerName;
    public String actionName;
    public int responseDepth;
    public int amountDue;
    public int remainingDue;
    public List<CardDto> payableCards = new ArrayList<>();
}
