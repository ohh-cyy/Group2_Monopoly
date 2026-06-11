package controller;

import controller.dialog.GameDialogService;
import controller.gameplay.OnlineCardPlayService;
import controller.network.NetworkMatchCoordinator;
import controller.network.NetworkPromptResponder;
import controller.network.OnlineTurnTimerDisplay;
import controller.view.CardSelectionFeedback;
import controller.view.GameBoardRefreshService;
import controller.view.NetworkBoardRefreshService;
import engine.GameEngine;
import engine.WildPropertyRules;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import model.achievement.AchievementManager;
import model.card.Card;
import model.card.PropertyCard;
import model.card.WildpropertyCard;
import model.enums.Color;
import model.player.Player;
import network.CardMapper;
import network.client.NetworkClient;
import network.protocol.ClientMessage;
import network.protocol.EmojiCatalog;
import network.protocol.GameStateDto;
import network.protocol.MessageTypes;
import network.protocol.ServerMessage;
import ui.AchievementUi;
import ui.AvatarResources;
import ui.CardView;
import ui.EmojiReactionOverlay;
import ui.GameAlertDialogs;
import ui.GameAudio;
import ui.GameLogPane;
import ui.StatusMessageDisplay;
import ui.YourTurnOverlay;
import ui.layout.GameBoardChrome;
import ui.layout.PropertyBoardLayoutTracker;
import ui.render.BankBarRenderer;
import ui.render.HandRenderer;
import ui.render.PlayerListRenderer;
import ui.render.PublicBoardRenderOptions;
import ui.render.PublicBoardRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 联机多人游戏视图的 FXML 控制器（{@code network-game-view.fxml}）。
 * <p>
 * 通过 {@link GameStateDto} 渲染服务器权威状态，经 {@link NetworkClient} 发送玩家操作，
 * 并处理服务器驱动的交互提示。
 */
public class NetworkGameController {
    /** 顶部状态栏中的回合摘要与牌堆标签。 */
    @FXML private Label currentPlayerLabel;
    @FXML private Label gameStatusText;
    @FXML private Label turnFlowLabel;
    @FXML private HBox playDotsBar;
    @FXML private HBox deckPilesBar;
    /** 棋盘下方显示的临时提示（错误使用独立样式）。 */
    @FXML private Label statusMessage;
    /** 左侧边栏，列出所有玩家及其银行总额。 */
    @FXML private VBox playersList;
    /** 可折叠侧边栏及其切换控件。 */
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;
    @FXML private Button leftSidebarToggle;
    @FXML private Button rightSidebarToggle;
    @FXML private Button leftSidebarHandle;
    @FXML private Button rightSidebarHandle;
    /** 底部手牌区：滚动区域、提示标签与折叠切换。 */
    @FXML private VBox handDock;
    @FXML private Label handDockHint;
    @FXML private Button handDockToggle;
    /** 中央棋盘：所有玩家的地产行与共享银行栏。 */
    @FXML private VBox publicBoardPanel;
    @FXML private HBox publicBoardHeader;
    @FXML private VBox allPlayersPropertiesPanel;
    @FXML private HBox allPlayersBankBar;
    /** 当前玩家的手牌与个人银行控件。 */
    @FXML private ScrollPane playerHandScroll;
    @FXML private HBox playerHand;
    @FXML private FlowPane playerBank;
    @FXML private Label bankTotalLabel;
    /** 右侧边栏中的可滚动事件日志。 */
    @FXML private GameLogPane gameLog;
    /** 主要回合操作按钮。 */
    @FXML private Button drawCardBtn;
    @FXML private Button discardCardBtn;
    @FXML private Button endTurnBtn;
    @FXML private Button newGameBtn;
    @FXML private Button achievementBtn;
    /** 表情反应选择器与浮动叠加层目标。 */
    @FXML private FlowPane emojiBar;
    @FXML private Pane reactionOverlay;
    /** 鼠标穿透的全棋盘层，承载本地「轮到你了」通知。 */
    @FXML private StackPane yourTurnOverlay;

