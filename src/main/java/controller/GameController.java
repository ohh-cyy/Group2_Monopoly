package controller;

import controller.dialog.GameDialogService;
import controller.dialog.HandDiscardDialogService;
import controller.gameplay.ActionEffectResolver;
import controller.gameplay.ActionEffectResult;
import controller.gameplay.JustSayNoService;
import controller.gameplay.CardPlayOutcome;
import controller.gameplay.LocalCardPlayService;
import controller.gameplay.PaymentService;
import controller.session.LocalGameSession;
import engine.GameEngine;
import engine.PropertyRules;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
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
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import model.achievement.AchievementManager;
import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.player.Player;

import java.util.*;

public class GameController {
    @FXML
    private Label currentPlayerLabel;
    
    @FXML
    private Label gameStatusText;
    
    @FXML
    private Label statusMessage;
    
    @FXML
    private VBox playersList;

    @FXML
    private VBox leftSidebar;

    @FXML
    private VBox rightSidebar;

    @FXML
    private Button leftSidebarToggle;

    @FXML
    private Button rightSidebarToggle;

    @FXML
    private Button leftSidebarHandle;

    @FXML
    private Button rightSidebarHandle;

    @FXML
    private VBox handDock;

    @FXML
    private Label handDockHint;

    @FXML
    private Button handDockToggle;

    @FXML
    private VBox publicBoardPanel;

    @FXML
    private HBox publicBoardHeader;
    
    @FXML
    private VBox allPlayersPropertiesPanel;

    @FXML
    private HBox allPlayersBankBar;
    
    @FXML
    private ScrollPane playerHandScroll;

    @FXML
    private HBox playerHand;

    @FXML
    private FlowPane playerBank;

    @FXML
    private Label bankTotalLabel;
    
    @FXML
    private TextArea gameLog;
    
    @FXML
    private Button drawCardBtn;
    
    @FXML
    private Button discardCardBtn;
    
    @FXML
    private Button endTurnBtn;
    
    @FXML
    private Button newGameBtn;

    @FXML
    private Button achievementBtn;
    
    private LocalGameSession localSession;
    private GameDialogService dialogs;
    private LocalCardPlayService cardPlayService;
    private HandRenderer handRenderer;
    private PlayerListRenderer playerListRenderer;
    private PublicBoardRenderer publicBoardRenderer;
    private BankBarRenderer bankBarRenderer;

    private GameEngine gameEngine;
    private List<Player> players;
    private Player currentPlayer;
    private Card selectedCard;
    private CardView selectedCardView;
    private CardView pendingPlayedCardView;
    private Random random;

    private static final double HAND_DOCK_HEIGHT = 266;
    private static final double HAND_DOCK_PEEK = 54;
    private boolean handDockExpanded = false;
    private double lastPropertyRowHeight = -1;
    private Image avatarImage;
    private String lastStatusMessage = "";
    private boolean lastStatusError = false;
    private FadeTransition statusFadeTransition;
    private HandRenderer.SelectionListener handSelectionListener;
    
