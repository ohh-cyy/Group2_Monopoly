package network.server;

import engine.GameEngine;
import engine.PaymentTransfer;
import model.card.Card;
import model.card.RentCard;
import model.card.actionCard.*;
import model.enums.Color;
import model.player.Player;
import network.CardMapper;
import network.protocol.CardDto;
import network.protocol.ClientMessage;
import network.protocol.InteractionPromptDto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resolves action/rent cards that need defender input (Just Say No, payment choice).
 */
public final class PendingActionResolution {
    public static final String PROMPT_JUST_SAY_NO = "JUST_SAY_NO";
    public static final String PROMPT_PAYMENT = "PAYMENT";

    /** Current step when resolving multi-player action cards. */
    private enum Phase {
        JUST_SAY_NO,
        PAYMENT,
        DONE
    }

    private final GameSession session;
    private final GameEngine engine;
    private final int attackerSeat;
    private final ActionCard card;
    private final ClientMessage playMessage;
    private final List<String> logLines;
    private final ServerPlayHandler playHandler = new ServerPlayHandler();

    private Phase phase = Phase.JUST_SAY_NO;
    /** Opponents that still need JSN/payment processing for this action. */
    private final List<Integer> opponentSeats = new ArrayList<>();
    private int opponentIndex;
    private int currentPayerSeat = -1;
    private int paymentRequired;
    private int paymentPaid;
    private int totalCollected;

    // Just Say No chain state (players may counter each other's JSN cards).
    private int jsnResponderSeat = -1;
    private int jsnOtherSeat = -1;
    private int jsnDepth;
    private boolean jsnBlocked;

    /** Id of the prompt awaiting a matching RESPOND. */
    private String currentPromptId;
    private Color chargeColor;
    private int rentPerPlayer;
    private boolean rentForcedDouble;

    public PendingActionResolution(GameSession session, GameEngine engine, int attackerSeat,
                                   ActionCard card, ClientMessage playMessage, List<String> logLines) {
        this.session = session;
        this.engine = engine;
        this.attackerSeat = attackerSeat;
        this.card = card;
        this.playMessage = playMessage;
        this.logLines = logLines;
    }

    /** Factory for a rent resolution where rent is already doubled (Double the Rent combo). */
    public static PendingActionResolution rentWithDouble(GameSession session, GameEngine engine, int attackerSeat,
                                                        RentCard rentCard, ClientMessage playMessage,
                                                        List<String> logLines) {
        PendingActionResolution resolution = new PendingActionResolution(
                session, engine, attackerSeat, rentCard, playMessage, logLines);
        resolution.rentForcedDouble = true;
        return resolution;
    }

    /** True when playing this card must pause for remote PROMPT/RESPOND interaction. */
    public static boolean requiresInteraction(ActionCard card, ClientMessage message) {
        if (card instanceof RentCard || card instanceof MyBirthday) {
            return true;
        }
        if (card instanceof DebtCollector || card instanceof SlyDeal
                || card instanceof ForcedDeal || card instanceof DealBreaker) {
            return message.targetSeat != null;
        }
        return false;
    }

    /** Starts the state machine and sends the first prompt to the appropriate seat. */
    public void begin() {
        Player attacker = engine.getPlayers().get(attackerSeat);
        if (card instanceof RentCard rentCard) {
            chargeColor = CardMapper.parseColor(playMessage.color);
            if (chargeColor == null || !isValidRentColor(rentCard, attacker, chargeColor)) {
                appendLog(attacker.getName() + " failed to play " + card.getName());
                finish(false);
                return;
            }
            rentPerPlayer = rentCard.calculateRent(attacker, chargeColor);
            if (rentForcedDouble) {
                rentPerPlayer *= 2;
            } else if (engine.isRentDoubled()) {
                rentPerPlayer *= 2;
                engine.setRentDoubled(false);
            }
            if (rentPerPlayer <= 0) {
                appendLog(attacker.getName() + " played " + card.getName() + " for 0M rent");
                finish(true);
                return;
            }
            for (int i = 0; i < engine.getPlayers().size(); i++) {
                if (i != attackerSeat) {
                    opponentSeats.add(i);
                }
            }
            opponentIndex = 0;
            String doubleNote = rentForcedDouble ? " + Double the Rent" : "";
            appendLog(attacker.getName() + " played " + card.getName() + doubleNote + " ("
                    + chargeColor + " rent " + rentPerPlayer + "M/player)");
            startCurrentOpponent();
            return;
        }
        if (card instanceof MyBirthday) {
            for (int i = 0; i < engine.getPlayers().size(); i++) {
                if (i != attackerSeat) {
                    opponentSeats.add(i);
                }
            }
            opponentIndex = 0;
            paymentRequired = MyBirthday.GIFT_AMOUNT;
            appendLog(attacker.getName() + " played My Birthday");
            startCurrentOpponent();
            return;
        }
        if (card instanceof DebtCollector) {
            opponentSeats.add(playMessage.targetSeat);
            opponentIndex = 0;
            paymentRequired = DebtCollector.DEBT_AMOUNT;
            appendLog(attacker.getName() + " played Debt Collector on "
                    + playerName(playMessage.targetSeat));
            startCurrentOpponent();
            return;
        }
        if (card instanceof SlyDeal || card instanceof ForcedDeal || card instanceof DealBreaker) {
            opponentSeats.add(playMessage.targetSeat);
            opponentIndex = 0;
            appendLog(attacker.getName() + " played " + card.getName() + " on "
                    + playerName(playMessage.targetSeat));
            startCurrentOpponent();
        }
    }

