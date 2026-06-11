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

/**
 * 从 {@link controller.NetworkGameController} 抽离的联机对局横切关注点。
 * <p>
 * 合并服务器日志行、显示胜利/重赛 UI、同步后协调手牌选中，
 * 并在结束回合前提示强制手牌上限弃牌。
 */
public final class NetworkMatchCoordinator {
    private final Label statusAnchor;
    private final GameLogPane gameLog;
    private final StatusMessageDisplay statusDisplay;
    private final Runnable onBoardRefresh;
    private final Runnable disableActionButtons;
    private final IntSupplier localSeatSupplier;
    private final Supplier<NetworkClient> clientSupplier;
    private final Consumer<String> onNewLogLine;

    /** 已合并进 {@link #gameLog} 的服务器日志行数。 */
    private int mergedLogSize;
    /** 防止每次游戏结束时重复显示胜利叠加层。 */
    private boolean victoryScreenShown;
    /** 防止胜利后重复弹出重赛投票对话框。 */
    private boolean rematchPromptShown;
    /** 确保每次拒绝重赛时「会话已结束」消息只显示一次。 */
    private boolean rematchDeclinedNotified;
    /** 上次显示的重赛赞成人数，避免重复等待状态。 */
    private int lastRematchYesCount = -1;
    /** 上一快照的游戏结束标志，用于检测重赛过渡。 */
    private boolean previousGameOver;
    /** 点击结束回合但因手牌未合法而被阻塞时置位。 */
    private boolean pendingEndTurnAfterDiscard;

    /**
     * @param statusAnchor         胜利/重赛对话框的所有者标签
     * @param gameLog              接收合并服务器日志的可滚动日志面板
     * @param statusDisplay        主要临时状态消息展示器
     * @param onBoardRefresh       日志合并后调用，供控制器重绘
     * @param disableActionButtons 显示胜利界面时调用
     * @param localSeatSupplier    本客户端座位索引，用于回合判断
     * @param clientSupplier       已连接客户端，用于结束回合与弃牌消息
     * @param onNewLogLine         每条新日志的可选音效/效果钩子
     */
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

    /** 是否已请求结束回合但因手牌未合法而被阻塞。 */
    public boolean isPendingEndTurnAfterDiscard() {
        return pendingEndTurnAfterDiscard;
    }

    /**
     * 记录手牌合法后应自动发送结束回合。
     *
     * @param pending 玩家在手牌超限时点击结束回合时为 {@code true}
     */
    public void setPendingEndTurnAfterDiscard(boolean pending) {
        this.pendingEndTurnAfterDiscard = pending;
    }

    /** 使用前确保可空的 DTO 集合非空。 */
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

    /**
     * 存储新快照后执行：重赛过渡、日志合并、UI 刷新、胜利界面。
     */
    public void onStateApplied(GameStateDto state) {
        handleRematchTransitions(state);
        mergeLog(state.logLines);
        onBoardRefresh.run();
        maybeShowVictory(state);
    }

    /**
     * 服务器同步后按实例 ID 将选中状态重新绑定到映射手牌。
     *
     * @return {@code mappedHand} 中的匹配卡牌；已不存在时为 {@code null}
     */
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

    /**
     * 本地玩家回合且手牌超限时提示弃牌；之后可能自动结束回合。
     */
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

    /** 显示手牌上限弃牌对话框并将选择发送给服务器。 */
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