    @FXML
    public void initialize() {
        random = new Random();
        loadAvatarImage();
        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, oldW, newW) -> {
                if (currentPlayer != null) {
                    updatePlayerHand();
                }
            });
        }
        setupButtonActions();
        setupCollapsibleSidebars();
        setupHandDockInteractions();
        setupPublicBoardSizing();
        initGameplayServices();
    }

    private void initGameplayServices() {
        localSession = new LocalGameSession();
        dialogs = new GameDialogService(statusMessage);
        PaymentService paymentService = new PaymentService(dialogs, this::logMessage, this::showStatusForServices);
        JustSayNoService justSayNoService = new JustSayNoService(dialogs, this::logMessage, this::showStatusForServices);
        ActionEffectResolver actionResolver = new ActionEffectResolver(dialogs, paymentService, justSayNoService,
                this::logMessage, this::showStatusForServices);
        cardPlayService = new LocalCardPlayService(dialogs, actionResolver, this::logMessage, this::showStatusForServices);
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
                cardView.playActivationAnimation(() -> {
                    if (!getHandCardsForView().contains(card)) {
                        return;
                    }
                    Platform.runLater(GameController.this::playSelectedCard);
                });
            }
        };
    }

    private void showStatusForServices(String message, boolean isError) {
        showStatus(message, isError);
    }

    public void startLocalGame() {
        initializeGame();
    }
    
    private void setupButtonActions() {
        if (drawCardBtn != null) {
            drawCardBtn.setOnAction(e -> onDrawCardsClick());
        }
        if (discardCardBtn != null) {
            discardCardBtn.setOnAction(e -> onDiscardCardClick());
        }
        if (endTurnBtn != null) {
            endTurnBtn.setOnAction(e -> onEndTurnClick());
        }
        if (newGameBtn != null) {
            newGameBtn.setOnAction(e -> onNewGameClick());
        }
        if (achievementBtn != null) {
            achievementBtn.setOnAction(e -> AchievementUi.showLibraryDialog(statusMessage));
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
        if (newW.doubleValue() > 0 && gameEngine != null) {
            updateAllPlayersProperties();
        }
    });
    attachTableAreaHeightListener();
}

private void attachTableAreaHeightListener() {
    Parent node = allPlayersPropertiesPanel;
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
    if (gameEngine == null) {
        PublicPropertyBoardLayout.applyEqualRows(allPlayersPropertiesPanel);
        return;
    }
    PublicPropertyBoardLayout.applyEqualRows(
            allPlayersPropertiesPanel, gameEngine.getPlayers().size());
    maybeRescalePropertyCards();
}

