package controller.network;

import controller.dialog.GameDialogService;
import controller.dialog.HandDiscardDialogService;
import engine.GameEngine;
import javafx.application.Platform;
import javafx.scene.control.Label;
import ui.GameLogPane;
import model.card.Card;
import network.client.NetworkClient;
import network.protocol.GameStateDto;
import ui.GameAlertDialogs;
import ui.GameVictoryScreen;
import ui.StatusMessageDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Victory, rematch, hand-limit discard, and log merge for online matches. */
public final class NetworkMatchCoordinator {
    private final Label statusAnchor;
    private final GameLogPane gameLog;
    private final StatusMessageDisplay statusDisplay;
    private final Runnable onBoardRefresh;
    private final Runnable disableActionButtons;
    private final IntSupplier localSeatSupplier;
    private final Supplier<NetworkClient> clientSupplier;
    private final Consumer<String> onNewLogLine;

    private int mergedLogSize;
    private boolean victoryScreenShown;
    private boolean rematchPromptShown;
    private boolean rematchDeclinedNotified;
    private int lastRematchYesCount = -1;
    private boolean previousGameOver;
    private boolean pendingEndTurnAfterDiscard;

    public NetworkMatchCoordinator(Label statusAnchor,
                                   GameLogPane gameLog,
                                   StatusMessageDisplay statusDisplay,
                                   Runnable onBoardRefresh,
                                   Runnable disableActionButtons,
                                   IntSupplier localSeatSupplier,
                                   Supplier<NetworkClient> clientSupplier,
                                   Consumer<String> onNewLogLine) {
        this.statusAnchor = statusAnchor;
        this.gameLog = gameLog;
        this.statusDisplay = statusDisplay;
        this.onBoardRefresh = onBoardRefresh;
        this.disableActionButtons = disableActionButtons;
        this.localSeatSupplier = localSeatSupplier;
        this.clientSupplier = clientSupplier;
        this.onNewLogLine = onNewLogLine;
    }

    public boolean isPendingEndTurnAfterDiscard() {
        return pendingEndTurnAfterDiscard;
    }

    public void setPendingEndTurnAfterDiscard(boolean pending) {
        this.pendingEndTurnAfterDiscard = pending;
    }

    public void normalize(GameStateDto state) {
        if (state.players == null) {
            state.players = new ArrayList<>();
        }
        if (state.myHand == null) {
            state.myHand = new ArrayList<>();
        }
        if (state.myBank == null) {
            state.myBank = new ArrayList<>();
        }
        if (state.logLines == null) {
            state.logLines = new ArrayList<>();
        }
    }

    public void onStateApplied(GameStateDto state) {
        handleRematchTransitions(state);
        mergeLog(state.logLines);
        onBoardRefresh.run();
        maybeShowVictory(state);
    }

    public static Card reconcileSelection(List<Card> mappedHand, Card selectedCard) {
        if (selectedCard == null || mappedHand == null) {
            return null;
        }
        String selectedId = selectedCard.getInstanceId();
        return mappedHand.stream()
                .filter(card -> selectedId.equals(card.getInstanceId()))
                .findFirst()
                .orElse(null);
    }

    public void checkHandLimitAfterRefresh(GameStateDto state, List<Card> hand, GameDialogService dialogs) {
        if (!isMyTurn(state)) {
            pendingEndTurnAfterDiscard = false;
            return;
        }
        if (hand.size() <= GameEngine.MAX_HAND_SIZE) {
            if (pendingEndTurnAfterDiscard) {
                pendingEndTurnAfterDiscard = false;
                NetworkClient client = clientSupplier.get();
                if (client != null) {
                    client.endTurn();
                }
            }
            return;
        }
        if (mustResolveHandLimitDiscard(state, hand)) {
            Platform.runLater(() -> promptDiscardForHandLimit(state, hand, dialogs));
        }
    }

