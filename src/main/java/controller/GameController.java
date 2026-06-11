package controller;

import controller.dialog.GameDialogService;
import controller.dialog.HandDiscardDialogService;
import controller.gameplay.ActionEffectResolver;
import controller.gameplay.ActionEffectResult;
import controller.gameplay.CardPlayOutcome;
import controller.gameplay.JustSayNoService;
import controller.gameplay.LocalCardPlayService;
import controller.gameplay.PaymentService;
import controller.gameplay.WildPropertyRecolorService;
import controller.local.LocalTurnTimer;
import controller.session.LocalGameSession;
import controller.view.CardSelectionFeedback;
import controller.view.GameBoardRefreshService;
import controller.view.LocalBoardRefreshService;
import engine.GameEngine;
import engine.WildPropertyRules;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import model.achievement.AchievementManager;
import model.card.*;
import model.card.actionCard.*;
import model.enums.Color;
import model.player.Player;
import network.protocol.EmojiCatalog;
import ui.AchievementUi;
import ui.AvatarResources;
import ui.CardView;
import ui.EmojiReactionOverlay;
import ui.GameAlertDialogs;
import ui.GameAudio;
import ui.GameLogPane;
import ui.GameVictoryScreen;
import ui.StatusMessageDisplay;
import ui.animation.PlayCardFlyAnimation;
import ui.layout.GameBoardChrome;
import ui.layout.PropertyBoardLayoutTracker;
import ui.render.BankBarRenderer;
import ui.render.HandRenderer;
import ui.render.PlayerBoardView;
import ui.render.PlayerListRenderer;
import ui.render.PublicBoardRenderOptions;
import ui.render.PublicBoardRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * FXML controller for the local hot-seat game view ({@code game-view.fxml}).
 * <p>
 * Wires UI widgets to {@link LocalGameSession}, card-play services, turn timer,
 * and board refresh. All rules run on the client; there is no network layer.
 */
public class GameController {
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
    /** Emoji reaction picker and floating overlay target. */
    @FXML private FlowPane emojiBar;
    @FXML private Pane reactionOverlay;
    /** Primary turn action buttons. */
    @FXML private Button drawCardBtn;
    @FXML private Button discardCardBtn;
    @FXML private Button endTurnBtn;
    @FXML private Button newGameBtn;
    @FXML private Button achievementBtn;

    /** Owns engine lifecycle and delegates turn mutations. */
    private LocalGameSession localSession;
    /** Themed dialogs for card play and hand-limit prompts. */
    private GameDialogService dialogs;
    /** Local card-play orchestration and effect routing. */
    private LocalCardPlayService cardPlayService;
    /** Wild property recolor flow when clicking board cards. */
    private WildPropertyRecolorService wildRecolorService;
    /** Presents transient status and error messages. */
    private StatusMessageDisplay statusDisplay;
    /** Collapsible sidebar and hand-dock chrome. */
    private GameBoardChrome boardChrome;
    /** Tracks property row heights for responsive board layout. */
    private PropertyBoardLayoutTracker layoutTracker;
    /** Mode-specific board refresh implementation. */
    private GameBoardRefreshService boardRefresh;
    /** Per-turn countdown before auto-skipping the current player. */
    private LocalTurnTimer turnTimer;
    /** Fly animation from hand to bank or property board. */
    private PlayCardFlyAnimation playFlyAnimation;
    /** Floating emoji reactions over the public property board. */
    private EmojiReactionOverlay emojiReactionOverlay;
    /** Renders and tracks hand card selection state. */
    private HandRenderer handRenderer;
    /** Routes hand click and double-click to selection/play. */
    private HandRenderer.SelectionListener handSelectionListener;

    /** Live engine reference; {@code null} until {@link #startLocalGame(int)} runs. */
    private GameEngine gameEngine;
    /** Seat list created when a new local game starts. */
    private List<Player> players;
    /** Hand card currently highlighted for discard or play. */
    private Card selectedCard;
    private CardView selectedCardView;
    /** Card view kept during the play-fly animation before removal from hand. */
    private CardView pendingPlayedCardView;
    /** Default avatar image for player list and board renderers. */
    private Image avatarImage;
    /** Player count for the next {@link #initializeGame()} call (2–5). */
    private int localPlayerCount = 4;

