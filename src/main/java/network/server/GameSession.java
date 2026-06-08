package network.server;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import engine.PropertyRules;
import model.card.Card;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.card.actionCard.*;
import model.enums.Color;
import model.player.Player;
import network.GameStateMapper;
import network.protocol.ClientMessage;
import network.protocol.InteractionPromptDto;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;
import network.CardMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameSession {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;
    public static final int TURN_TIME_SECONDS = 60;

    private final ClientHandler[] seats = new ClientHandler[MAX_PLAYERS];
    private final String[] names = new String[MAX_PLAYERS];
    private int playerCount;
    private GameEngine engine;
    private final List<String> logLines = new ArrayList<>();
    private final ServerPlayHandler playHandler = new ServerPlayHandler();
    private PendingActionResolution pendingResolution;
    private boolean pendingUsesTwoPlays;
    private boolean rematchOpen;
    private boolean rematchDeclined;
    private final Boolean[] rematchVotes = new Boolean[MAX_PLAYERS];
    private final ScheduledExecutorService turnTimerExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "game-session-turn-timer");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> turnTimeoutTask;
    private long turnDeadlineEpochMillis;

    public synchronized void bindPlayer(int seat, ClientHandler handler, String name) {
        if (seat < 0 || seat >= MAX_PLAYERS) {
            return;
        }
        seats[seat] = handler;
        names[seat] = name;
        playerCount = Math.max(playerCount, seat + 1);
    }

    public synchronized void startGame(int activePlayers) {
        List<Player> players = new ArrayList<>();
        playerCount = activePlayers;
        for (int i = 0; i < activePlayers; i++) {
            players.add(new Player(names[i]));
        }
        engine = new GameEngine(players, new Deck(DeckFactory.createFullDeck()));
        engine.startGame();
        pendingResolution = null;
        pendingUsesTwoPlays = false;
        logLines.clear();
        clearRematchState();
        appendLog("=== Game started with " + activePlayers + " players ===");
        startTurnClockLocked();
        broadcastState();
    }

    public synchronized ServerMessage handleMessage(int seat, ClientMessage message) {
        if (message == null || message.type == null) {
            return error("Invalid message");
        }
        return switch (message.type) {
            case MessageTypes.DRAW -> handleDraw(seat);
            case MessageTypes.PLAY_CARD -> handlePlayCard(seat, message);
            case MessageTypes.DISCARD_CARD -> handleDiscardCard(seat, message);
            case MessageTypes.END_TURN -> handleEndTurn(seat);
            case MessageTypes.SYNC -> buildStateMessage(seat);
            case MessageTypes.RESPOND -> handleRespond(seat, message);
            case MessageTypes.REMATCH_VOTE -> handleRematchVote(seat, message);
            case MessageTypes.SEND_EMOJI -> handleEmoji(seat, message);
            default -> error("Unknown command: " + message.type);
        };
    }

    public synchronized void sendPrompt(int seat, InteractionPromptDto prompt) {
        if (seat < 0 || seat >= playerCount || seats[seat] == null) {
            return;
        }
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.PROMPT;
        msg.prompt = prompt;
        msg.state = GameStateMapper.buildForSeat(engine, seat, logLines);
        msg.state.turnDeadlineEpochMillis = turnDeadlineEpochMillis;
        enrichRematchState(msg.state, seat);
        seats[seat].send(msg);
        broadcastStateExceptPrompt(seat);
    }

    private void broadcastStateExceptPrompt(int promptedSeat) {
        for (int i = 0; i < playerCount; i++) {
            if (i == promptedSeat) {
                continue;
            }
            ClientHandler handler = seats[i];
            if (handler != null && handler.isConnected()) {
                handler.send(buildStateMessage(i));
            }
        }
    }

    public synchronized void onActionResolutionComplete(boolean success) {
        pendingResolution = null;
        if (engine == null) {
            return;
        }
        Player player = engine.getCurrentPlayer();
        engine.recordCardsPlayed(pendingUsesTwoPlays ? 2 : 1);
        pendingUsesTwoPlays = false;
        if (engine.checkWin(player)) {
            markGameWon(player);
        } else if (engine.isTurnOver()) {
            tryAdvanceTurnAfterPlay(player);
        }
        broadcastState();
    }

    private void tryAdvanceTurnAfterPlay(Player player) {
        if (player.getHandSize() > GameEngine.MAX_HAND_SIZE) {
            appendLog(player.getName() + " must discard down to "
                    + GameEngine.MAX_HAND_SIZE + " cards to end turn");
            return;
        }
        appendLog(player.getName() + " played 3 cards, turn ending");
        advanceTurnLocked();
    }

    public synchronized void broadcastState() {
        for (int i = 0; i < playerCount; i++) {
            ClientHandler handler = seats[i];
            if (handler != null && handler.isConnected()) {
                handler.send(buildStateMessage(i));
            }
        }
    }

    public synchronized ServerMessage buildStateMessage(int seat) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.STATE;
        if (engine != null) {
            msg.state = GameStateMapper.buildForSeat(engine, seat, logLines);
            msg.state.turnDeadlineEpochMillis = turnDeadlineEpochMillis;
            enrichRematchState(msg.state, seat);
        }
        return msg;
    }

    private ServerMessage handleRematchVote(int seat, ClientMessage message) {
        if (!rematchOpen || engine == null || !engine.isGameOver()) {
            return error("Rematch not available");
        }
        if (seat < 0 || seat >= playerCount) {
            return error("Invalid seat");
        }
        if (rematchVotes[seat] != null) {
            return error("Already voted");
        }
        boolean accept = Boolean.TRUE.equals(message.acceptRematch);
        rematchVotes[seat] = accept;
        if (!accept) {
            rematchOpen = false;
            rematchDeclined = true;
            appendLog(names[seat] + " declined a rematch");
            broadcastState();
            return ok("Rematch declined");
        }

        appendLog(names[seat] + " wants a rematch");
        if (allRematchVotesYes()) {
            appendLog("All players voted yes — starting a new game");
            startGame(playerCount);
            return ok("New game started");
        }
        broadcastState();
        return ok("Vote recorded");
    }

    private void markGameWon(Player winner) {
        engine.setGameOver(true);
        cancelTurnClockLocked();
        appendLog("=== " + winner.getName() + " wins! ===");
        openRematchPhase();
    }

    private void openRematchPhase() {
        rematchOpen = true;
        rematchDeclined = false;
        java.util.Arrays.fill(rematchVotes, null);
    }

    private void clearRematchState() {
        rematchOpen = false;
        rematchDeclined = false;
        java.util.Arrays.fill(rematchVotes, null);
    }

    private boolean allRematchVotesYes() {
        for (int i = 0; i < playerCount; i++) {
            if (!Boolean.TRUE.equals(rematchVotes[i])) {
                return false;
            }
        }
        return playerCount > 0;
    }

    private int countRematchYesVotes() {
        int count = 0;
        for (int i = 0; i < playerCount; i++) {
            if (Boolean.TRUE.equals(rematchVotes[i])) {
                count++;
            }
        }
        return count;
    }

    private void enrichRematchState(network.protocol.GameStateDto state, int seat) {
        state.rematchOpen = rematchOpen;
        state.rematchRequired = playerCount;
        state.rematchYesCount = countRematchYesVotes();
        state.rematchDeclined = rematchDeclined;
        if (seat >= 0 && seat < MAX_PLAYERS) {
            state.myRematchVote = rematchVotes[seat];
        }
    }

    private ServerMessage handleRespond(int seat, ClientMessage message) {
        if (pendingResolution == null) {
            return error("No pending prompt");
        }
        if (!pendingResolution.handleResponse(seat, message)) {
            return error("Invalid response");
        }
        broadcastState();
        return ok("Response accepted");
    }

    private ServerMessage handleDraw(int seat) {
        if (pendingResolution != null) {
            return error("Waiting for player response");
        }
        if (engine == null) {
            return error("Game not started");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("Not your turn");
        }
        if (!engine.drawCardsForCurrentPlayer()) {
            return error("Cannot draw cards now");
        }
        appendLog(engine.getCurrentPlayer().getName() + " drew 2 cards");
        broadcastState();
        return ok("Drew 2 cards");
    }

    private ServerMessage handleEndTurn(int seat) {
        if (pendingResolution != null) {
            return error("Waiting for player response");
        }
        if (engine == null) {
            return error("Game not started");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("Not your turn");
        }
        Player ending = engine.getCurrentPlayer();
        if (!engine.canEndTurn(ending)) {
            return error("Discard down to " + GameEngine.MAX_HAND_SIZE + " cards before ending turn");
        }
        appendLog(ending.getName() + " ended turn");
        advanceTurnLocked();
        broadcastState();
        return ok("Turn ended");
    }

    private ServerMessage handleDiscardCard(int seat, ClientMessage message) {
        if (pendingResolution != null) {
            return error("Waiting for player response");
        }
        if (engine == null) {
            return error("Game not started");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("Not your turn");
        }
        if (!engine.hasDrawnThisTurn()) {
            return error("Draw cards before discarding");
        }
        if (message.cardId == null || message.cardId.isBlank()) {
            return error("Missing card id");
        }
        Player player = engine.getCurrentPlayer();
        Card card = player.findInHandById(message.cardId);
        if (card == null) {
            return error("Card not in hand");
        }
        if (!engine.discardFromHand(player, card)) {
            return error("Could not discard card");
        }
        appendLog(player.getName() + " discarded " + card.getName());
        if (engine.isTurnOver() && engine.canEndTurn(player)) {
            appendLog(player.getName() + " played 3 cards, turn ending");
            advanceTurnLocked();
        }
        broadcastState();
        return ok("Card discarded");
    }

    private ServerMessage handlePlayCard(int seat, ClientMessage message) {
        if (pendingResolution != null) {
            return error("Waiting for player response");
        }
        if (engine == null) {
            return error("Game not started");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("Not your turn");
        }
        if (!engine.hasDrawnThisTurn()) {
            return error("Draw cards before playing");
        }
        if (!engine.canPlayCard()) {
            return error("No plays remaining this turn");
        }

        String mode = message.mode != null ? message.mode.toUpperCase() : "PLAY";
        if ("DOUBLE_RENT".equals(mode) && engine.getRemainingPlays() < 2) {
            return error("Double the Rent requires 2 plays remaining this turn");
        }

        if (message.cardId == null || message.cardId.isBlank()) {
            return error("Missing card id");
        }

        Player player = engine.getCurrentPlayer();
        Card card = player.findInHandById(message.cardId);
        if (card == null) {
            return error("Card not in hand");
        }

        boolean success = switch (mode) {
            case "BANK" -> playToBank(player, card);
            case "PROPERTY" -> playWildAsProperty(player, card, message);
            case "DOUBLE_RENT" -> playDoubleRentCombo(seat, player, message);
            case "EFFECT" -> playActionEffect(seat, player, card, message);
            default -> playSimpleCard(player, card);
        };

        if (!success) {
            return error(playFailureReason(player, card, message));
        }

        if (pendingResolution != null) {
            return ok("Waiting for responses");
        }

        engine.recordCardPlayed();
        if (engine.checkWin(player)) {
            markGameWon(player);
        } else if (engine.isTurnOver()) {
            tryAdvanceTurnAfterPlay(player);
        }
        broadcastState();
        return ok("Card played");
    }

    boolean playToBank(Player player, Card card) {
        if (!(card instanceof ActionCard action)) {
            if (card instanceof WildpropertyCard wild && wild.isBankable()) {
                player.removeFromHand(wild);
                wild.depositToBank(player);
                appendLog(player.getName() + " banked " + wild.getName());
                return true;
            }
            return false;
        }
        player.removeFromHand(action);
        action.depositToBank(player);
        appendLog(player.getName() + " banked " + action.getName());
        return true;
    }

    private boolean playWildAsProperty(Player player, Card card, ClientMessage message) {
        if (!(card instanceof WildpropertyCard wild)) {
            return false;
        }
        Color color = CardMapper.parseColor(message.color);
        if (color == null || !PropertyRules.canAddBillableProperty(player, color)) {
            return false;
        }
        wild.setChosenColor(color);
        player.removeFromHand(wild);
        wild.use(player, engine);
        appendLog(player.getName() + " played wild property as " + color);
        return true;
    }

    private boolean playActionEffect(int seat, Player player, Card card, ClientMessage message) {
        if (!(card instanceof ActionCard action)) {
            return false;
        }
        if (action instanceof JustSayNo || action instanceof DoubleTheRent) {
            return false;
        }
        if (PendingActionResolution.requiresInteraction(action, message)) {
            player.removeFromHand(action);
            engine.getDiscardPile().addCard(action);
            pendingResolution = new PendingActionResolution(this, engine, seat, action, message, logLines);
            pendingResolution.begin();
            return true;
        }
        boolean ok = playHandler.applyEffect(engine, player, action, message, logLines);
        if (ok) {
            player.removeFromHand(action);
            engine.getDiscardPile().addCard(action);
        }
        return ok;
    }

    private boolean playDoubleRentCombo(int seat, Player player, ClientMessage message) {
        if (message.secondCardId == null || message.secondCardId.isBlank()) {
            return false;
        }
        Card doubleCard = player.findInHandById(message.cardId);
        Card rentRaw = player.findInHandById(message.secondCardId);
        if (!(doubleCard instanceof DoubleTheRent) || !(rentRaw instanceof RentCard rentCard)) {
            return false;
        }
        Color chargeColor = CardMapper.parseColor(message.color);
        if (chargeColor == null || !isValidRentChargeColor(rentCard, player, chargeColor)) {
            return false;
        }
        if (rentCard.calculateRent(player, chargeColor) <= 0) {
            return false;
        }

        player.removeFromHand(doubleCard);
        player.removeFromHand(rentCard);
        engine.getDiscardPile().addCard(doubleCard);
        engine.getDiscardPile().addCard(rentCard);

        pendingUsesTwoPlays = true;
        pendingResolution = PendingActionResolution.rentWithDouble(
                this, engine, seat, rentCard, message, logLines);
        pendingResolution.begin();
        return true;
    }

    private boolean isValidRentChargeColor(RentCard rentCard, Player player, Color color) {
        if (rentCard.getChargeableColors(player).contains(color)) {
            return true;
        }
        return rentCard.isAllColors() && rentCard.countProperties(player, color) > 0;
    }

    private boolean playSimpleCard(Player player, Card card) {
        if (card instanceof WildpropertyCard) {
            return false;
        }
        if (card instanceof ActionCard) {
            return false;
        }
        if (card instanceof model.card.PropertyCard property
                && !PropertyRules.isSetImprovement(property)) {
            Color color = property.getColor();
            if (color != null && !PropertyRules.canAddBillableProperty(player, color)) {
                return false;
            }
        }
        card.use(player, engine);
        player.removeFromHand(card);
        appendLog(player.getName() + " played " + card.getName());
        return true;
    }


    private ServerMessage handleEmoji(int seat, ClientMessage message) {
        if (engine == null) {
            return error("Game not started");
        }
        if (seat < 0 || seat >= playerCount) {
            return error("Invalid seat");
        }
        String emoji = sanitizeEmoji(message.emoji);
        if (emoji.isEmpty()) {
            return error("Choose an emoji to send");
        }
        String name = names[seat] != null ? names[seat] : ("Player " + (seat + 1));
        appendLog(name + " sent " + emoji);
        broadcastState();
        return ok("Emoji sent");
    }

    private String sanitizeEmoji(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.length() > 12) {
            value = value.substring(0, 12);
        }
        return value;
    }

    private void advanceTurnLocked() {
        if (engine == null || engine.isGameOver()) {
            return;
        }
        engine.nextTurn();
        startTurnClockLocked();
    }

    private void startTurnClockLocked() {
        cancelTurnClockLocked();
        if (engine == null || engine.isGameOver()) {
            turnDeadlineEpochMillis = 0;
            return;
        }
        turnDeadlineEpochMillis = System.currentTimeMillis() + TURN_TIME_SECONDS * 1000L;
        turnTimeoutTask = turnTimerExecutor.schedule(this::handleTurnTimeout, TURN_TIME_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelTurnClockLocked() {
        if (turnTimeoutTask != null) {
            turnTimeoutTask.cancel(false);
            turnTimeoutTask = null;
        }
        turnDeadlineEpochMillis = 0;
    }

    private void handleTurnTimeout() {
        synchronized (this) {
            if (engine == null || engine.isGameOver()) {
                return;
            }
            if (pendingResolution != null) {
                startTurnClockLocked();
                broadcastState();
                return;
            }
            Player skipped = engine.getCurrentPlayer();
            List<Card> discarded = engine.enforceHandSizeLimit(skipped);
            for (Card card : discarded) {
                appendLog(skipped.getName() + " auto-discarded " + card.getName() + " (hand size limit)");
            }
            appendLog(skipped.getName() + " ran out of time and was skipped");
            engine.nextTurn();
            startTurnClockLocked();
            broadcastState();
        }
    }

    public synchronized void shutdown() {
        cancelTurnClockLocked();
        turnTimerExecutor.shutdownNow();
    }

    private void appendLog(String line) {
        logLines.add("[" + java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + line);
    }

    private ServerMessage ok(String text) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.OK;
        msg.text = text;
        return msg;
    }

    private String playFailureReason(Player player, Card card, ClientMessage message) {
        String mode = message.mode != null ? message.mode.toUpperCase() : "PLAY";
        if ("EFFECT".equals(mode) && card instanceof ActionCard action) {
            if (action instanceof JustSayNo) {
                return "Just Say No can only be played in response to an action against you";
            }
            if (action instanceof DoubleTheRent) {
                return "Choose a playable Rent card in hand (requires 2 plays remaining)";
            }
            if (action instanceof House || action instanceof Hotel) {
                return "Cannot add improvement to that set (need complete set"
                        + (action instanceof Hotel ? " with a House first" : "")
                        + ", and no duplicate improvement)";
            }
            if (action instanceof SlyDeal || action instanceof ForcedDeal || action instanceof DealBreaker) {
                return "Missing or invalid target for this action card";
            }
            if (action instanceof DebtCollector) {
                return "Choose an opponent to collect from";
            }
            if (action instanceof RentCard) {
                return "Choose a valid rent color or play a matching property first";
            }
        }
        if ("BANK".equals(mode)) {
            return "This card cannot be deposited to the bank";
        }
        if ("PROPERTY".equals(mode)) {
            return "That color set is already complete; choose another color or deposit to bank";
        }
        if (card instanceof model.card.PropertyCard) {
            return "That color set is already complete; only House or Hotel can be added";
        }
        return "Could not play card";
    }

    private ServerMessage error(String text) {
        ServerMessage msg = new ServerMessage();
        msg.type = MessageTypes.ERROR;
        msg.text = text;
        return msg;
    }
}