private void maybeRescalePropertyCards() {
    if (allPlayersPropertiesPanel == null || gameEngine == null) {
        return;
    }
    double rowHeight = PublicPropertyBoardLayout.rowHeightFor(
            allPlayersPropertiesPanel, gameEngine.getPlayers().size());
    if (rowHeight <= 0 || Math.abs(rowHeight - lastPropertyRowHeight) <= 2) {
        return;
    }
    lastPropertyRowHeight = rowHeight;
    updateAllPlayersProperties();
}

    
    private void initializeGame() {
        initializeGameWithPlayers(List.of("Player 1", "Player 2", "Player 3", "Player 4"));
    }

    private void initializeGameWithPlayers(List<String> names) {
        logMessage("=== Starting New Game ===");
        localSession.startNewGame(names);
        gameEngine = localSession.getEngine();
        players = localSession.getPlayers();
        currentPlayer = localSession.getCurrentPlayer();
        selectedCard = null;
        selectedCardView = null;
        pendingPlayedCardView = null;
        logMessage("Players: " + String.join(", ", names));
        updateUI();
    }
    
    @FXML
    private void onDrawCardsClick() {
        if (!isMyActionTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (!gameEngine.canDrawCards()) {
            showStatus("You have already drawn 2 cards this turn", true);
            return;
        }
        Player player = gameEngine.getCurrentPlayer();
        if (!localSession.drawForCurrentPlayer()) {
            showStatus("Fail to draw cards!", true);
            return;
        }
        logMessage(player.getName() + " drew 2 cards");
        unlockAchievement(AchievementManager.FIRST_DRAW);
        showStatus("Two cards have already been drawn (cannot be drawn again in this round). Remaining number of available card games: "
                + gameEngine.getRemainingPlays(), false);
        afterStateChange();
    }
    
    @FXML
    private void onDiscardCardClick() {
        if (!isMyActionTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (!gameEngine.hasDrawnThisTurn()) {
            showStatus("Please click Draw Cards first", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("Please select a card from your hand to discard", true);
            return;
        }
        discardSelectedCard();
    }

    private void discardSelectedCard() {
        Player player = gameEngine.getCurrentPlayer();
        Card card = selectedCard;
        selectedCard = null;
        selectedCardView = null;
        pendingPlayedCardView = null;
        localSession.discardFromHand(player, card);
        logMessage(player.getName() + " discarded " + card.getName());
        showStatus("Discarded " + card.getName(), false);
        afterStateChange();
    }

    private boolean ensureHandSizeWithinLimit(Player player) {
        while (player.getHandSize() > GameEngine.MAX_HAND_SIZE) {
            int excess = player.getHandSize() - GameEngine.MAX_HAND_SIZE;
            Optional<Card> choice = HandDiscardDialogService.promptDiscardOne(
                    helper -> dialogs.showChoiceDialog(
                            helper.title(),
                            helper.header(),
                            helper.prompt(),
                            helper.hand(),
                            Card::getName,
                            card -> null),
                    player.getHand(),
                    excess,
                    true);
            if (choice.isEmpty()) {
                showStatus("You must discard down to " + GameEngine.MAX_HAND_SIZE + " cards before ending your turn", true);
                return false;
            }
            gameEngine.discardFromHand(player, choice.get());
            logMessage(player.getName() + " discarded " + choice.get().getName() + " (hand size limit)");
        }
        return true;
    }
    
    private void playSelectedCard() {
        if (!gameEngine.hasDrawnThisTurn()) {
            showStatus("Please click Draw Cards first before playing a card", true);
            return;
        }
        if (!gameEngine.canPlayCard()) {
            showStatus("Three cards have been played in this round, no more cards can be played!", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("Please first click on the hand to select a card, then click on「Play」", true);
            return;
        }

        try {
            Player player = gameEngine.getCurrentPlayer();
            Card played = selectedCard;
            pendingPlayedCardView = selectedCardView;
            selectedCard = null;
            CardPlayOutcome outcome = cardPlayService.play(localSession, player, played);
            if (outcome.result == ActionEffectResult.CANCELLED || outcome.result == ActionEffectResult.FAILED) {
                selectedCard = played;
                pendingPlayedCardView = null;
                updateUI();
                return;
            }
            completePlayStep(player, played, outcome.depositedToBank, outcome.consumesExtraPlay);
        } catch (Exception e) {
            showStatus("Error playing card: " + e.getMessage(), true);
            logMessage("Error playing card: " + e.getMessage());
        }
    }

    private void completePlayStep(Player player, Card played, boolean depositedToBank, boolean extraPlay) {
        localSession.recordCardPlayed();
        if (extraPlay) {
            localSession.recordCardPlayed();
        }
        runAfterPlayAnimation(played, depositedToBank, () -> finishPlayStep(player, played, depositedToBank));
    }

    private void finishPlayStep(Player player, Card played, boolean depositedToBank) {
        unlockAchievement(AchievementManager.FIRST_PLAY);
        if (localSession.checkWin(player)) {
            localSession.setGameOver(true);
            showGameOver(player);
            return;
        }

        if (gameEngine.isTurnOver()) {
            logMessage(player.getName() + " Three cards have been played in this turn, turn end");
            forceEndTurn();
            return;
        }

        if (!depositedToBank) {
            showStatus("Already typed. This turn can still be played " + gameEngine.getRemainingPlays() + " cards", false);
        } else {
            showStatus("Already deposited into the bank. This turn can still be played " + gameEngine.getRemainingPlays() + " cards", false);
        }
        afterStateChange();
    }

    private void runAfterPlayAnimation(Card played, boolean depositedToBank, Runnable onFinished) {
        CardView source = pendingPlayedCardView;
        pendingPlayedCardView = null;
        selectedCardView = null;

        if (source == null || source.getScene() == null || handDock == null) {
            onFinished.run();
            return;
        }

        Parent parent = handDock.getParent();
        if (!(parent instanceof Pane overlay)) {
            onFinished.run();
            return;
        }

        Node target = targetNodeForPlayedCard(played, depositedToBank);
        if (target == null || target.getScene() == null) {
            onFinished.run();
            return;
        }

        Bounds sourceBounds = source.localToScene(source.getBoundsInLocal());
        Bounds targetBounds = target.localToScene(target.getBoundsInLocal());
        Point2D start = overlay.sceneToLocal(sourceBounds.getMinX(), sourceBounds.getMinY());
        Point2D targetPoint = overlay.sceneToLocal(
                targetBounds.getMinX() + targetBounds.getWidth() / 2,
                targetBounds.getMinY() + targetBounds.getHeight() / 2
        );

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        WritableImage snapshot = source.snapshot(params, null);
        ImageView ghost = new ImageView(snapshot);
        ghost.setFitWidth(sourceBounds.getWidth());
        ghost.setFitHeight(sourceBounds.getHeight());
        ghost.setPreserveRatio(false);
        ghost.setMouseTransparent(true);
        ghost.setManaged(false);
        ghost.setLayoutX(start.getX());
        ghost.setLayoutY(start.getY());

        source.setVisible(false);
        overlay.getChildren().add(ghost);
        ghost.toFront();

        double targetX = targetPoint.getX() - start.getX() - sourceBounds.getWidth() / 2;
        double targetY = targetPoint.getY() - start.getY() - sourceBounds.getHeight() / 2;

        TranslateTransition move = new TranslateTransition(Duration.millis(420), ghost);
        move.setByX(targetX);
        move.setByY(targetY);
        move.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition scale = new ScaleTransition(Duration.millis(420), ghost);
        scale.setToX(depositedToBank ? 0.45 : 0.65);
        scale.setToY(depositedToBank ? 0.45 : 0.65);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        RotateTransition rotate = new RotateTransition(Duration.millis(420), ghost);
        rotate.setByAngle(depositedToBank ? -10 : 12);
        rotate.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fade = new FadeTransition(Duration.millis(420), ghost);
        fade.setFromValue(0.98);
        fade.setToValue(0.12);

        ParallelTransition fly = new ParallelTransition(move, scale, rotate, fade);
        fly.setOnFinished(e -> {
            overlay.getChildren().remove(ghost);
            onFinished.run();
        });
        fly.play();
    }

    private Node targetNodeForPlayedCard(Card played, boolean depositedToBank) {
        if (depositedToBank) {
            return playerBank != null ? playerBank : bankTotalLabel;
        }
        if (played instanceof PropertyCard) {
            return allPlayersPropertiesPanel != null ? allPlayersPropertiesPanel : gameStatusText;
        }
        return gameStatusText != null ? gameStatusText : allPlayersPropertiesPanel;
    }

    
    private void loadAvatarImage() {
        try {
            avatarImage = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/ui/avatar.png")));
        } catch (Exception ex) {
            avatarImage = null;
        }
    }

    private void unlockAchievement(String achievementId) {
        Platform.runLater(() -> AchievementUi.unlockAndShow(achievementId, statusMessage));
    }

private void forceEndTurn() {
        Platform.runLater(() -> {
            Player ending = gameEngine.getCurrentPlayer();
            if (!ensureHandSizeWithinLimit(ending)) {
                updateUI();
                return;
            }
            gameEngine.nextTurn();
            currentPlayer = gameEngine.getCurrentPlayer();
            logMessage(ending.getName() + " turn ends → " + currentPlayer.getName() + "'s turn");
            showStatus("Played 3 cards, automatically switching to " + currentPlayer.getName()
                    + ". Please draw 2 cards before playing", false);
            afterStateChange();
        });
    }
    
    @FXML
    private void onEndTurnClick() {
        if (!isMyActionTurn()) {
            showStatus("Not your turn yet", true);
            return;
        }
        endCurrentTurn();
    }
    
    private void endCurrentTurn() {
        Player player = gameEngine.getCurrentPlayer();
        if (!ensureHandSizeWithinLimit(player)) {
            updateUI();
            return;
        }
        logMessage(player.getName() + " ended turn voluntarily");
        gameEngine.nextTurn();
        currentPlayer = gameEngine.getCurrentPlayer();
        showStatus("Now " + currentPlayer.getName() + "'s turn, please draw 2 cards first", false);
        afterStateChange();
    }
    private void onNewGameClick() {
        ButtonType start = new ButtonType("Start New Game");
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> result = dialogs.showButtonDialog(
                "New Game",
                "Start a new game?",
                "This will reset the current game.",
                start, cancel);
        if (result.isPresent() && result.get() == start) {
            initializeGame();
        }
    }
    
    private boolean isMyActionTurn() {
        return gameEngine != null && !gameEngine.isGameOver();
    }

    private boolean isCurrentPlayerTurn() {
        return isMyActionTurn();
    }
    
    private void updateUI() {
        Platform.runLater(() -> {
            if (gameEngine != null) {
                currentPlayer = gameEngine.getCurrentPlayer();
            }
            updatePlayerInfo();
            updateCurrentPlayerDisplay();
            updateAllPlayersProperties();
            updateAllPlayersBankBar();
            updatePlayerHand();
            updatePlayerBank();
            updatePileCounts();
            updateButtonStates();
        });
    }

    private void updatePlayerInfo() {
        playerListRenderer.render(playersList, players,
                gameEngine != null ? gameEngine.getCurrentPlayer() : null);
    }

    private void updateCurrentPlayerDisplay() {
        if (currentPlayerLabel == null || currentPlayer == null || gameEngine == null) {
            return;
        }
        String drawStatus = gameEngine.hasDrawnThisTurn() ? "Drew cards" : "Hasn't drawn";
        currentPlayerLabel.setText("Current Player: " + currentPlayer.getName()
                + " | " + drawStatus
                + " | Remaining plays: " + gameEngine.getRemainingPlays() + "/"
                + GameEngine.MAX_PLAYS_PER_TURN);
    }
    
    private void updatePlayerHand() {
        if (currentPlayer == null) {
            if (playerHand != null) {
                playerHand.getChildren().clear();
            }
            return;
        }

        boolean canSelect = isMyActionTurn() && gameEngine.hasDrawnThisTurn();
        boolean canPlay = canSelect && canPlayFromView();
        handRenderer.render(playerHand, playerHandScroll, getHandCardsForView(), selectedCard,
                canSelect, canPlay, handSelectionListener);
        if (selectedCard != null) {
            handRenderer.applySelection(playerHand, selectedCard, (card, view) -> selectedCardView = view);
        }
    }

    private void selectCard(Card card, CardView cardView) {
        if (card.equals(selectedCard)) {
            return;
        }
        handRenderer.clearSelection(playerHand);

        selectedCard = card;
        selectedCardView = cardView;
        cardView.setSelected(true);

        if (card instanceof WildpropertyCard wild) {
            showStatus("Selected wild property [" + card.getName() + "]"
                    + (wild.isBankable() ? " (can deposit to bank for " + wild.getBankValueM() + "M)" : " (cannot deposit to bank)")
                    + ". Play card to choose color or deposit to bank", false);
        } else if (card instanceof RentCard rentCard) {
            showStatus("Selected rent card [" + card.getName() + "] (bank " + rentCard.getBankValueM()
                    + "M). Play card to collect rent or deposit to bank", false);
        } else if (card instanceof ActionCard actionCard) {
            showStatus("Selected action card [" + card.getName() + "] (bank " + actionCard.getBankValueM()
                    + "M). Play card to choose: use effect or deposit to bank", false);
        } else {
            showStatus("Selected: " + card.getName() + ". Double-click to play, or click Discard Selected Card to discard", false);
        }
        updateButtonStates();
    }
    
    private void updatePlayerBank() {
        if (playerBank == null || currentPlayer == null) {
            return;
        }
        playerBank.getChildren().clear();
        if (bankTotalLabel != null) {
            bankTotalLabel.setText(currentPlayer.getBankTotalValue() + "M");
        }
        for (Card card : currentPlayer.getBank()) {
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

    private void updatePlayerProperties() {
        updateAllPlayersProperties();
    }

    private void updateAllPlayersProperties() {
        double rowHeight = publicBoardRenderer.render(
                publicBoardPanel, allPlayersPropertiesPanel, getPublicPlayerViews(), getCurrentTurnSeat());
        if (rowHeight > 0) {
            lastPropertyRowHeight = rowHeight;
        }
    }

    private void updateAllPlayersBankBar() {
        if (gameEngine == null) {
            return;
        }
        bankBarRenderer.render(allPlayersBankBar, gameEngine.getPlayers(), getCurrentTurnSeat());
    }

    private void updatePileCounts() {
        if (gameStatusText == null || gameEngine == null || gameEngine.isGameOver()) {
            return;
        }
        gameStatusText.setText("Draw pile: " + gameEngine.getDeck().size()
                + "  |  Discard pile: " + gameEngine.getDiscardPile().size());
    }

    private void updateButtonStates() {
        if (gameEngine == null) {
            return;
        }
        boolean canDraw = isMyActionTurn() && gameEngine.canDrawCards();
        drawCardBtn.setDisable(!canDraw);
        discardCardBtn.setDisable(!isMyActionTurn() || !gameEngine.hasDrawnThisTurn() || selectedCard == null);
        endTurnBtn.setDisable(!isMyActionTurn());
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String line = "[" + timestamp + "] " + message;
            gameLog.appendText(line + "\n");
            gameLog.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void showStatus(String message, boolean isError) {
        if (isError) {
            GameAlertDialogs.showError(statusMessage, message);
            return;
        }
        Platform.runLater(() -> {
            if (message.equals(lastStatusMessage) && isError == lastStatusError) {
                return;
            }
            lastStatusMessage = message;
            lastStatusError = isError;

            if (statusFadeTransition != null) {
                statusFadeTransition.stop();
            }
            statusMessage.setOpacity(0.4);
            statusMessage.setText(message);
            if (isError) {
                statusMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
            } else {
                statusMessage.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            }
            statusFadeTransition = new FadeTransition(Duration.millis(160), statusMessage);
            statusFadeTransition.setFromValue(0.4);
            statusFadeTransition.setToValue(1);
            statusFadeTransition.setInterpolator(Interpolator.EASE_OUT);
            statusFadeTransition.play();
        });
    }
    
        private void showGameOver(Player winner) {
        Platform.runLater(() -> {
            GameVictoryScreen.show(statusMessage, winner.getName(), () ->
                    GameAlertDialogs.askPlayAgain(statusMessage, accept -> {
                        if (accept) {
                            initializeGame();
                        }
                    }));
            gameStatusText.setText("Game Over - " + winner.getName() + " Wins!");
            drawCardBtn.setDisable(true);
            discardCardBtn.setDisable(true);
            endTurnBtn.setDisable(true);
        });
    }

    private void afterStateChange() {
        updateUI();
    }

    private List<Card> getHandCardsForView() {
        return currentPlayer != null ? currentPlayer.getHand() : List.of();
    }

    private boolean canPlayFromView() {
        return gameEngine != null
                && gameEngine.hasDrawnThisTurn()
                && gameEngine.canPlayCard();
    }

    private List<PlayerBoardView> getPublicPlayerViews() {
        return gameEngine != null ? PlayerBoardView.fromPlayers(players) : List.of();
    }

    private int getCurrentTurnSeat() {
        return gameEngine != null ? gameEngine.getCurrentPlayerIndex() : 0;
    }
}