    /**
     * 初始化游戏
     */
    @FXML
    public void initialize() {
        avatarImage = AvatarResources.loadDefaultAvatar(getClass());
        statusDisplay = new StatusMessageDisplay(statusMessage);
        dialogs = new GameDialogService(statusMessage);
        playFlyAnimation = new PlayCardFlyAnimation();
        emojiReactionOverlay = new EmojiReactionOverlay(reactionOverlay, allPlayersPropertiesPanel);

        boardChrome = new GameBoardChrome(
                leftSidebar, rightSidebar, leftSidebarToggle, rightSidebarToggle,
                leftSidebarHandle, rightSidebarHandle, handDock, handDockHint, handDockToggle);
        boardChrome.setup();

        initGameplayServices();
        initBoardRefresh();
        initTurnTimer();
        setupButtonActions();
        setupEmojiBar();

        layoutTracker = new PropertyBoardLayoutTracker(
                allPlayersPropertiesPanel,
                () -> players != null ? players.size() : 0,
                this::updateUi);
        layoutTracker.attach();

        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, oldW, newW) -> {
                if (gameEngine != null) {
                    updateUi();
                }
            });
        }
    }

    private void initGameplayServices() {
        localSession = new LocalGameSession();
        PaymentService paymentService = new PaymentService(dialogs, this::logMessage, this::showStatus);
        JustSayNoService justSayNoService = new JustSayNoService(dialogs, this::logMessage, this::showStatus);
        ActionEffectResolver actionResolver = new ActionEffectResolver(
                dialogs, paymentService, justSayNoService, this::logMessage, this::showStatus);
        cardPlayService = new LocalCardPlayService(dialogs, actionResolver, this::logMessage, this::showStatus);
        wildRecolorService = new WildPropertyRecolorService(dialogs, this::logMessage, this::showStatus);
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
    }

    private void initBoardRefresh() {
        boardRefresh = new LocalBoardRefreshService(
                currentPlayerLabel, gameStatusText, turnFlowLabel, playDotsBar, deckPilesBar,
                publicBoardPanel, allPlayersPropertiesPanel,
                allPlayersBankBar, playerHandScroll, playerHand, playerBank, bankTotalLabel,
                drawCardBtn, discardCardBtn, endTurnBtn,
                handRenderer, new PlayerListRenderer(() -> avatarImage),
                new PublicBoardRenderer(() -> avatarImage), new BankBarRenderer(),
                () -> gameEngine, () -> players, () -> currentPlayer(),
                this::getHandCardsForView, this::currentTurnSeat, this::publicPlayerViews,
                handSelectionListener);
        ((LocalBoardRefreshService) boardRefresh).setBoardOptionsSupplier(this::buildWildRecolorOptions);
        ((LocalBoardRefreshService) boardRefresh).setTurnSecondsSupplier(
                () -> turnTimer != null ? turnTimer.getSecondsRemaining() : -1);
        boardRefresh.applySelectionCallback((card, view) -> selectedCardView = view);
    }

    private void initTurnTimer() {
        turnTimer = new LocalTurnTimer(new LocalTurnTimer.Host() {
            @Override
            public GameEngine engine() {
                return gameEngine;
            }

            @Override
            public void onTimerTick(int secondsRemaining) {
                updateUi();
            }

            @Override
            public void onTurnWarning(Player currentPlayer) {
                showStatus(currentPlayer.getName() + " has 10 seconds left to play", false);
                logMessage(currentPlayer.getName() + " has 10 seconds left");
            }

            @Override
            public void onTurnTimedOut(Player skipped) {
                selectedCard = null;
                selectedCardView = null;
                pendingPlayedCardView = null;
                for (Card card : gameEngine.enforceHandSizeLimit(skipped)) {
                    logMessage(skipped.getName() + " auto-discarded " + card.getName() + " (hand size limit)");
                }
                gameEngine.nextTurn();
                GameAudio.play(GameAudio.Cue.TURN);
                Player next = gameEngine.getCurrentPlayer();
                logMessage(skipped.getName() + " ran out of time and was skipped → " + next.getName() + "'s turn");
                showStatus(skipped.getName() + " ran out of time. Skipped to " + next.getName(), false);
                turnTimer.reset();
                afterStateChange();
            }
        });
    }

    /**
     * Starts a local game using the player count from the lobby combo box (default 4).
     */
    public void startLocalGame() {
        startLocalGame(localPlayerCount);
    }

    /**
     * Starts a new local hot-seat game with the given number of players (clamped to 2–5).
     *
     * @param playerCount desired seat count
     */
    public void startLocalGame(int playerCount) {
        localPlayerCount = Math.max(2, Math.min(5, playerCount));
        initializeGame();
    }

    /** Pauses the turn countdown while the pause dialog is open. */
    public void pauseGame() {
        if (turnTimer != null) {
            turnTimer.pause();
        }
    }

    /** Resumes the turn countdown after closing the pause dialog. */
    public void resumeGame() {
        if (turnTimer != null) {
            turnTimer.resume();
        }
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
            button.setOnAction(e -> sendLocalEmoji(emoji));
            emojiBar.getChildren().add(button);
        }
    }

    private void sendLocalEmoji(String emoji) {
        if (gameEngine == null || gameEngine.isGameOver()) {
            return;
        }
        GameAudio.play(GameAudio.Cue.EMOJI);
        emojiReactionOverlay.show(gameEngine.getCurrentPlayerIndex(), emoji);
    }

    private void initializeGame() {
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= localPlayerCount; i++) {
            names.add("Player " + i);
        }
        if (gameLog != null) {
            gameLog.clear();
        }
        localSession.startNewGame(names);
        gameEngine = localSession.getEngine();
        players = localSession.getPlayers();
        selectedCard = null;
        selectedCardView = null;
        pendingPlayedCardView = null;
        logMessage("=== Game started with " + names.size() + " players ===");
        turnTimer.reset();
        updateUi();
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
        GameAudio.play(GameAudio.Cue.DRAW);
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

    private void playSelectedCard() {
        if (!gameEngine.hasDrawnThisTurn()) {
            showStatus("Please click Draw Cards first before playing a card", true);
            return;
        }
        if (!gameEngine.canPlayCard()) {
            GameAlertDialogs.showNoPlaysRemaining(dialogOwner());
            return;
        }
        if (selectedCard == null) {
            showStatus("Please select a card from your hand, then double-click to play it.", true);
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
                updateUi();
                return;
            }
            completePlayStep(player, played, outcome);
        } catch (Exception e) {
            showStatus("Error playing card: " + e.getMessage(), true);
            logMessage("Error playing card: " + e.getMessage());
        }
    }

    private void completePlayStep(Player player, Card played, CardPlayOutcome outcome) {
        localSession.recordCardPlayed();
        if (outcome.consumesExtraPlay) {
            localSession.recordCardPlayed();
        }
        if (boardRefresh instanceof LocalBoardRefreshService localRefresh) {
            localRefresh.refreshTurnStatusOnly();
        }
        playAcceptedCardSound(played, outcome);
        playFlyAnimation.play(
                boardChrome.handDock(),
                pendingPlayedCardView,
                played,
                outcome.depositedToBank,
                playerBank,
                allPlayersPropertiesPanel,
                gameStatusText,
                () -> finishPlayStep(player, played, outcome.depositedToBank));
        pendingPlayedCardView = null;
        selectedCardView = null;
    }

    private void playAcceptedCardSound(Card played, CardPlayOutcome outcome) {
        if (outcome.result == ActionEffectResult.BLOCKED) {
            return;
        }
        if (outcome.depositedToBank || played instanceof MoneyCard) {
            GameAudio.play(GameAudio.Cue.BANK);
        } else if (played instanceof RentCard || played instanceof DoubleTheRent) {
            GameAudio.play(GameAudio.Cue.RENT);
        } else if (played instanceof SlyDeal
                || played instanceof ForcedDeal
                || played instanceof DealBreaker) {
            GameAudio.play(GameAudio.Cue.DRAW);
        } else {
            GameAudio.play(GameAudio.Cue.PLAY);
        }
    }

    private void finishPlayStep(Player player, Card played, boolean depositedToBank) {
        unlockAchievement(AchievementManager.FIRST_PLAY);
        if (localSession.checkWin(player)) {
            localSession.setGameOver(true);
            showGameOver(player);
            return;
        }
        if (gameEngine.isTurnOver()) {
            logMessage(player.getName() + " used all 3 plays this turn");
            showStatus("All 3 plays used. Double-click a card for a reminder, or click End Turn.", false);
            afterStateChange();
            return;
        }
        if (!depositedToBank) {
            showStatus("Card played. You can still play " + gameEngine.getRemainingPlays() + " more cards this turn", false);
        } else {
            showStatus("Already deposited into the bank. This turn can still be played "
                    + gameEngine.getRemainingPlays() + " cards", false);
        }
        afterStateChange();
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
            updateUi();
            return;
        }
        logMessage(player.getName() + " ended turn voluntarily");
        gameEngine.nextTurn();
        GameAudio.play(GameAudio.Cue.TURN);
        turnTimer.reset();
        showStatus("Now " + gameEngine.getCurrentPlayer().getName() + "'s turn, please draw 2 cards first", false);
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

    private void onWildPropertyRecolorRequested(WildpropertyCard wild, int ownerSeat) {
        if (gameEngine == null || ownerSeat != gameEngine.getCurrentPlayerIndex()) {
            return;
        }
        if (!gameEngine.hasDrawnThisTurn()) {
            showStatus("Draw cards before changing wild property color", true);
            return;
        }
        if (!gameEngine.canPlayCard()) {
            GameAlertDialogs.showNoPlaysRemaining(dialogOwner());
            return;
        }

        Player player = gameEngine.getCurrentPlayer();
        WildpropertyCard owned = WildPropertyRules.findOwnedWild(player, wild);
        if (owned == null) {
            showStatus("Wild property not found on your board", true);
            return;
        }
        List<Color> options = WildPropertyRules.getRecolorOptions(player, owned);
        if (options.isEmpty()) {
            showStatus("No alternate colors available (target set may be complete)", true);
            return;
        }

        GameAudio.play(GameAudio.Cue.BUTTON);
        if (!wildRecolorService.attemptRecolor(player, owned)) {
            return;
        }

        localSession.recordCardPlayed();
        GameAudio.play(GameAudio.Cue.PLAY);
        if (localSession.checkWin(player)) {
            localSession.setGameOver(true);
            showGameOver(player);
            return;
        }
        if (gameEngine.isTurnOver()) {
            logMessage(player.getName() + " used all 3 plays this turn");
            showStatus("All 3 plays used. Double-click a card for a reminder, or click End Turn.", false);
            afterStateChange();
            return;
        }
        afterStateChange();
    }

    private void showGameOver(Player winner) {
        turnTimer.stop();
        Platform.runLater(() -> {
            GameVictoryScreen.show(statusMessage, winner.getName(), () ->
                    GameAlertDialogs.askPlayAgain(statusMessage, accept -> {
                        if (accept) {
                            initializeGame();
                        }
                    }));
            gameStatusText.setText("Game Over - " + winner.getName() + " Wins!");
            boardRefresh.disableActionButtons();
        });
    }

    private PublicBoardRenderOptions buildWildRecolorOptions() {
        Player current = currentPlayer();
        if (gameEngine == null || current == null || gameEngine.isGameOver() || !gameEngine.hasDrawnThisTurn()) {
            return PublicBoardRenderOptions.none();
        }
        return new PublicBoardRenderOptions(
                this::onWildPropertyRecolorRequested,
                gameEngine.getCurrentPlayerIndex(),
                true,
                current);
    }

    private void updateUi() {
        Platform.runLater(() -> {
            boardRefresh.setSelectedCard(selectedCard);
            boardRefresh.refreshAll(playersList, layoutTracker::setLastRowHeight);
        });
    }

    private void afterStateChange() {
        updateUi();
    }

    private boolean isMyActionTurn() {
        return gameEngine != null && !gameEngine.isGameOver();
    }

    private Player currentPlayer() {
        return gameEngine != null ? gameEngine.getCurrentPlayer() : null;
    }

    private List<Card> getHandCardsForView() {
        Player current = currentPlayer();
        return current != null ? current.getHand() : List.of();
    }

    private List<PlayerBoardView> publicPlayerViews() {
        return gameEngine != null ? PlayerBoardView.fromPlayers(players) : List.of();
    }

    private int currentTurnSeat() {
        return gameEngine != null ? gameEngine.getCurrentPlayerIndex() : 0;
    }

    private void attemptPlayFromHand(Card card, CardView cardView) {
        if (!validatePlayFromHand()) {
            return;
        }
        cardView.playActivationAnimation(() -> {
            if (getHandCardsForView().contains(card) && validatePlayFromHand()) {
                Platform.runLater(GameController.this::playSelectedCard);
            }
        });
    }

    private boolean validatePlayFromHand() {
        if (gameEngine == null || gameEngine.isGameOver()) {
            return false;
        }
        if (!gameEngine.hasDrawnThisTurn()) {
            showStatus("Please click Draw Cards first before playing a card", true);
            return false;
        }
        if (!gameEngine.canPlayCard()) {
            GameAlertDialogs.showNoPlaysRemaining(dialogOwner());
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

    private void logMessage(String message) {
        if (gameLog != null) {
            gameLog.append(message);
        }
    }

    private void showStatus(String message, boolean isError) {
        statusDisplay.show(message, isError);
    }

    private void unlockAchievement(String achievementId) {
        Platform.runLater(() -> AchievementUi.unlockAndShow(achievementId, statusMessage));
    }
}