    /** Processes a client RESPOND; returns false if prompt id or seat does not match. */
    public boolean handleResponse(int seat, ClientMessage response) {
        if (response == null || response.promptId == null || !response.promptId.equals(currentPromptId)) {
            return false;
        }
        if (seat != expectedResponderSeat()) {
            return false;
        }
        if (phase == Phase.JUST_SAY_NO) {
            return handleJustSayNoResponse(response);
        }
        if (phase == Phase.PAYMENT) {
            return handlePaymentResponse(response);
        }
        return false;
    }

    /** Toggles block state or extends the JSN counter-chain, then re-prompts or continues. */
    private boolean handleJustSayNoResponse(ClientMessage response) {
        if (response.useJustSayNo == null) {
            return false;
        }
        Player responder = engine.getPlayers().get(jsnResponderSeat);
        Player other = engine.getPlayers().get(jsnOtherSeat);
        if (response.useJustSayNo) {
            if (!discardJustSayNoFromHand(responder)) {
                return false;
            }
            jsnBlocked = !jsnBlocked;
            if (jsnDepth == 0) {
                appendLog(responder.getName() + " played Just Say No against "
                        + other.getName() + "'s " + actionLabel());
            } else {
                appendLog(responder.getName() + " countered with Just Say No");
            }
            jsnDepth++;
            int nextResponder = jsnOtherSeat;
            jsnOtherSeat = jsnResponderSeat;
            jsnResponderSeat = nextResponder;
            promptJustSayNoOrContinue();
            return true;
        }
        finishJustSayNoChain();
        return true;
    }

    /** Transfers one chosen asset; repeats until paid in full or payer has nothing left. */
    private boolean handlePaymentResponse(ClientMessage response) {
        if (response.paymentCardId == null || response.paymentCardId.isBlank()) {
            return false;
        }
        Player collector = engine.getPlayers().get(attackerSeat);
        Player payer = engine.getPlayers().get(currentPayerSeat);
        Card chosen = PaymentTransfer.findPayableCard(payer, response.paymentCardId);
        if (chosen == null) {
            return false;
        }
        String chosenName = chosen.getName();
        var paid = PaymentTransfer.payWithCard(collector, payer, response.paymentCardId);
        if (paid.isEmpty()) {
            return false;
        }
        paymentPaid += paid.getAsInt();
        totalCollected += paid.getAsInt();
        appendLog(payer.getName() + " paid " + chosenName
                + " (" + paid.getAsInt() + "M) to " + collector.getName());
        continuePaymentOrNextOpponent();
        return true;
    }

    /** Begins JSN/payment processing for the opponent at {@link #opponentIndex}. */
    private void startCurrentOpponent() {
        if (opponentIndex >= opponentSeats.size()) {
            completeActionEffect();
            return;
        }
        currentPayerSeat = opponentSeats.get(opponentIndex);
        paymentPaid = 0;
        if (card instanceof RentCard) {
            paymentRequired = rentPerPlayer;
        }
        jsnDepth = 0;
        jsnBlocked = false;
        jsnResponderSeat = currentPayerSeat;
        jsnOtherSeat = attackerSeat;
        phase = Phase.JUST_SAY_NO;
        promptJustSayNoOrContinue();
    }

    private void promptJustSayNoOrContinue() {
        Player responder = engine.getPlayers().get(jsnResponderSeat);
        if (findJustSayNo(responder) != null) {
            sendJustSayNoPrompt();
            return;
        }
        finishJustSayNoChain();
    }

    /** After JSN resolves: skip opponent, collect payment, or apply steal/swap effect. */
    private void finishJustSayNoChain() {
        if (jsnBlocked) {
            appendLog(playerName(currentPayerSeat) + " blocked " + actionLabel() + " with Just Say No");
            nextOpponent();
            return;
        }
        if (needsPaymentPhase()) {
            phase = Phase.PAYMENT;
            continuePaymentOrNextOpponent();
            return;
        }
        applyPropertyEffectForCurrentOpponent();
    }