    private void handleRematchTransitions(GameStateDto state) {
        boolean wasGameOver = previousGameOver;
        previousGameOver = state.gameOver;
        if (wasGameOver && !state.gameOver) {
            victoryScreenShown = false;
            rematchPromptShown = false;
            rematchDeclinedNotified = false;
            lastRematchYesCount = -1;
            mergedLogSize = 0;
            if (gameLog != null) {
                gameLog.clear();
            }
            statusDisplay.show("A new game has started. Please draw your cards first.", false);
        }
        if (state.rematchDeclined && !rematchDeclinedNotified) {
            rematchDeclinedNotified = true;
            statusDisplay.show("A player chose not to continue. This session has ended.", false);
        }
        if (state.rematchOpen
                && Boolean.TRUE.equals(state.myRematchVote)
                && state.rematchYesCount < state.rematchRequired
                && state.rematchYesCount != lastRematchYesCount) {
            lastRematchYesCount = state.rematchYesCount;
            statusDisplay.show("Rematch vote recorded. Waiting for other players ("
                    + state.rematchYesCount + "/" + state.rematchRequired + ")", false);
        }
    }

    private void mergeLog(List<String> lines) {
        if (lines == null || gameLog == null) {
            return;
        }
        for (int i = mergedLogSize; i < lines.size(); i++) {
            String line = lines.get(i);
            gameLog.append(line);
            if (onNewLogLine != null) {
                onNewLogLine.accept(line);
            }
        }
        mergedLogSize = lines.size();
    }

    private void maybeShowVictory(GameStateDto state) {
        if (!state.gameOver || victoryScreenShown) {
            return;
        }
        if (state.winnerName == null || state.winnerName.isBlank()) {
            return;
        }
        victoryScreenShown = true;
        GameVictoryScreen.show(statusAnchor, state.winnerName, () -> promptRematchAfterVictory(state));
        disableActionButtons.run();
    }

    private void promptRematchAfterVictory(GameStateDto state) {
        if (rematchPromptShown || clientSupplier.get() == null) {
            return;
        }
        rematchPromptShown = true;
        GameAlertDialogs.askPlayAgain(
                statusAnchor,
                "Play another round with everyone? All players must vote yes to start a new game.",
                accept -> {
                    NetworkClient client = clientSupplier.get();
                    if (client == null) {
                        return;
                    }
                    client.voteRematch(accept);
                    if (accept) {
                        lastRematchYesCount = state.rematchYesCount;
                        statusDisplay.show("Rematch vote recorded. Waiting for other players...", false);
                    } else {
                        statusDisplay.show("You chose to end this session.", false);
                    }
                });
    }

    public void promptDiscardForHandLimit(GameStateDto state,
                                          List<Card> hand,
                                          GameDialogService dialogs) {
        if (!mustResolveHandLimitDiscard(state, hand)) {
            if (pendingEndTurnAfterDiscard && hand.size() <= GameEngine.MAX_HAND_SIZE) {
                pendingEndTurnAfterDiscard = false;
                NetworkClient client = clientSupplier.get();
                if (client != null) {
                    client.endTurn();
                }
            }
            return;
        }
        int excess = hand.size() - GameEngine.MAX_HAND_SIZE;
        Optional<Card> choice = HandDiscardDialogService.promptDiscardOne(
                helper -> dialogs.showChoiceDialog(
                        helper.title(),
                        helper.header(),
                        helper.prompt(),
                        helper.hand(),
                        Card::getName,
                        card -> null),
                hand,
                excess,
                pendingEndTurnAfterDiscard || state.remainingPlays <= 0);
        if (choice.isEmpty()) {
            statusDisplay.show("You must discard down to " + GameEngine.MAX_HAND_SIZE
                    + " cards before ending your turn", true);
            return;
        }
        NetworkClient client = clientSupplier.get();
        if (client != null) {
            client.discardCard(choice.get().getInstanceId());
        }
    }

    private boolean mustResolveHandLimitDiscard(GameStateDto state, List<Card> hand) {
        if (!isMyTurn(state) || hand.size() <= GameEngine.MAX_HAND_SIZE) {
            return false;
        }
        return pendingEndTurnAfterDiscard
                || (state.hasDrawnThisTurn && state.remainingPlays <= 0);
    }

    private boolean isMyTurn(GameStateDto state) {
        return state != null
                && !state.gameOver
                && state.currentPlayerIndex == localSeatSupplier.getAsInt();
    }
}
