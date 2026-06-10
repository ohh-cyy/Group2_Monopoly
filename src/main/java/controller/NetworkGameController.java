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
 * FXML controller for the online multiplayer game view ({@code network-game-view.fxml}).
 * <p>
 * Renders authoritative state from the server via {@link GameStateDto}, sends player
 * actions through {@link NetworkClient}, and handles server-driven interaction prompts.
 */
public class NetworkGameController {
    /** Turn summary and deck pile labels in the top status bar. */
    @FXML private Label currentPlayerLabel;
    @FXML private Label gameStatusText;
    @FXML private Label turnFlowLabel;
    @FXML private HBox playDotsBar;
    @FXML private HBox deckPilesBar;
    /** Transient hint shown below the board (errors use a distinct style). */
    @FXML private Label statusMessage;
    /** Left sidebar listing all players and their bank totals. */
    @FXML private VBox playersList;
    /** Collapsible side panels and their toggle controls. */
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;
    @FXML private Button leftSidebarToggle;
    @FXML private Button rightSidebarToggle;
    @FXML private Button leftSidebarHandle;
    @FXML private Button rightSidebarHandle;
    /** Bottom hand dock: scroll area, hint label, and collapse toggle. */
    @FXML private VBox handDock;
    @FXML private Label handDockHint;
    @FXML private Button handDockToggle;
    /** Center board: all players' property rows and shared bank bar. */
    @FXML private VBox publicBoardPanel;
    @FXML private HBox publicBoardHeader;
    @FXML private VBox allPlayersPropertiesPanel;
    @FXML private HBox allPlayersBankBar;
    /** Current player's hand and personal bank widgets. */
    @FXML private ScrollPane playerHandScroll;
    @FXML private HBox playerHand;
    @FXML private FlowPane playerBank;
    @FXML private Label bankTotalLabel;
    /** Scrollable event log in the right sidebar. */
    @FXML private GameLogPane gameLog;
    /** Primary turn action buttons. */
    @FXML private Button drawCardBtn;
    @FXML private Button discardCardBtn;
    @FXML private Button endTurnBtn;
    @FXML private Button newGameBtn;
    @FXML private Button achievementBtn;
    /** Emoji reaction picker and floating overlay target. */
    @FXML private FlowPane emojiBar;
    @FXML private Pane reactionOverlay;

    /** Renders and tracks hand card selection state. */
    private HandRenderer handRenderer;
    /** Themed dialogs for play prompts and wild recolor. */
    private GameDialogService dialogs;
    /** Presents transient status and error messages. */
    private StatusMessageDisplay statusDisplay;
    /** Collapsible sidebar and hand-dock chrome. */
    private GameBoardChrome boardChrome;
    /** Tracks property row heights for responsive board layout. */
    private PropertyBoardLayoutTracker layoutTracker;
    /** Mode-specific board refresh implementation. */
    private GameBoardRefreshService boardRefresh;
    /** Log merge, victory/rematch, and hand-limit coordination. */
    private NetworkMatchCoordinator matchCoordinator;
    /** Handles server-driven Just Say No and payment prompts. */
    private NetworkPromptResponder promptResponder;
    /** Builds play messages before sending to the server. */
    private OnlineCardPlayService onlineCardPlay;
    /** Client-side countdown display between server syncs. */
    private OnlineTurnTimerDisplay turnTimerDisplay;
    /** Routes hand click and double-click to selection/play. */
    private HandRenderer.SelectionListener handSelectionListener;

    /** Active socket client; set by {@link #startOnlineGame}. */
    private NetworkClient client;
    /** This client's seat index assigned by the server (-1 until joined). */
    private int localSeat = -1;
    /** Latest authoritative snapshot from the server. */
    private GameStateDto state;
    /** Mapped hand and bank cards for the local seat only. */
    private List<Card> myHand = new ArrayList<>();
    /** Mapped bank cards for the local seat only. */
    private List<Card> myBank = new ArrayList<>();
    /** Hand card currently highlighted for discard or play. */
    private Card selectedCard;
    /** View widget for {@link #selectedCard}, used to restore highlight after sync. */
    private CardView selectedCardView;
    /** Default avatar image for player list and board renderers. */
    private Image avatarImage;
    /** Floating emoji reactions over the public property board. */
    private EmojiReactionOverlay emojiReactionOverlay;

    /**
     * FXML lifecycle hook: builds services and board refresh without connecting to a server.
     * Call {@link #startOnlineGame} to attach the client and apply initial state.
     */
    @FXML
    public void initialize() {
        avatarImage = AvatarResources.loadDefaultAvatar(getClass());
        statusDisplay = new StatusMessageDisplay(statusMessage);
        dialogs = new GameDialogService(statusMessage);
        onlineCardPlay = new OnlineCardPlayService(dialogs, this::showStatus);
        promptResponder = new NetworkPromptResponder(dialogs, statusDisplay, this::showStatus);
        emojiReactionOverlay = new EmojiReactionOverlay(reactionOverlay, allPlayersPropertiesPanel);
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
     * Binds this controller to an active network session and renders the first state snapshot.
     *
     * @param networkClient connected client whose listener is routed here
     * @param seat          local player's seat index
     * @param initialState  first {@link GameStateDto} from the server (may be refreshed via sync)
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

    /** Pauses the turn countdown while the pause dialog is open. */
    public void pauseGame() {
        if (client != null) {
            client.pauseGame();
        }
        if (turnTimerDisplay != null) {
            turnTimerDisplay.pause();
        }
    }

    /** Resumes the turn countdown after closing the pause dialog. */
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