    private void continuePaymentOrNextOpponent() {
        if (paymentPaid >= paymentRequired) {
            nextOpponent();
            return;
        }
        Player payer = engine.getPlayers().get(currentPayerSeat);
        if (!PaymentTransfer.hasPayableAsset(payer)) {
            nextOpponent();
            return;
        }
        sendPaymentPrompt();
    }

    private void nextOpponent() {
        opponentIndex++;
        startCurrentOpponent();
    }

    private void applyPropertyEffectForCurrentOpponent() {
        Player attacker = engine.getPlayers().get(attackerSeat);
        boolean ok = playHandler.applyEffect(engine, attacker, card, playMessage, logLines);
        finish(ok);
    }

    private void completeActionEffect() {
        Player attacker = engine.getPlayers().get(attackerSeat);
        if (card instanceof RentCard) {
            appendLog(attacker.getName() + " collected total " + totalCollected + "M rent");
            finish(true);
            return;
        }
        if (card instanceof MyBirthday) {
            appendLog(attacker.getName() + " collected " + totalCollected + "M from My Birthday");
            finish(true);
            return;
        }
        if (card instanceof DebtCollector) {
            appendLog(attacker.getName() + " collected " + totalCollected + "M from Debt Collector");
            finish(totalCollected > 0);
        }
    }

    private boolean needsPaymentPhase() {
        return card instanceof RentCard
                || card instanceof MyBirthday
                || card instanceof DebtCollector;
    }

    private void sendJustSayNoPrompt() {
        currentPromptId = UUID.randomUUID().toString();
        InteractionPromptDto prompt = new InteractionPromptDto();
        prompt.promptId = currentPromptId;
        prompt.promptType = PROMPT_JUST_SAY_NO;
        prompt.responderSeat = jsnResponderSeat;
        prompt.attackerSeat = attackerSeat;
        prompt.attackerName = playerName(attackerSeat);
        prompt.actionName = actionLabel();
        prompt.responseDepth = jsnDepth;
        session.sendPrompt(jsnResponderSeat, prompt);
    }

    private void sendPaymentPrompt() {
        currentPromptId = UUID.randomUUID().toString();
        Player payer = engine.getPlayers().get(currentPayerSeat);
        InteractionPromptDto prompt = new InteractionPromptDto();
        prompt.promptId = currentPromptId;
        prompt.promptType = PROMPT_PAYMENT;
        prompt.responderSeat = currentPayerSeat;
        prompt.attackerSeat = attackerSeat;
        prompt.attackerName = playerName(attackerSeat);
        prompt.actionName = actionLabel();
        prompt.amountDue = paymentRequired;
        prompt.remainingDue = Math.max(0, paymentRequired - paymentPaid);
        for (Card asset : PaymentTransfer.listPayableAssets(payer)) {
            CardDto dto = CardMapper.toDto(asset);
            if (dto != null) {
                prompt.payableCards.add(dto);
            }
        }
        session.sendPrompt(currentPayerSeat, prompt);
    }

    private int expectedResponderSeat() {
        if (phase == Phase.JUST_SAY_NO) {
            return jsnResponderSeat;
        }
        if (phase == Phase.PAYMENT) {
            return currentPayerSeat;
        }
        return -1;
    }

    private String actionLabel() {
        if (card instanceof RentCard) {
            String prefix = rentForcedDouble ? card.getName() + " + Double the Rent" : card.getName();
            return prefix + " (" + chargeColor + " rent " + rentPerPlayer + "M)";
        }
        return card.getName();
    }

    private boolean isValidRentColor(RentCard rentCard, Player player, Color color) {
        if (rentCard.getChargeableColors(player).contains(color)) {
            return true;
        }
        return rentCard.isAllColors() && rentCard.countProperties(player, color) > 0;
    }

    private String playerName(int seat) {
        if (seat >= 0 && seat < engine.getPlayers().size()) {
            return engine.getPlayers().get(seat).getName();
        }
        return "Player";
    }

    private boolean discardJustSayNoFromHand(Player player) {
        for (Card handCard : player.getHand()) {
            if (handCard instanceof JustSayNo) {
                String id = handCard.getInstanceId();
                if (player.removeFromHandById(id)) {
                    engine.getDiscardPile().addCard(handCard);
                    return true;
                }
            }
        }
        return false;
    }

    private JustSayNo findJustSayNo(Player player) {
        for (Card handCard : player.getHand()) {
            if (handCard instanceof JustSayNo justSayNo) {
                return justSayNo;
            }
        }
        return null;
    }

    private void appendLog(String line) {
        logLines.add("[" + java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + line);
    }

    /** Ends the state machine and lets {@link GameSession} record plays and broadcast. */
    private void finish(boolean success) {
        phase = Phase.DONE;
        session.onActionResolutionComplete(success);
    }
}
