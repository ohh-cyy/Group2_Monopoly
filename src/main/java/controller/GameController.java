package controller;

import controller.dialog.GameDialogService;
import controller.dialog.HandDiscardDialogService;
import controller.gameplay.ActionEffectResolver;
import controller.gameplay.ActionEffectResult;
import controller.gameplay.CardPlayOutcome;
import controller.gameplay.JustSayNoService;
import controller.gameplay.LocalCardPlayService;
import controller.gameplay.PaymentService;
import controller.session.LocalGameSession;
import controller.view.CardSelectionFeedback;
import controller.view.LocalBoardRefreshService;
import engine.GameEngine;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import model.achievement.AchievementManager;
import model.card.*;
import model.player.Player;
import ui.AchievementUi;
import ui.AvatarResources;
import ui.CardView;
import ui.GameAlertDialogs;
import ui.GameVictoryScreen;
import ui.StatusMessageDisplay;
import ui.animation.PlayCardFlyAnimation;
import ui.layout.GameBoardChrome;
import ui.layout.PropertyBoardLayoutTracker;
import ui.render.BankBarRenderer;
import ui.render.HandRenderer;
import ui.render.PlayerBoardView;
import ui.render.PlayerListRenderer;
import ui.render.PublicBoardRenderer;

import java.util.List;
import java.util.Optional;

public class GameController {
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
    @FXML private Button drawCardBtn;
    @FXML private Button discardCardBtn;
    @FXML private Button endTurnBtn;
    @FXML private Button newGameBtn;
    @FXML private Button achievementBtn;

    private LocalGameSession localSession;
    private GameDialogService dialogs;
    private LocalCardPlayService cardPlayService;
    private StatusMessageDisplay statusDisplay;
    private GameBoardChrome boardChrome;
    private PropertyBoardLayoutTracker layoutTracker;
    private LocalBoardRefreshService boardRefresh;
    private PlayCardFlyAnimation playAnimation;

    private GameEngine gameEngine;
    private List<Player> players;
    private Player currentPlayer;
    private Card selectedCard;
    private CardView selectedCardView;
    private CardView pendingPlayedCardView;
    private Image avatarImage;
    private HandRenderer handRenderer;
    private HandRenderer.SelectionListener handSelectionListener;

