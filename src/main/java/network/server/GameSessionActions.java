package network.server;

import engine.GameEngine;
import engine.PropertyRules;
import engine.WildPropertyRules;
import model.card.Card;
import model.card.RentCard;
import model.card.WildpropertyCard;
import model.card.actionCard.*;
import model.enums.Color;
import model.player.Player;
import network.CardMapper;
import network.protocol.ClientMessage;
import network.protocol.EmojiCatalog;
import network.protocol.ServerMessage;

import java.util.List;

/** Handles gameplay commands routed from {@link GameSession}. */
final class GameSessionActions {
    private final ServerPlayHandler playHandler = new ServerPlayHandler();

    ServerMessage handleDraw(GameSession session, int seat) {
        if (session.pendingResolution() != null) {
            return error("Waiting for player response");
        }
        GameEngine engine = session.engine();
        if (engine == null) {
            return error("Game not started");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("Not your turn");
        }
        if (!engine.drawCardsForCurrentPlayer()) {
            return error("Cannot draw cards now");
        }
        session.appendLog(engine.getCurrentPlayer().getName() + " drew 2 cards");
        session.broadcastState();
        return ok("Drew 2 cards");
    }

    ServerMessage handleEndTurn(GameSession session, int seat) {
        if (session.pendingResolution() != null) {
            return error("Waiting for player response");
        }
        GameEngine engine = session.engine();
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
        session.appendLog(ending.getName() + " ended turn");
        session.advanceTurnLocked();
        session.broadcastState();
        return ok("Turn ended");
    }

    ServerMessage handleDiscardCard(GameSession session, int seat, ClientMessage message) {
        if (session.pendingResolution() != null) {
            return error("Waiting for player response");
        }
        GameEngine engine = session.engine();
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
        session.appendLog(player.getName() + " discarded " + card.getName());
        if (engine.isTurnOver() && engine.canEndTurn(player)) {
            session.appendLog(player.getName() + " played 3 cards, turn ending");
            session.advanceTurnLocked();
        }
        session.broadcastState();
        return ok("Card discarded");
    }

    ServerMessage handleRecolorWild(GameSession session, int seat, ClientMessage message) {
        if (session.pendingResolution() != null) {
            return error("Waiting for player response");
        }
        GameEngine engine = session.engine();
        if (engine == null) {
            return error("Game not started");
        }
        if (seat != engine.getCurrentPlayerIndex()) {
            return error("Not your turn");
        }
        if (!engine.hasDrawnThisTurn()) {
            return error("Draw cards before changing wild property color");
        }
        if (!engine.canPlayCard()) {
            return error("No plays remaining this turn");
        }
        if (message.cardId == null || message.cardId.isBlank()) {
            return error("Missing card id");
        }
        Color newColor = CardMapper.parseColor(message.color);
        if (newColor == null) {
            return error("Missing or invalid color");
        }

        Player player = engine.getCurrentPlayer();
        WildpropertyCard wild = findWildPropertyById(player, message.cardId);
        if (wild == null) {
            return error("Wild property not found on your board");
        }
        Color previous = wild.getChosenColor();
        if (!WildPropertyRules.recolor(player, wild, newColor)) {
            return error("Cannot change wild property to that color");
        }

        engine.recordCardPlayed();
        session.appendLog(player.getName() + " recolored wild: "
                + (previous != null ? previous.logKey() : "?") + " → " + newColor.logKey());
        session.afterSuccessfulPlay(player);
        session.broadcastState();
        return ok("Wild property recolored");
    }

    ServerMessage handlePlayCard(GameSession session, int seat, ClientMessage message) {
        if (session.pendingResolution() != null) {
            return error("Waiting for player response");
        }
        GameEngine engine = session.engine();
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
            case "BANK" -> playToBank(session, player, card);
            case "PROPERTY" -> playWildAsProperty(session, player, card, message);
            case "DOUBLE_RENT" -> playDoubleRentCombo(session, seat, player, message);
            case "EFFECT" -> playActionEffect(session, seat, player, card, message);
            default -> playSimpleCard(session, player, card);
        };

        if (!success) {
            return error(playFailureReason(player, card, message));
        }
        if (session.pendingResolution() != null) {
            return ok("Waiting for responses");
        }