    /** 渲染并跟踪手牌选中状态。 */
    private HandRenderer handRenderer;
    /** 出牌提示与万能改色的主题对话框。 */
    private GameDialogService dialogs;
    /** 展示临时状态与错误消息。 */
    private StatusMessageDisplay statusDisplay;
    /** 可折叠侧边栏与手牌区装饰。 */
    private GameBoardChrome boardChrome;
    /** 跟踪地产行高度以实现响应式棋盘布局。 */
    private PropertyBoardLayoutTracker layoutTracker;
    /** 按模式区分的棋盘刷新实现。 */
    private GameBoardRefreshService boardRefresh;
    /** 日志合并、胜利/重赛与手牌上限协调。 */
    private NetworkMatchCoordinator matchCoordinator;
    /** 处理服务器驱动的 Just Say No 与支付提示。 */
    private NetworkPromptResponder promptResponder;
    /** 发送至服务器前构建出牌消息。 */
    private OnlineCardPlayService onlineCardPlay;
    /** 服务器同步间隔内的客户端倒计时显示。 */
    private OnlineTurnTimerDisplay turnTimerDisplay;
    /** 将手牌单击/双击路由到选中/出牌。 */
    private HandRenderer.SelectionListener handSelectionListener;

    /** 活跃的套接字客户端；由 {@link #startOnlineGame} 设置。 */
    private NetworkClient client;
    /** 服务器分配的本客户端座位索引（加入前为 -1）。 */
    private int localSeat = -1;
    /** 服务器下发的最新权威快照。 */
    private GameStateDto state;
    /** 仅映射本地座位的银行与手牌。 */
    private List<Card> myHand = new ArrayList<>();
    /** 仅映射本地座位的银行卡牌。 */
    private List<Card> myBank = new ArrayList<>();
    /** 当前高亮用于弃牌或出牌的手牌。 */
    private Card selectedCard;
    /** {@link #selectedCard} 的视图控件，同步后用于恢复高亮。 */
    private CardView selectedCardView;
    /** 玩家列表与棋盘渲染器使用的默认头像。 */
    private Image avatarImage;
    /** 公共地产棋盘上的浮动表情反应。 */
    private EmojiReactionOverlay emojiReactionOverlay;
    /** 权威回合轮到本座位时显示的全棋盘通知。 */
    private YourTurnOverlay yourTurnNotification;
    /** 上次观察到的权威回合座位，用于在同步时抑制重复通知。 */
    private int lastObservedTurnSeat = -1;
    /** 允许同座位重赛时触发新的通知。 */
    private boolean lastObservedGameOver;