    @FXML
    public void initialize() {
        avatarImage = AvatarResources.loadDefaultAvatar(getClass());
        statusDisplay = new StatusMessageDisplay(statusMessage);
        boardChrome = new GameBoardChrome(
                leftSidebar, rightSidebar, leftSidebarToggle, rightSidebarToggle,
                leftSidebarHandle, rightSidebarHandle, handDock, handDockHint, handDockToggle);
        boardChrome.setup();
        playAnimation = new PlayCardFlyAnimation();

        initGameplayServices();
        initBoardRefresh();

        layoutTracker = new PropertyBoardLayoutTracker(
                allPlayersPropertiesPanel,
                () -> gameEngine != null ? gameEngine.getPlayers().size() : 0,
                this::updateUI);
        layoutTracker.attach();

        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, oldW, newW) -> {
                if (currentPlayer != null) {
                    updateUI();
                }
            });
        }
        setupButtonActions();
    }

    private void initGameplayServices() {
        localSession = new LocalGameSession();
        dialogs = new GameDialogService(statusMessage);
        PaymentService paymentService = new PaymentService(dialogs, this::logMessage, this::showStatus);
        JustSayNoService justSayNoService = new JustSayNoService(dialogs, this::logMessage, this::showStatus);
        ActionEffectResolver actionResolver = new ActionEffectResolver(
                dialogs, paymentService, justSayNoService, this::logMessage, this::showStatus);
        cardPlayService = new LocalCardPlayService(dialogs, actionResolver, this::logMessage, this::showStatus);
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
                cardView.playActivationAnimation(() -> {
                    if (!getHandCardsForView().contains(card)) {
                        return;
                    }
                    Platform.runLater(GameController.this::playSelectedCard);
                });
            }
        };

        boardRefresh = new LocalBoardRefreshService(
                currentPlayerLabel, gameStatusText, publicBoardPanel, allPlayersPropertiesPanel,
                allPlayersBankBar, playerHandScroll, playerHand, playerBank, bankTotalLabel,
                drawCardBtn, discardCardBtn, endTurnBtn,
                handRenderer, new PlayerListRenderer(() -> avatarImage),
                new PublicBoardRenderer(() -> avatarImage), new BankBarRenderer(),
                () -> gameEngine, () -> players, () -> currentPlayer, this::getHandCardsForView,
                () -> gameEngine != null ? gameEngine.getCurrentPlayerIndex() : 0,
                this::getPublicPlayerViews, handSelectionListener);
        boardRefresh.applySelectionCallback((card, view) -> selectedCardView = view);
    }

    public void startLocalGame() {
        initializeGame();
    }

    private void setupButtonActions() {
        if (drawCardBtn != null) drawCardBtn.setOnAction(e -> onDrawCardsClick());
        if (discardCardBtn != null) discardCardBtn.setOnAction(e -> onDiscardCardClick());
        if (endTurnBtn != null) endTurnBtn.setOnAction(e -> onEndTurnClick());
        if (newGameBtn != null) newGameBtn.setOnAction(e -> onNewGameClick());
        if (achievementBtn != null) {
            achievementBtn.setOnAction(e -> AchievementUi.showLibraryDialog(statusMessage));
        }
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
                            helper.title(), helper.header(), helper.prompt(),
                            helper.hand(), Card::getName, card -> null),
                    player.getHand(), excess, true);
            if (choice.isEmpty()) {
                showStatus("You must discard down to " + GameEngine.MAX_HAND_SIZE
                        + " cards before ending your turn", true);
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
        String action = depositedToBank ? "Already deposited into the bank." : "Already typed.";
        showStatus(action + " This turn can still be played " + gameEngine.getRemainingPlays() + " cards", false);
        afterStateChange();
    }

    private void runAfterPlayAnimation(Card played, boolean depositedToBank, Runnable onFinished) {
        CardView source = pendingPlayedCardView;
        pendingPlayedCardView = null;
        selectedCardView = null;
        playAnimation.play(
                handDock, source, played, depositedToBank,
                playerBank, allPlayersPropertiesPanel, gameStatusText,
                onFinished);
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
                "New Game", "Start a new game?", "This will reset the current game.", start, cancel);
        if (result.isPresent() && result.get() == start) {
            initializeGame();
        }
    }

    private boolean isMyActionTurn() {
        return gameEngine != null && !gameEngine.isGameOver();
    }

    private void updateUI() {
        Platform.runLater(() -> {
            if (gameEngine != null) {
                currentPlayer = gameEngine.getCurrentPlayer();
            }
            boardRefresh.setSelectedCard(selectedCard);
            boardRefresh.refreshAll(playersList, layoutTracker::setLastRowHeight);
        });
    }

    private void selectCard(Card card, CardView cardView) {
        if (card.equals(selectedCard)) {
            return;
        }
        handRenderer.clearSelection(playerHand);
        selectedCard = card;
        selectedCardView = cardView;
        cardView.setSelected(true);
        showStatus(CardSelectionFeedback.messageFor(card), false);
        boardRefresh.setSelectedCard(selectedCard);
        boardRefresh.refreshButtons();
    }

    private void logMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            gameLog.appendText("[" + timestamp + "] " + message + "\n");
            gameLog.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void showStatus(String message, boolean isError) {
        statusDisplay.show(message, isError);
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

    private void unlockAchievement(String achievementId) {
        Platform.runLater(() -> AchievementUi.unlockAndShow(achievementId, statusMessage));
    }

    private void afterStateChange() {
        updateUI();
    }

    private List<Card> getHandCardsForView() {
        return currentPlayer != null ? currentPlayer.getHand() : List.of();
    }

    private List<PlayerBoardView> getPublicPlayerViews() {
        return gameEngine != null ? PlayerBoardView.fromPlayers(players) : List.of();
    }
}