        engine.recordCardPlayed();
        session.afterSuccessfulPlay(player);
        session.broadcastState();
        return ok("Card played");
    }

    ServerMessage handleEmoji(GameSession session, int seat, ClientMessage message) {
        if (session.engine() == null) {
            return error("Game not started");
        }
        if (seat < 0 || seat >= session.playerCount()) {
            return error("Invalid seat");
        }
        String emoji = message.emoji == null ? "" : message.emoji.trim();
        if (!EmojiCatalog.contains(emoji)) {
            return error("Choose an emoji to send");
        }
        session.broadcastEmoji(seat, emoji);
        return ok("Emoji sent");
    }

    ServerMessage handleRespond(GameSession session, int seat, ClientMessage message) {
        PendingActionResolution pending = session.pendingResolution();
        if (pending == null) {
            return error("No pending prompt");
        }
        if (!pending.handleResponse(seat, message)) {
            return error("Invalid response");
        }
        session.broadcastState();
        return ok("Response accepted");
    }

    boolean playToBank(GameSession session, Player player, Card card) {
        if (!(card instanceof ActionCard action)) {
            if (card instanceof WildpropertyCard wild && wild.isBankable()) {
                player.removeFromHand(wild);
                wild.depositToBank(player);
                session.appendLog(player.getName() + " banked " + wild.getName());
                return true;
            }
            return false;
        }
        player.removeFromHand(action);
        action.depositToBank(player);
        session.appendLog(player.getName() + " banked " + action.getName());
        return true;
    }

    private boolean playWildAsProperty(GameSession session, Player player, Card card, ClientMessage message) {
        if (!(card instanceof WildpropertyCard wild)) {
            return false;
        }
        Color color = CardMapper.parseColor(message.color);
        if (color == null || !PropertyRules.canAddBillableProperty(player, color)) {
            return false;
        }
        wild.setChosenColor(color);
        player.removeFromHand(wild);
        wild.use(player, session.engine());
        session.appendLog(player.getName() + " played " + color.logKey());
        return true;
    }

    private boolean playActionEffect(GameSession session, int seat, Player player, Card card, ClientMessage message) {
        if (!(card instanceof ActionCard action)) {
            return false;
        }
        if (action instanceof JustSayNo || action instanceof DoubleTheRent) {
            return false;
        }
        GameEngine engine = session.engine();
        if (PendingActionResolution.requiresInteraction(action, message)) {
            player.removeFromHand(action);
            engine.getDiscardPile().addCard(action);
            session.setPendingResolution(new PendingActionResolution(
                    session, engine, seat, action, message, session.logLines()));
            session.pendingResolution().begin();
            return true;
        }
        boolean ok = playHandler.applyEffect(engine, player, action, message, session.logLines());
        if (ok) {
            player.removeFromHand(action);
            engine.getDiscardPile().addCard(action);
        }
        return ok;
    }

    private boolean playDoubleRentCombo(GameSession session, int seat, Player player, ClientMessage message) {
        GameEngine engine = session.engine();
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

        session.setPendingUsesTwoPlays(true);
        session.setPendingResolution(PendingActionResolution.rentWithDouble(
                session, engine, seat, rentCard, message, session.logLines()));
        session.pendingResolution().begin();
        return true;
    }

    private boolean isValidRentChargeColor(RentCard rentCard, Player player, Color color) {
        if (rentCard.getChargeableColors(player).contains(color)) {
            return true;
        }
        return rentCard.isAllColors() && rentCard.countProperties(player, color) > 0;
    }

    private boolean playSimpleCard(GameSession session, Player player, Card card) {
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
        card.use(player, session.engine());
        player.removeFromHand(card);
        if (card instanceof model.card.MoneyCard) {
            session.appendLog(player.getName() + " banked " + card.getName());
        } else {
            session.appendLog(player.getName() + " played " + propertyPlayDetail(card));
        }
        return true;
    }

    private static WildpropertyCard findWildPropertyById(Player player, String cardId) {
        for (model.card.PropertyCard property : player.getAllProperties()) {
            if (property instanceof WildpropertyCard wild && cardId.equals(wild.getInstanceId())) {
                return wild;
            }
        }
        return null;
    }

    private static String propertyPlayDetail(Card card) {
        if (card instanceof model.card.PropertyCard property
                && !PropertyRules.isSetImprovement(property)
                && property.getColor() != null) {
            return property.getColor().logKey();
        }
        return card.getName();
    }

    private static String playFailureReason(Player player, Card card, ClientMessage message) {
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

    private static ServerMessage ok(String text) {
        ServerMessage msg = new ServerMessage();
        msg.type = network.protocol.MessageTypes.OK;
        msg.text = text;
        return msg;
    }

    private static ServerMessage error(String text) {
        ServerMessage msg = new ServerMessage();
        msg.type = network.protocol.MessageTypes.ERROR;
        msg.text = text;
        return msg;
    }
}