    /**
     * FXML 生命周期钩子：构建服务与棋盘刷新，但不连接服务器。
     * 调用 {@link #startOnlineGame} 以绑定客户端并应用初始状态。
     */
    @FXML
    public void initialize() {
        avatarImage = AvatarResources.loadDefaultAvatar(getClass());
        statusDisplay = new StatusMessageDisplay(statusMessage);
        dialogs = new GameDialogService(statusMessage);
        onlineCardPlay = new OnlineCardPlayService(dialogs, this::showStatus);
        promptResponder = new NetworkPromptResponder(dialogs, statusDisplay, this::showStatus);
        emojiReactionOverlay = new EmojiReactionOverlay(reactionOverlay, allPlayersPropertiesPanel);
        yourTurnNotification = new YourTurnOverlay(yourTurnOverlay);
        setupEmojiBar();

        boardChrome = new GameBoardChrome(
                leftSidebar, rightSidebar, leftSidebarToggle, rightSidebarToggle,
                leftSidebarHandle, rightSidebarHandle, handDock, handDockHint, handDockToggle);
        boardChrome.setup();

        initBoardRefresh();
        matchCoordinator = new NetworkMatchCoordinator(
                statusMessage, gameLog, statusDisplay, this::updateUi,
                () -> boardRefresh.disableActionButtons(),
                () -> localSeat, () -> client, GameAudio::playForGameLog);

        layoutTracker = new PropertyBoardLayoutTracker(
                allPlayersPropertiesPanel,
                () -> state != null ? state.players.size() : 0,
                this::updateUi);
        layoutTracker.attach();

        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, oldW, newW) -> {
                if (state != null) {
                    updateUi();
                }
            });
        }
    }

    private void initBoardRefresh() {
        handRenderer = new HandRenderer();
        handSelectionListener = new HandRenderer.SelectionListener() {
            @Override
            public void onCardSelected(Card card, CardView cardView) {
                selectCard(card, cardView);
            }

            @Override
            public void onCardPlayAttempt(Card card, CardView cardView) {
                if (!card.equals(selectedCard)) {
                    selectCard(card, cardView);
                }
                attemptPlayFromHand(card, cardView);
            }
        };

        boardRefresh = new NetworkBoardRefreshService(
                currentPlayerLabel, gameStatusText, turnFlowLabel, playDotsBar, deckPilesBar,
                publicBoardPanel, allPlayersPropertiesPanel,
                allPlayersBankBar, playerHandScroll, playerHand, playerBank, bankTotalLabel,
                drawCardBtn, discardCardBtn, endTurnBtn,
                handRenderer, new PlayerListRenderer(() -> avatarImage),
                new PublicBoardRenderer(() -> avatarImage), new BankBarRenderer(),
                () -> state, () -> myHand, () -> myBank, () -> localSeat, handSelectionListener);
        ((NetworkBoardRefreshService) boardRefresh).setBoardOptionsSupplier(this::buildWildRecolorOptions);
        boardRefresh.applySelectionCallback((card, view) -> selectedCardView = view);
        initTurnTimerDisplay();
    }

    private void initTurnTimerDisplay() {
        turnTimerDisplay = new OnlineTurnTimerDisplay(new OnlineTurnTimerDisplay.Host() {
            @Override
            public GameStateDto state() {
                return state;
            }

            @Override
            public boolean isMyTurn() {
                return NetworkGameController.this.isMyTurn();
            }

            @Override
            public String currentPlayerName() {
                if (state == null || state.players == null
                        || state.currentPlayerIndex < 0
                        || state.currentPlayerIndex >= state.players.size()) {
                    return "Player";
                }
                return state.players.get(state.currentPlayerIndex).name;
            }

            @Override
            public void refreshTimerLabel() {
                if (boardRefresh instanceof NetworkBoardRefreshService onlineRefresh) {
                    onlineRefresh.refreshTurnStatus();
                }
            }

            @Override
            public void onTurnWarning(String playerName) {
                showStatus(playerName + " has 10 seconds left to play", false);
            }
        });
    }

    /**
     * 绑定网络游戏，接收gameStateDto，刷新ui，通过networkClient发送消息
     */
    public void startOnlineGame(NetworkClient networkClient, int seat, GameStateDto initialState) {
        this.client = networkClient;
        this.localSeat = seat;
        if (client != null) {
            client.setListener(this::handleServerMessage);
            client.requestSync();
        }
        if (newGameBtn != null) {
            newGameBtn.setDisable(true);
        }
        setupButtons();
        Platform.runLater(() -> {
            AchievementUi.unlockAndShow(AchievementManager.CHOOSE_MODE, statusMessage);
            applyState(initialState);
        });
    }

    /** 暂停对话框打开期间暂停回合计时。 */
    public void pauseGame() {
        if (client != null) {
            client.pauseGame();
        }
        if (turnTimerDisplay != null) {
            turnTimerDisplay.pause();
        }
    }

    /** 关闭暂停对话框后恢复回合计时。 */
    public void resumeGame() {
        if (client != null) {
            client.resumeGame();
        }
        if (turnTimerDisplay != null) {
            turnTimerDisplay.resume();
        }
    }

    private void setupButtons() {
        if (drawCardBtn != null) drawCardBtn.setOnAction(e -> onDraw());
        if (discardCardBtn != null) discardCardBtn.setOnAction(e -> onDiscard());
        if (endTurnBtn != null) endTurnBtn.setOnAction(e -> onEndTurn());
        if (achievementBtn != null) {
            achievementBtn.setOnAction(e -> AchievementUi.showLibraryDialog(statusMessage));
        }
    }

    private void handleServerMessage(ServerMessage message) {
        Platform.runLater(() -> dispatchServerMessage(message));
    }

    private void dispatchServerMessage(ServerMessage message) {
            if (message == null) {
                return;
            }
            if (MessageTypes.STATE.equals(message.type) && message.state != null) {
                applyState(message.state);
            } else if (MessageTypes.PROMPT.equals(message.type)) {
                if (message.state != null) {
                    applyState(message.state);
                }
                if (message.prompt != null && message.prompt.responderSeat == localSeat) {
                    promptResponder.handle(client, message.prompt);
                }
            } else if (MessageTypes.ERROR.equals(message.type)) {
                showStatus(message.text, true);
            } else if (MessageTypes.EMOJI.equals(message.type)) {
                GameAudio.play(GameAudio.Cue.EMOJI);
                emojiReactionOverlay.show(message.seat, message.emoji);
            } else if (MessageTypes.GAME_STARTED.equals(message.type) && message.state != null) {
                applyState(message.state);
            }
    }

    private void applyState(GameStateDto newState) {
        if (newState == null) {
            statusDisplay.show("Waiting for server to sync game state...", false);
            if (client != null) {
                client.requestSync();
            }
            return;
        }
        matchCoordinator.normalize(newState);
        showYourTurnIfNeeded(newState);
        this.state = newState;
        myHand = CardMapper.fromDtos(newState.myHand);
        myBank = CardMapper.fromDtos(newState.myBank);
        selectedCard = NetworkMatchCoordinator.reconcileSelection(myHand, selectedCard);
        if (selectedCard == null) {
            selectedCardView = null;
        }
        matchCoordinator.onStateApplied(newState);
        matchCoordinator.checkHandLimitAfterRefresh(newState, myHand, dialogs);
        if (turnTimerDisplay != null) {
            if (newState.gameOver) {
                turnTimerDisplay.stop();
            } else {
                turnTimerDisplay.onStateUpdated();
            }
        }
    }

    /**
     * 仅在有意义的回合边界显示本地回合通知。
     * 普通 STATE 消息在摸牌、出牌、支付与计时器更新后到达，比较座位可避免
     * 这些常规同步重复播放叠加层。
     */
    private void showYourTurnIfNeeded(GameStateDto newState) {
        boolean freshGame = lastObservedGameOver && !newState.gameOver;
        boolean turnChanged = newState.currentPlayerIndex != lastObservedTurnSeat;
        boolean localTurn = !newState.gameOver && newState.currentPlayerIndex == localSeat;
        if (localTurn && (turnChanged || freshGame)) {
            yourTurnNotification.show();
        }
        lastObservedTurnSeat = newState.currentPlayerIndex;
        lastObservedGameOver = newState.gameOver;
    }

    private void onPlay() {
        if (!isMyTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (!state.hasDrawnThisTurn) {
            showStatus("Please click Draw Cards first before playing a card", true);
            return;
        }
        if (state.remainingPlays <= 0) {
            GameAlertDialogs.showNoPlaysRemaining(dialogOwner());
            return;
        }
        if (selectedCard == null) {
            showStatus("Select a card first", true);
            return;
        }
        playSelectedCard();
    }

    private void attemptPlayFromHand(Card card, CardView cardView) {
        if (!validatePlayFromHand()) {
            return;
        }
        cardView.playActivationAnimation(() -> Platform.runLater(() -> {
            if (validatePlayFromHand()) {
                playSelectedCard();
            }
        }));
    }

    private boolean validatePlayFromHand() {
        if (!isMyTurn()) {
            showStatus("It's not your turn", true);
            return false;
        }
        if (state == null || state.gameOver) {
            return false;
        }
        if (!state.hasDrawnThisTurn) {
            showStatus("Please click Draw Cards first before playing a card", true);
            return false;
        }
        if (state.remainingPlays <= 0) {
            GameAlertDialogs.showNoPlaysRemaining(dialogOwner());
            return false;
        }
        if (selectedCard == null) {
            showStatus("Select a card first", true);
            return false;
        }
        return true;
    }

    private Node dialogOwner() {
        if (playerHand != null && playerHand.getScene() != null) {
            return playerHand;
        }
        if (publicBoardPanel != null && publicBoardPanel.getScene() != null) {
            return publicBoardPanel;
        }
        return statusMessage;
    }

    private void onDraw() {
        if (!isMyTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        client.draw();
        AchievementUi.unlockAndShow(AchievementManager.FIRST_DRAW, statusMessage);
    }

    private void onEndTurn() {
        if (!isMyTurn()) {
            showStatus("Not your turn yet", true);
            return;
        }
        if (myHand.size() > GameEngine.MAX_HAND_SIZE) {
            matchCoordinator.setPendingEndTurnAfterDiscard(true);
            matchCoordinator.promptDiscardForHandLimit(state, myHand, dialogs);
            return;
        }
        client.endTurn();
    }

    private void onDiscard() {
        if (!isMyTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (!state.hasDrawnThisTurn) {
            showStatus("Please click Draw Cards first", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("Please select a card from your hand to discard", true);
            return;
        }
        Card card = selectedCard;
        selectedCard = null;
        selectedCardView = null;
        client.discardCard(card.getInstanceId());
        showStatus("Discarded " + card.getName(), false);
    }

    private void playSelectedCard() {
        Card card = selectedCard;
        CardView sourceView = selectedCardView;
        selectedCard = null;
        selectedCardView = null;
        Optional<ClientMessage> built = onlineCardPlay.buildPlayMessage(state, localSeat, myHand, card);
        if (built.isEmpty()) {
            restoreSelectedCard(card, sourceView);
            return;
        }
        client.playCard(built.get());
        AchievementUi.unlockAndShow(AchievementManager.FIRST_PLAY, statusMessage);
    }

    private void restoreSelectedCard(Card card, CardView sourceView) {
        selectedCard = card;
        selectedCardView = sourceView;
        if (sourceView != null && sourceView.getScene() != null) {
            handRenderer.clearSelection(playerHand);
            sourceView.setSelected(true);
            return;
        }
        handRenderer.applySelection(playerHand, card, (c, view) -> selectedCardView = view);
    }

    private boolean isMyTurn() {
        return state != null && !state.gameOver && state.currentPlayerIndex == localSeat;
    }

    private void updateUi() {
        if (state == null) {
            return;
        }
        boardRefresh.setSelectedCard(selectedCard);
        boardRefresh.refreshAll(playersList, layoutTracker::setLastRowHeight);
    }

    private void selectCard(Card card, CardView cv) {
        if (card.equals(selectedCard)) {
            return;
        }
        handRenderer.clearSelection(playerHand);
        selectedCard = card;
        selectedCardView = cv;
        cv.setSelected(true);
        showStatus(CardSelectionFeedback.messageForOnline(card), false);
        boardRefresh.setSelectedCard(selectedCard);
        boardRefresh.refreshButtons();
    }

    private void showStatus(String text, boolean error) {
        statusDisplay.show(text, error);
    }

    private PublicBoardRenderOptions buildWildRecolorOptions() {
        if (state == null || localSeat < 0 || !isMyTurn() || !state.hasDrawnThisTurn || state.gameOver) {
            return PublicBoardRenderOptions.none();
        }
        return new PublicBoardRenderOptions(
                this::onWildPropertyRecolorRequested,
                localSeat,
                true,
                playerViewFromLocalState());
    }

    private void onWildPropertyRecolorRequested(WildpropertyCard wild, int ownerSeat) {
        if (client == null || state == null || ownerSeat != localSeat || !isMyTurn()) {
            return;
        }
        if (!state.hasDrawnThisTurn) {
            showStatus("Draw cards before changing wild property color", true);
            return;
        }
        if (state.remainingPlays <= 0) {
            GameAlertDialogs.showNoPlaysRemaining(dialogOwner());
            return;
        }

        Player view = playerViewFromLocalState();
        WildpropertyCard owned = WildPropertyRules.findOwnedWild(view, wild);
        if (owned == null) {
            showStatus("Wild property not found on your board", true);
            return;
        }

        List<Color> options = WildPropertyRules.getRecolorOptions(view, owned);
        if (options.isEmpty()) {
            showStatus("No alternate colors available (target set may be complete)", true);
            return;
        }

        GameAudio.play(GameAudio.Cue.BUTTON);
        Color current = owned.getChosenColor();
        Optional<Color> chosen = dialogs.showChoiceDialog(
                "Change Wild Property Color",
                owned.getName(),
                "Current color: " + current + "\nChoose a new color (uses 1 play):",
                options,
                color -> color + "  —  change to " + color,
                color -> "-fx-background-color: " + dialogs.cssColorFor(color) + ";"
                        + "-fx-text-fill: " + dialogs.textColorFor(color) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
        if (chosen.isEmpty()) {
            return;
        }

        client.recolorWildProperty(owned.getInstanceId(), chosen.get().name());
        showStatus("Changing wild property color...", false);
    }

    private Player playerViewFromLocalState() {
        Player view = new Player("view");
        if (state == null || state.players == null) {
            return view;
        }
        for (var dto : state.players) {
            if (dto.seat != localSeat || dto.properties == null) {
                continue;
            }
            for (var cardDto : dto.properties) {
                Card card = CardMapper.fromDto(cardDto);
                if (card instanceof PropertyCard property) {
                    view.addProperty(property);
                }
            }
            break;
        }
        return view;
    }

    private void setupEmojiBar() {
        if (emojiBar == null) {
            return;
        }
        emojiBar.getChildren().clear();
        for (String emoji : EmojiCatalog.ALL) {
            Button button = new Button(emoji);
            button.getStyleClass().add("emoji-button");
            button.setFocusTraversable(false);
            button.setTooltip(new Tooltip(EmojiCatalog.nameFor(emoji)));
            button.setOnAction(event -> sendEmoji(emoji));
            emojiBar.getChildren().add(button);
        }
    }

    private void sendEmoji(String emoji) {
        if (client != null && state != null && !state.gameOver) {
            client.sendEmoji(emoji);
        }
    }
}
