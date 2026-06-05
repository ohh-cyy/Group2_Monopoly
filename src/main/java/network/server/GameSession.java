package network.server;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
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

public class GameSession {
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

    private final ClientHandler[] seats = new ClientHandler[MAX_PLAYERS];
    private final String[] names = new String[MAX_PLAYERS];
    private int playerCount;
    private GameEngine engine;
    private final List<String> logLines = new ArrayList<>();
    private final ServerPlayHandler playHandler = new ServerPlayHandler();
    private PendingActionResolution pendingResolution;

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
        logLines.clear();
        appendLog("=== Game started with " + activePlayers + " players ===");
        broadcastState();
    }

    public synchronized ServerMessage handleMessage(int seat, ClientMessage message) {
        if (message == null || message.type == null) {
            return error("Invalid message");
        }
        return switch (message.type) {
            case MessageTypes.DRAW -> handleDraw(seat);
            case MessageTypes.PLAY_CARD -> handlePlayCard(seat, message);
            case MessageTypes.END_TURN -> handleEndTurn(seat);
            case MessageTypes.SYNC -> buildStateMessage(seat);
            case MessageTypes.RESPOND -> handleRespond(seat, message);
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
        engine.recordCardPlayed();
        if (engine.checkWin(player)) {
            engine.setGameOver(true);
            appendLog("=== " + player.getName() + " wins! ===");
        } else if (engine.isTurnOver()) {
            appendLog(player.getName() + " played 3 cards, turn ending");
            engine.nextTurn();
        }
        broadcastState();
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
        }
        return msg;
    }

    private ServerMessage handleRespond(int seat, ClientMessage message) {
        if (pendingResolution == null) {
            return error("No pending prompt");
        }
        if (!pendingResolution.handleResponse(seat, message)) {
            return error("Invalid response");
        }
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
        appendLog(ending.getName() + " ended turn");
        engine.nextTurn();
        broadcastState();
        return ok("Turn ended");
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
        if (!engine.canPlayCard()) {
            return error("No plays remaining this turn");
        }
        if (message.cardId == null || message.cardId.isBlank()) {
            return error("Missing card id");
        }

        Player player = engine.getCurrentPlayer();
        Card card = player.findInHandById(message.cardId);
        if (card == null) {
            return error("Card not in hand");
        }

        String mode = message.mode != null ? message.mode.toUpperCase() : "PLAY";
        boolean success = switch (mode) {
            case "BANK" -> playToBank(player, card);
            case "PROPERTY" -> playWildAsProperty(player, card, message);
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
            engine.setGameOver(true);
            appendLog("=== " + player.getName() + " wins! ===");
        } else if (engine.isTurnOver()) {
            appendLog(player.getName() + " played 3 cards, turn ending");
            engine.nextTurn();
        }
        broadcastState();
        return ok("Card played");
    }

    private boolean playToBank(Player player, Card card) {
        if (!(card instanceof ActionCard action)) {
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
        if (color == null) {
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
        if (action instanceof JustSayNo) {
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

    private boolean playSimpleCard(Player player, Card card) {
        if (card instanceof WildpropertyCard) {
            return false;
        }
        if (card instanceof ActionCard) {
            return false;
        }
        card.use(player, engine);
        player.removeFromHand(card);
        appendLog(player.getName() + " played " + card.getName());
        return true;
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
            if (action instanceof DoubleTheRent && engine.isRentDoubled()) {
                return "Double the Rent is already active — play a Rent card first";
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
            return "Choose a valid color for the wild property";
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
