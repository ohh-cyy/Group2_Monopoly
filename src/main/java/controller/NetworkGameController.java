package controller;

import controller.dialog.GameDialogService;
import controller.gameplay.OnlineCardPlayService;
import controller.network.NetworkMatchCoordinator;
import controller.network.NetworkPromptResponder;
import controller.view.CardSelectionFeedback;
import controller.view.GameBoardRefreshService;
import controller.view.NetworkBoardRefreshService;
import engine.GameEngine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import model.achievement.AchievementManager;
import model.card.Card;
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
import ui.GameAudio;
import ui.GameLogPane;
import ui.StatusMessageDisplay;
import ui.layout.GameBoardChrome;
import ui.layout.PropertyBoardLayoutTracker;
import ui.render.BankBarRenderer;
import ui.render.HandRenderer;
import ui.render.PlayerListRenderer;
import ui.render.PublicBoardRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NetworkGameController {
    @FXML private Label currentPlayerLabel;
    @FXML private Label gameStatusText;
    @FXML private Label statusMessage;
    @FXML private VBox playersList;
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;
    @FXML private Button leftSidebarToggle;
    @FXML private Button rightSidebarToggle;
    @FXML private Button leftSidebarHandle;
    @FXML private Button rightSidebarHandle;
    @FXML private VBox handDock;
    @FXML private Label handDockHint;
    @FXML private Button handDockToggle;
    @FXML private VBox publicBoardPanel;
    @FXML private HBox publicBoardHeader;
    @FXML private VBox allPlayersPropertiesPanel;
    @FXML private HBox allPlayersBankBar;
    @FXML private ScrollPane playerHandScroll;
    @FXML private HBox playerHand;
    @FXML private FlowPane playerBank;
    @FXML private Label bankTotalLabel;
    @FXML private GameLogPane gameLog;
    @FXML private Button drawCardBtn;
    @FXML private Button discardCardBtn;
    @FXML private Button endTurnBtn;
    @FXML private Button newGameBtn;
    @FXML private Button achievementBtn;
    @FXML private FlowPane emojiBar;
    @FXML private Pane reactionOverlay;

    private HandRenderer handRenderer;
    private GameDialogService dialogs;
    private StatusMessageDisplay statusDisplay;
    private GameBoardChrome boardChrome;
    private PropertyBoardLayoutTracker layoutTracker;
    private GameBoardRefreshService boardRefresh;
    private NetworkMatchCoordinator matchCoordinator;
    private NetworkPromptResponder promptResponder;
    private OnlineCardPlayService onlineCardPlay;
    private HandRenderer.SelectionListener handSelectionListener;

    private NetworkClient client;
    private int localSeat = -1;
    private GameStateDto state;
    private List<Card> myHand = new ArrayList<>();
    private List<Card> myBank = new ArrayList<>();
    private Card selectedCard;
    private CardView selectedCardView;
    private Image avatarImage;
    private EmojiReactionOverlay emojiReactionOverlay;

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
            public void onCardDoubleClickPlay(Card card, CardView cardView) {
                if (!card.equals(selectedCard)) {
                    selectCard(card, cardView);
                }
                cardView.playActivationAnimation(() -> Platform.runLater(NetworkGameController.this::onPlay));
            }
        };

        boardRefresh = new NetworkBoardRefreshService(
                currentPlayerLabel, gameStatusText, publicBoardPanel, allPlayersPropertiesPanel,
                allPlayersBankBar, playerHandScroll, playerHand, playerBank, bankTotalLabel,
                drawCardBtn, discardCardBtn, endTurnBtn,
                handRenderer, new PlayerListRenderer(() -> avatarImage),
                new PublicBoardRenderer(() -> avatarImage), new BankBarRenderer(),
                () -> state, () -> myHand, () -> myBank, () -> localSeat, handSelectionListener);
        boardRefresh.applySelectionCallback((card, view) -> selectedCardView = view);
    }

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
            showStatus("No plays remaining this turn", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("Select a card first", true);
            return;
        }
        playSelectedCard();
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
