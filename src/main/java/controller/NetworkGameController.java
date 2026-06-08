package controller;

import controller.dialog.GameDialogService;
import controller.dialog.HandDiscardDialogService;
import controller.gameplay.OnlineCardPlayService;
import engine.PaymentTransfer;
import engine.GameEngine;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.TranslateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.util.Duration;
import model.achievement.AchievementManager;
import model.card.*;
import model.card.actionCard.*;
import model.enums.Color;
import network.CardMapper;
import network.client.NetworkClient;
import network.protocol.ClientMessage;
import network.protocol.GameStateDto;
import network.protocol.InteractionPromptDto;
import network.protocol.MessageTypes;
import network.protocol.PlayerViewDto;
import network.protocol.ServerMessage;
import network.server.PendingActionResolution;
import ui.AchievementUi;
import ui.CardView;
import ui.GameAlertDialogs;
import ui.GameVictoryScreen;
import ui.PublicPropertyBoardLayout;
import ui.render.BankBarRenderer;
import ui.render.HandRenderer;
import ui.render.PlayerBoardView;
import ui.render.PlayerListRenderer;
import ui.render.PublicBoardRenderer;

import java.util.*;

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
    @FXML private TextArea gameLog;
    @FXML private HBox emojiBar;
    @FXML private Button drawCardBtn;
    @FXML private Button discardCardBtn;
    @FXML private Button endTurnBtn;
    @FXML private Button newGameBtn;
    @FXML private Button achievementBtn;

    private static final double HAND_DOCK_HEIGHT = 266;
    private static final double HAND_DOCK_PEEK = 54;
    private static final int MAX_PLAYS_PER_TURN = 3;
    private static final int TURN_WARNING_SECONDS = 10;
    private static final List<String> EMOJIS = List.of("😀", "😂", "😎", "😮", "👏", "💰", "🎲", "🔥");

    private HandRenderer handRenderer;
    private PlayerListRenderer playerListRenderer;
    private PublicBoardRenderer publicBoardRenderer;
    private BankBarRenderer bankBarRenderer;
    private HandRenderer.SelectionListener handSelectionListener;
    private GameDialogService dialogs;
    private OnlineCardPlayService onlineCardPlay;

    private NetworkClient client;
    private int localSeat = -1;
    private GameStateDto state;
    private List<Card> myHand = new ArrayList<>();
    private List<Card> myBank = new ArrayList<>();
    private Card selectedCard;
    private CardView selectedCardView;
    private int mergedLogSize;
    private Timeline countdownTimer;
    private int turnSecondsRemaining;
    private boolean turnWarningShown;
    private int lastTimerTurnIndex = -1;
    private long lastTurnDeadline = -1;
    private boolean handDockExpanded = false;
    private boolean victoryScreenShown = false;
    private boolean rematchPromptShown = false;
    private boolean rematchDeclinedNotified = false;
    private int lastRematchYesCount = -1;
    private boolean previousGameOver = false;
    private double lastPropertyRowHeight = -1;
    private boolean pendingEndTurnAfterDiscard = false;
    private Image avatarImage;
    private String lastStatusMessage = "";
    private boolean lastStatusError = false;
    private FadeTransition statusFadeTransition;

    @FXML
    public void initialize() {
        loadAvatarImage();
        dialogs = new GameDialogService(statusMessage);
        onlineCardPlay = new OnlineCardPlayService(dialogs, this::showStatus);
        handRenderer = new HandRenderer();
        playerListRenderer = new PlayerListRenderer(() -> avatarImage);
        publicBoardRenderer = new PublicBoardRenderer(() -> avatarImage);
        bankBarRenderer = new BankBarRenderer();
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
        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, oldW, newW) -> {
                if (state != null) {
                    updateHandOnline();
                }
            });
        }
        setupEmojiBar();
        setupCollapsibleSidebars();
        setupHandDockInteractions();
        setupPublicBoardSizing();
    }

    public void startOnlineGame(NetworkClient networkClient, int seat, GameStateDto initialState) {
        this.client = networkClient;
        this.localSeat = seat;
        if (client != null) {
            client.setListener(this::handleMessage);
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
        if (achievementBtn != null) achievementBtn.setOnAction(e -> AchievementUi.showLibraryDialog(statusMessage));
    }


    private void setupEmojiBar() {
        if (emojiBar == null) {
            return;
        }
        emojiBar.getChildren().clear();
        for (String emoji : EMOJIS) {
            Button button = new Button(emoji);
            button.getStyleClass().add("emoji-button");
            button.setFocusTraversable(false);
            button.setOnAction(e -> sendEmoji(emoji));
            emojiBar.getChildren().add(button);
        }
    }

    private void sendEmoji(String emoji) {
        if (client == null || state == null || state.gameOver) {
            return;
        }
        client.sendEmoji(emoji);
        showStatus("Sent " + emoji, false);
    }

    private void syncTurnCountdown(GameStateDto newState) {
        if (newState == null || newState.gameOver || newState.turnDeadlineEpochMillis <= 0) {
            stopCountdownTimer();
            turnSecondsRemaining = 0;
            lastTimerTurnIndex = -1;
            lastTurnDeadline = -1;
            return;
        }
        boolean newTurn = newState.currentPlayerIndex != lastTimerTurnIndex
                || newState.turnDeadlineEpochMillis != lastTurnDeadline
                || countdownTimer == null;
        lastTimerTurnIndex = newState.currentPlayerIndex;
        lastTurnDeadline = newState.turnDeadlineEpochMillis;
        updateTurnSecondsFromDeadline();
        if (newTurn) {
            turnWarningShown = false;
            startCountdownTimer();
        }
    }

    private void startCountdownTimer() {
        stopCountdownTimer();
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            updateTurnSecondsFromDeadline();
            maybeWarnForCountdown();
            updateLabelsOnline();
            if (turnSecondsRemaining <= 0) {
                stopCountdownTimer();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void stopCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
            countdownTimer = null;
        }
    }

    private void updateTurnSecondsFromDeadline() {
        if (state == null || state.turnDeadlineEpochMillis <= 0) {
            turnSecondsRemaining = 0;
            return;
        }
        long millisLeft = state.turnDeadlineEpochMillis - System.currentTimeMillis();
        turnSecondsRemaining = (int) Math.max(0, Math.ceil(millisLeft / 1000.0));
    }

    private void maybeWarnForCountdown() {
        if (state == null || turnWarningShown
                || turnSecondsRemaining <= 0
                || turnSecondsRemaining > TURN_WARNING_SECONDS) {
            return;
        }
        turnWarningShown = true;
        if (state.currentPlayerIndex == localSeat) {
            showStatus("You have 10 seconds left to play", false);
        }
    }

    private void setupCollapsibleSidebars() {
        setLeftSidebarOpen(false);
        setRightSidebarOpen(false);
        if (leftSidebarToggle != null) {
            leftSidebarToggle.setOnAction(e -> setLeftSidebarOpen(false));
        }
        if (leftSidebarHandle != null) {
            leftSidebarHandle.setOnAction(e -> setLeftSidebarOpen(true));
        }
        if (rightSidebarToggle != null) {
            rightSidebarToggle.setOnAction(e -> setRightSidebarOpen(false));
        }
        if (rightSidebarHandle != null) {
            rightSidebarHandle.setOnAction(e -> setRightSidebarOpen(true));
        }
    }

    private void setLeftSidebarOpen(boolean open) {
        setNodeLayoutVisible(leftSidebar, open);
        setNodeLayoutVisible(leftSidebarHandle, !open);
    }

    private void setRightSidebarOpen(boolean open) {
        setNodeLayoutVisible(rightSidebar, open);
        setNodeLayoutVisible(rightSidebarHandle, !open);
    }

    private void setNodeLayoutVisible(javafx.scene.Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setupHandDockInteractions() {
        if (handDock == null) {
            return;
        }
        setHandDockExpanded(false, false);
        if (handDockToggle != null) {
            handDockToggle.setOnAction(e -> setHandDockExpanded(!handDockExpanded, true));
        }
    }

    private void setHandDockExpanded(boolean expanded, boolean animate) {
        if (handDock == null || (handDockExpanded == expanded && animate)) {
            return;
        }
        handDockExpanded = expanded;
        double collapsedY = HAND_DOCK_HEIGHT - HAND_DOCK_PEEK;
        double targetY = expanded ? 0 : collapsedY;
        if (handDockToggle != null) {
            handDockToggle.setText(expanded ? "Hide Hand ▼" : "Show Hand ▲");
        }
        if (handDockHint != null) {
            handDockHint.setText(expanded
                    ? "Double-click a card to play"
                    : "Click Show Hand to view your cards");
        }
        if (expanded) {
            handDock.toFront();
        }
        if (!animate) {
            handDock.setTranslateY(targetY);
            return;
        }
        TranslateTransition transition = new TranslateTransition(Duration.millis(280), handDock);
        transition.setToY(targetY);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.play();
    }

    private void setupPublicBoardSizing() {
        if (allPlayersPropertiesPanel == null) {
            return;
        }
        allPlayersPropertiesPanel.widthProperty().addListener((obs, oldW, newW) -> {
            if (newW.doubleValue() > 0 && state != null) {
                updateUi();
            }
        });
        attachTableAreaHeightListener();
    }

    private void attachTableAreaHeightListener() {
        javafx.scene.Parent node = allPlayersPropertiesPanel;
        while (node != null && !node.getStyleClass().contains("table-area")) {
            node = node.getParent();
        }
        if (!(node instanceof Region tableArea)) {
            return;
        }
        tableArea.heightProperty().addListener((obs, oldH, newH) -> {
            if (newH.doubleValue() <= 0) {
                return;
            }
            refreshPropertyAreaLayout();
        });
    }

    private void refreshPropertyAreaLayout() {
        if (state == null) {
            PublicPropertyBoardLayout.applyEqualRows(allPlayersPropertiesPanel);
            return;
        }
        PublicPropertyBoardLayout.applyEqualRows(allPlayersPropertiesPanel, state.players.size());
        maybeRescalePropertyCards();
    }

    private void maybeRescalePropertyCards() {
        if (allPlayersPropertiesPanel == null || state == null) {
            return;
        }
        double rowHeight = PublicPropertyBoardLayout.rowHeightFor(allPlayersPropertiesPanel, state.players.size());
        if (rowHeight <= 0 || Math.abs(rowHeight - lastPropertyRowHeight) <= 2) {
            return;
        }
        lastPropertyRowHeight = rowHeight;
        updateUi();
    }


    private void handleMessage(ServerMessage message) {
        Platform.runLater(() -> {
            if (message == null) return;
            if (MessageTypes.STATE.equals(message.type) && message.state != null) {
                applyState(message.state);
            } else if (MessageTypes.PROMPT.equals(message.type)) {
                if (message.state != null) {
                    applyState(message.state);
                }
                if (message.prompt != null && message.prompt.responderSeat == localSeat) {
                    handleInteractionPrompt(message.prompt);
                }
            } else if (MessageTypes.ERROR.equals(message.type)) {
                showStatus(message.text, true);
            } else if (MessageTypes.GAME_STARTED.equals(message.type) && message.state != null) {
                applyState(message.state);
            }
        });
    }

    private void handleInteractionPrompt(InteractionPromptDto prompt) {
        if (client == null || prompt == null || prompt.promptId == null) {
            return;
        }
        if (PendingActionResolution.PROMPT_JUST_SAY_NO.equals(prompt.promptType)) {
            respondJustSayNo(prompt);
        } else if (PendingActionResolution.PROMPT_PAYMENT.equals(prompt.promptType)) {
            respondPayment(prompt);
        }
    }

    private void respondJustSayNo(InteractionPromptDto prompt) {
        ButtonType playBtn = new ButtonType("Play Just Say No");
        ButtonType allowBtn = new ButtonType("Allow Effect");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        String header = prompt.responseDepth == 0
                ? "Block " + prompt.attackerName + "'s action?"
                : "Counter the previous Just Say No?";
        String content = prompt.responseDepth == 0
                ? prompt.actionName + "\n\nPlaying Just Say No cancels this effect."
                : prompt.actionName + "\n\nPlaying Just Say No cancels the previous Just Say No.";
        Optional<ButtonType> choice = dialogs.showButtonDialog(
                "Just Say No", header, content, playBtn, allowBtn, cancelBtn);
        boolean useJustSayNo = choice.isPresent() && choice.get() == playBtn;
        client.respond(prompt.promptId, useJustSayNo, null);
        showStatus(useJustSayNo ? "You played Just Say No." : "You allowed the effect.", false);
    }

    private void respondPayment(InteractionPromptDto prompt) {
        List<Card> options = new ArrayList<>();
        if (prompt.payableCards != null) {
            for (var dto : prompt.payableCards) {
                Card card = CardMapper.fromDto(dto);
                if (card != null) {
                    options.add(card);
                }
            }
        }
        if (options.isEmpty()) {
            showStatus("No assets available to pay.", true);
            return;
        }
        Optional<Card> chosen = dialogs.showChoiceDialog(
                "Choose Payment Asset",
                "You must pay " + prompt.remainingDue + "M",
                prompt.actionName + "\nChoose one bank card or property to pay. Properties in complete sets cannot be used.",
                options,
                this::describePaymentCard,
                this::paymentCardStyle);
        if (chosen.isEmpty()) {
            respondPayment(prompt);
            return;
        }
        client.respond(prompt.promptId, null, chosen.get().getInstanceId());
        showStatus("Paid with " + chosen.get().getName(), false);
    }

    private String describePaymentCard(Card card) {
        if (card instanceof PropertyCard propertyCard) {
            Color color = propertyCard.getColor() != null ? propertyCard.getColor() : Color.BROWN;
            return card.getName() + " (" + color + ", " + PaymentTransfer.getPaymentValue(card) + "M)";
        }
        return card.getName() + " (" + PaymentTransfer.getPaymentValue(card) + "M)";
    }

    private String paymentCardStyle(Card card) {
        if (card instanceof PropertyCard propertyCard && propertyCard.getColor() != null) {
            return "-fx-border-color: " + dialogs.cssColorFor(propertyCard.getColor()) + ";";
        }
        if (card instanceof model.card.MoneyCard) {
            return "-fx-border-color: #27ae60;";
        }
        return "-fx-border-color: #d64545;";
    }

    private void applyState(GameStateDto newState) {
        if (newState == null) {
            showStatus("Waiting for server to sync game state...", false);
            if (client != null) {
                client.requestSync();
            }
            return;
        }
        if (newState.players == null) {
            newState.players = new ArrayList<>();
        }
        if (newState.myHand == null) {
            newState.myHand = new ArrayList<>();
        }
        if (newState.myBank == null) {
            newState.myBank = new ArrayList<>();
        }
        if (newState.logLines == null) {
            newState.logLines = new ArrayList<>();
        }
        boolean wasGameOver = previousGameOver;
        this.state = newState;
        previousGameOver = newState.gameOver;
        syncTurnCountdown(newState);
        if (wasGameOver && !newState.gameOver) {
            victoryScreenShown = false;
            rematchPromptShown = false;
            rematchDeclinedNotified = false;
            lastRematchYesCount = -1;
            showStatus("新一局已开始，请先抽牌。", false);
        }
        if (newState.rematchDeclined && !rematchDeclinedNotified) {
            rematchDeclinedNotified = true;
            showStatus("有玩家选择不继续，本局结束。", false);
        }
        if (newState.rematchOpen
                && Boolean.TRUE.equals(newState.myRematchVote)
                && newState.rematchYesCount < newState.rematchRequired
                && newState.rematchYesCount != lastRematchYesCount) {
            lastRematchYesCount = newState.rematchYesCount;
            showStatus("已选择再来一局，等待其他玩家（"
                    + newState.rematchYesCount + "/" + newState.rematchRequired + "）", false);
        }
        myHand = CardMapper.fromDtos(newState.myHand);
        myBank = CardMapper.fromDtos(newState.myBank);
        if (selectedCard != null) {
            String selectedId = selectedCard.getInstanceId();
            selectedCard = myHand.stream()
                    .filter(card -> selectedId.equals(card.getInstanceId()))
                    .findFirst()
                    .orElse(null);
            if (selectedCard == null) {
                selectedCardView = null;
            }
        }
        mergeLog(newState.logLines);
        updateUi();
        maybeShowVictoryScreen();
        maybePromptHandLimitDiscard();
    }

    private void maybeShowVictoryScreen() {
        if (state == null || !state.gameOver || victoryScreenShown) {
            return;
        }
        if (state.winnerName == null || state.winnerName.isBlank()) {
            return;
        }
        victoryScreenShown = true;
        GameVictoryScreen.show(statusMessage, state.winnerName, this::promptRematchAfterVictory);
        if (drawCardBtn != null) {
            drawCardBtn.setDisable(true);
        }
        if (discardCardBtn != null) {
            discardCardBtn.setDisable(true);
        }
        if (endTurnBtn != null) {
            endTurnBtn.setDisable(true);
        }
    }

    private void promptRematchAfterVictory() {
        if (rematchPromptShown || client == null || state == null) {
            return;
        }
        rematchPromptShown = true;
        GameAlertDialogs.askPlayAgain(
                statusMessage,
                "是否和大家再开一局？只有所有玩家都选择再来一局才会重新开始。",
                accept -> {
                    if (client == null || state == null) {
                        return;
                    }
                    client.voteRematch(accept);
                    if (accept) {
                        lastRematchYesCount = state.rematchYesCount;
                        showStatus("已选择再来一局，等待其他玩家…", false);
                    } else {
                        showStatus("你已选择结束本局", false);
                    }
                });
    }

    private void maybePromptHandLimitDiscard() {
        if (!isMyTurn() || state == null) {
            pendingEndTurnAfterDiscard = false;
            return;
        }
        if (myHand.size() <= GameEngine.MAX_HAND_SIZE) {
            if (pendingEndTurnAfterDiscard) {
                pendingEndTurnAfterDiscard = false;
                client.endTurn();
            }
            return;
        }
        if (!mustResolveHandLimitDiscard()) {
            return;
        }
        Platform.runLater(this::promptDiscardForHandLimit);
    }

    private boolean mustResolveHandLimitDiscard() {
        if (!isMyTurn() || state == null || myHand.size() <= GameEngine.MAX_HAND_SIZE) {
            return false;
        }
        return pendingEndTurnAfterDiscard
                || (state.hasDrawnThisTurn && state.remainingPlays <= 0);
    }

    private void promptDiscardForHandLimit() {
        if (!mustResolveHandLimitDiscard()) {
            if (pendingEndTurnAfterDiscard && myHand.size() <= GameEngine.MAX_HAND_SIZE) {
                pendingEndTurnAfterDiscard = false;
                client.endTurn();
            }
            return;
        }
        int excess = myHand.size() - GameEngine.MAX_HAND_SIZE;
        Optional<Card> choice = HandDiscardDialogService.promptDiscardOne(
                helper -> dialogs.showChoiceDialog(
                        helper.title(),
                        helper.header(),
                        helper.prompt(),
                        helper.hand(),
                        Card::getName,
                        card -> null),
                myHand,
                excess,
                pendingEndTurnAfterDiscard || state.remainingPlays <= 0);
        if (choice.isEmpty()) {
            showStatus("You must discard down to " + GameEngine.MAX_HAND_SIZE + " cards before ending your turn", true);
            return;
        }
        client.discardCard(choice.get().getInstanceId());
    }

    private void onPlay() {
        if (!isMyTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (state != null && !state.hasDrawnThisTurn) {
            showStatus("Please click Draw Cards first before playing a card", true);
            return;
        }
        if (state != null && state.remainingPlays <= 0) {
            showStatus("No plays remaining this turn", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("Select a card first", true);
            return;
        }
        playSelectedCard();
    }

    private void mergeLog(List<String> lines) {
        if (lines == null || gameLog == null) return;
        for (int i = mergedLogSize; i < lines.size(); i++) {
            gameLog.appendText(lines.get(i) + "\n");
        }
        mergedLogSize = lines.size();
        gameLog.setScrollTop(Double.MAX_VALUE);
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
            pendingEndTurnAfterDiscard = true;
            promptDiscardForHandLimit();
            return;
        }
        client.endTurn();
    }

    private void onDiscard() {
        if (!isMyTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (state != null && !state.hasDrawnThisTurn) {
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
        sendPlayCard(built.get());
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

    private void sendPlayCard(ClientMessage msg) {
        client.playCard(msg);
        AchievementUi.unlockAndShow(AchievementManager.FIRST_PLAY, statusMessage);
    }

    private boolean isMyTurn() {
        return state != null && !state.gameOver && state.currentPlayerIndex == localSeat;
    }

    private void updateUi() {
        if (state == null) {
            return;
        }
        List<PlayerBoardView> boardViews = PlayerBoardView.fromDtos(state.players, localSeat);
        playerListRenderer.renderBoardViews(playersList, boardViews, state.currentPlayerIndex);
        double rowHeight = publicBoardRenderer.render(
                publicBoardPanel, allPlayersPropertiesPanel, boardViews, state.currentPlayerIndex);
        if (rowHeight > 0) {
            lastPropertyRowHeight = rowHeight;
        }
        bankBarRenderer.renderBoardViews(allPlayersBankBar, boardViews, state.currentPlayerIndex);
        updateHandOnline();
        updateBankOnline();
        updateLabelsOnline();
        updateButtons();
    }

    private void updateHandOnline() {
        boolean canSelect = canSelectHandCards();
        boolean canPlay = canPlayFromHand();
        handRenderer.render(playerHand, playerHandScroll, myHand, selectedCard,
                canSelect, canPlay, handSelectionListener);
        if (selectedCard != null) {
            handRenderer.applySelection(playerHand, selectedCard, (card, view) -> selectedCardView = view);
        }
    }

    private void updateBankOnline() {
        if (playerBank == null) {
            return;
        }
        playerBank.getChildren().clear();
        int total = 0;
        for (PlayerViewDto p : state.players) {
            if (p.seat == localSeat) {
                total = p.bankTotal;
                break;
            }
        }
        if (bankTotalLabel != null) {
            bankTotalLabel.setText(total + "M");
        }
        for (Card card : myBank) {
            StackPane slot = CardView.wrapInSlot(card, false, CardView.COMPACT);
            slot.getStyleClass().add("bank-card-slot");
            playerBank.getChildren().add(slot);
        }
        if (playerBank.getChildren().isEmpty()) {
            Label hint = new Label("(Money cards / action cards played, or rent collected, go into bank)");
            hint.setStyle("-fx-text-fill: #476272; -fx-font-size: 12px; -fx-wrap-text: true;");
            playerBank.getChildren().add(hint);
        }
    }

    private void updateLabelsOnline() {
        if (currentPlayerLabel != null && state.currentPlayerIndex >= 0
                && state.currentPlayerIndex < state.players.size()) {
            String drawStatus = state.hasDrawnThisTurn ? "Drew cards" : "Hasn't drawn";
            currentPlayerLabel.setText("Current Player: " + state.players.get(state.currentPlayerIndex).name
                    + " | " + drawStatus
                    + " | Remaining plays: " + state.remainingPlays + "/" + MAX_PLAYS_PER_TURN
                    + " | Time: " + turnSecondsRemaining + "s");
        }
        if (gameStatusText != null) {
            if (state.gameOver) {
                gameStatusText.setText("Draw pile: " + state.drawPileSize
                        + "  |  Discard pile: " + state.discardPileSize
                        + "  |  Winner: " + state.winnerName);
            } else {
                gameStatusText.setText("Draw pile: " + state.drawPileSize
                        + "  |  Discard pile: " + state.discardPileSize);
            }
        }
    }

    private void selectCard(Card card, CardView cv) {
        if (card.equals(selectedCard)) {
            return;
        }
        for (var node : playerHand.getChildren()) {
            if (node instanceof StackPane sp) {
                CardView view = CardView.getCardView(sp);
                if (view != null) view.setSelected(false);
            }
        }
        selectedCard = card;
        selectedCardView = cv;
        cv.setSelected(true);

        if (card instanceof WildpropertyCard wild) {
            showStatus("Selected wild property [" + card.getName() + "]"
                    + (wild.isBankable() ? " (can deposit to bank for " + wild.getBankValueM() + "M)" : " (cannot deposit to bank)")
                    + ". Play card to choose color or deposit to bank", false);
        } else if (card instanceof RentCard rentCard) {
            showStatus("Selected rent card [" + card.getName() + "] (bank " + rentCard.getBankValueM()
                    + "M). Play card to collect rent or deposit to bank", false);
        } else if (card instanceof DoubleTheRent) {
            showStatus("Selected Double the Rent — pick a Rent card and collect double (uses 2 plays)", false);
        } else if (card instanceof ActionCard actionCard) {
            showStatus("Selected action card [" + card.getName() + "] (bank " + actionCard.getBankValueM()
                    + "M). Play card to choose: use effect or deposit to bank", false);
        } else {
            showStatus("Selected: " + card.getName() + ". Double-click to play, or click Discard Selected Card to discard", false);
        }
        updateButtons();
    }




    private boolean canSelectHandCards() {
        return isMyTurn()
                && state != null
                && state.hasDrawnThisTurn;
    }

    private boolean canPlayFromHand() {
        return canSelectHandCards() && state.remainingPlays > 0;
    }

    private void updateButtons() {
        if (state == null) return;
        if (state.gameOver) {
            if (drawCardBtn != null) drawCardBtn.setDisable(true);
            if (discardCardBtn != null) discardCardBtn.setDisable(true);
            if (endTurnBtn != null) endTurnBtn.setDisable(true);
            return;
        }
        boolean myTurn = isMyTurn();
        if (drawCardBtn != null) drawCardBtn.setDisable(!myTurn || state.hasDrawnThisTurn);
        if (discardCardBtn != null) {
            discardCardBtn.setDisable(!myTurn || !state.hasDrawnThisTurn || selectedCard == null);
        }
        if (endTurnBtn != null) endTurnBtn.setDisable(!myTurn);
    }

    private void showStatus(String text, boolean error) {
        if (error) {
            GameAlertDialogs.showError(statusMessage, text);
            return;
        }
        if (statusMessage == null) {
            return;
        }
        if (text.equals(lastStatusMessage) && error == lastStatusError) {
            return;
        }
        lastStatusMessage = text;
        lastStatusError = error;

        if (statusFadeTransition != null) {
            statusFadeTransition.stop();
        }
        statusMessage.setOpacity(0.4);
        statusMessage.setText(text);
        statusMessage.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: white;");
        statusFadeTransition = new FadeTransition(Duration.millis(160), statusMessage);
        statusFadeTransition.setFromValue(0.4);
        statusFadeTransition.setToValue(1);
        statusFadeTransition.setInterpolator(Interpolator.EASE_OUT);
        statusFadeTransition.play();
    }

    private void loadAvatarImage() {
        try {
            avatarImage = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/ui/avatar.png")));
        } catch (Exception ex) {
            avatarImage = null;
        }
    }
}
