package controller;

import engine.Deck;
import engine.DeckFactory;
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
import controller.dialog.HandDiscardDialogService;
import ui.AchievementUi;
import ui.CardView;
import ui.PublicPropertyBoardLayout;
import ui.PublicPropertySetView;
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
import javafx.scene.input.MouseButton;
import javafx.scene.shape.Circle;
import model.achievement.AchievementManager;
import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

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
    
    private GameEngine gameEngine;
    private Deck deck;
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

private double computePropertyRowHeight(int playerCount) {
    return PublicPropertyBoardLayout.rowHeightFor(allPlayersPropertiesPanel, playerCount);
}
    
    private void initializeGame() {
        initializeGameWithPlayers(List.of("Player 1", "Player 2", "Player 3", "Player 4"));
    }

    private void initializeGameWithPlayers(List<String> names) {
        logMessage("=== Starting New Game ===");
        players = new ArrayList<>();
        for (String name : names) {
            players.add(new Player(name));
        }
        List<Card> cardList = DeckFactory.createFullDeck();
        deck = new Deck(cardList);
        gameEngine = new GameEngine(players, deck);
        gameEngine.startGame();
        currentPlayer = gameEngine.getCurrentPlayer();
        logMessage("Players: " + String.join(", ", names));
        updateUI();
    }
    
    @FXML
    private void onDrawCardsClick() {
        if (!isMyActionTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        drawCardsFromPile();
    }
    
    private void drawCardsFromPile() {
        if (!gameEngine.canDrawCards()) {
            showStatus("You have already drawn 2 cards this turn", true);
            return;
        }

        Player player = gameEngine.getCurrentPlayer();
        if (!gameEngine.drawCardsForCurrentPlayer()) {
            showStatus("Fail to draw cards!", true);
            return;
        }

        logMessage(player.getName() + "drew 2 cards");
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
            showStatus("请先点击 Draw Cards 抽牌", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("请先在手牌中选择一张要丢弃的卡牌", true);
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
        gameEngine.discardFromHand(player, card);
        logMessage(player.getName() + " 丢弃 " + card.getName());
        showStatus("已丢弃 " + card.getName(), false);
        afterStateChange();
    }

    private boolean ensureHandSizeWithinLimit(Player player) {
        while (player.getHandSize() > GameEngine.MAX_HAND_SIZE) {
            int excess = player.getHandSize() - GameEngine.MAX_HAND_SIZE;
            Optional<Card> choice = HandDiscardDialogService.promptDiscardOne(
                    helper -> showStyledChoiceDialog(
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
                showStatus("结束回合前必须将手牌弃至 " + GameEngine.MAX_HAND_SIZE + " 张以内", true);
                return false;
            }
            gameEngine.discardFromHand(player, choice.get());
            logMessage(player.getName() + " 丢弃 " + choice.get().getName() + "（手牌上限）");
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

            if (played instanceof ActionCard actionCard) {
                playActionCard(player, actionCard);
                return;
            }

            if (played instanceof WildpropertyCard wildCard) {
                playWildPropertyCard(player, wildCard);
                return;
            }

            logMessage(player.getName() + " played: " + played.getName());
            played.use(player, gameEngine);
            player.removeFromHand(played);
            completePlayStep(player, played, false);
        } catch (Exception e) {
            showStatus("Error playing card: " + e.getMessage(), true);
            logMessage("Error playing card: " + e.getMessage());
        }
    }

    private void playActionCard(Player player, ActionCard actionCard) {
        Optional<ActionPlayChoice> choice = promptActionCardChoice(actionCard);
        if (choice.isEmpty()) {
            selectedCard = actionCard;
            pendingPlayedCardView = null;
            return;
        }

        if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
            player.removeFromHand(actionCard);
            actionCard.depositToBank(player);
            logMessage(player.getName() + " deposit「" + actionCard.getName()
                    + "」into the bank（" + actionCard.getBankValueM() + "M）");
            showStatus("Already deposited in the bank " + actionCard.getBankValueM() + "M", false);
            completePlayStep(player, actionCard, true);
            return;
        }

        ActionEffectResult result = resolveActionCardEffect(player, actionCard);
        if (result == ActionEffectResult.CANCELLED) {
            selectedCard = actionCard;
            pendingPlayedCardView = null;
            showStatus("The card has been cancelled, the action card is kept in hand", false);
            updateUI();
            return;
        }

        player.removeFromHand(actionCard);
        gameEngine.getDiscardPile().addCard(actionCard);
        if (result == ActionEffectResult.SUCCESS) {
            logMessage(player.getName() + " use「" + actionCard.getName() + "」effect");
            showStatus("Effect has been successfully used: " + actionCard.getName(), false);
        } else {
            logMessage(player.getName() + " Fail to use「" + actionCard.getName() + "」,the cards enter the discard pile");
            showStatus("The effect did not take effect (invalid target, etc.)", true);
        }
        completePlayStep(player, actionCard, false);
        if (actionCard instanceof DoubleTheRent && result == ActionEffectResult.SUCCESS) {
            gameEngine.recordCardPlayed();
        }
    }

    private void completePlayStep(Player player, Card played, boolean depositedToBank) {
        gameEngine.recordCardPlayed();

        runAfterPlayAnimation(played, depositedToBank, () -> finishPlayStep(player, played, depositedToBank));
    }

    private void finishPlayStep(Player player, Card played, boolean depositedToBank) {
        unlockAchievement(AchievementManager.FIRST_PLAY);
        if (gameEngine.checkWin(player)) {
            gameEngine.setGameOver(true);
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

    private enum ActionPlayChoice {
        USE_EFFECT, DEPOSIT_BANK
    }

    private enum ActionEffectResult {
        SUCCESS, FAILED, CANCELLED
    }

        private Optional<ActionPlayChoice> promptActionCardChoice(ActionCard card) {
        ButtonType useBtn = new ButtonType("Use Effect");
        ButtonType bankBtn = new ButtonType("Deposit to Bank (" + card.getBankValueM() + "M)");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> result = showStyledButtonDialog(
                "Action Card",
                card.getName() + " — Bank value " + card.getBankValueM() + "M",
                card.getDescription() + "\n\nChoose 'Use Effect' or 'Deposit to Bank'?",
                useBtn, bankBtn, cancelBtn);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        if (result.get() == useBtn) {
            return Optional.of(ActionPlayChoice.USE_EFFECT);
        }
        if (result.get() == bankBtn) {
            return Optional.of(ActionPlayChoice.DEPOSIT_BANK);
        }
        return Optional.empty();
    }

    private ActionEffectResult resolveActionCardEffect(Player player, ActionCard actionCard) {
        if (actionCard instanceof JustSayNo) {
            showStatus("Just Say No can only be used when opponent plays an action card against you", true);
            return ActionEffectResult.CANCELLED;
        }
        if (actionCard instanceof DealBreaker dealBreaker) {
            return resolveDealBreaker(player, dealBreaker);
        }
        if (actionCard instanceof DebtCollector debtCollector) {
            return resolveDebtCollector(player, debtCollector);
        }
        if (actionCard instanceof MyBirthday myBirthday) {
            return resolveMyBirthday(player, myBirthday);
        }
        if (actionCard instanceof DoubleTheRent doubleRent) {
            return resolveDoubleTheRent(player, doubleRent);
        }
        if (actionCard instanceof SlyDeal slyDeal) {
            return resolveSlyDeal(player, slyDeal);
        }
        if (actionCard instanceof ForcedDeal forcedDeal) {
            return resolveForcedDeal(player, forcedDeal);
        }
        if (actionCard instanceof House house) {
            Optional<Color> color = promptSelectOwnCompleteSet(player, "Select a complete set to add House");
            if (color.isEmpty()) {
                return hasAnyCompleteSet(player)
                        ? ActionEffectResult.CANCELLED
                        : ActionEffectResult.FAILED;
            }
            return house.addHouseToSet(player, color.get())
                    ? ActionEffectResult.SUCCESS
                    : ActionEffectResult.FAILED;
        }
        if (actionCard instanceof Hotel hotel) {
            Optional<Color> color = promptSelectOwnCompleteSet(player, "Select a complete set to add Hotel");
            if (color.isEmpty()) {
                return hasAnyCompleteSet(player)
                        ? ActionEffectResult.CANCELLED
                        : ActionEffectResult.FAILED;
            }
            return hotel.addHotelToSet(player, color.get())
                    ? ActionEffectResult.SUCCESS
                    : ActionEffectResult.FAILED;
        }
        if (actionCard instanceof RentCard rentCard) {
            return resolveRentCard(player, rentCard);
        }

        actionCard.use(player, gameEngine);
        return ActionEffectResult.SUCCESS;
    }

    private ActionEffectResult resolveRentCard(Player player, RentCard rentCard) {
        if (!rentCard.canPlay(player)) {
            showStatus("You don't have properties of applicable colors, cannot use this rent card", true);
            return ActionEffectResult.FAILED;
        }

        List<Color> options = rentCard.getChargeableColors(player);
        Color chargeColor;
        if (rentCard.isAllColors() && options.isEmpty()) {
            Optional<Color> picked = promptSelectRentColor(player, rentCard, Arrays.asList(Color.values()));
            if (picked.isEmpty()) {
                return ActionEffectResult.CANCELLED;
            }
            chargeColor = picked.get();
        } else if (options.size() == 1) {
            chargeColor = options.get(0);
        } else {
            Optional<Color> picked = promptSelectRentColor(player, rentCard, options);
            if (picked.isEmpty()) {
                return ActionEffectResult.CANCELLED;
            }
            chargeColor = picked.get();
        }

        int rent = rentCard.calculateRent(player, chargeColor);
        if (rent <= 0) {
            showStatus("No properties of this color, rent is 0", true);
            return ActionEffectResult.FAILED;
        }

        boolean doubled = gameEngine.isRentDoubled();
        int total = rentCard.collectFromAll(player, gameEngine, chargeColor, rent);

        String rentNote = doubled ? "(double rent)" : "";
        logMessage(player.getName() + " used [" + rentCard.getName() + "] to collect " + chargeColor
                + " rent " + (doubled ? rent * 2 : rent) + "M/player" + rentNote + ", total " + total + "M");
        showStatus("Collected " + chargeColor + " rent" + rentNote + " from all players (total " + total + "M)", false);
        return ActionEffectResult.SUCCESS;
    }

    private ActionEffectResult resolveDebtCollector(Player player, DebtCollector debtCollector) {
        Optional<Player> target = promptSelectOpponent(player, "Debt Collector: Select player to collect 5M from");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (tryRespondWithJustSayNo(target.get(), player, "Debt Collector (collect 5M)")) {
            return ActionEffectResult.CANCELLED;
        }
        int paid = debtCollector.collectFrom(player, target.get());
        logMessage(player.getName() + " received " + paid + "M from " + target.get().getName());
        return paid > 0 ? ActionEffectResult.SUCCESS : ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveMyBirthday(Player player, MyBirthday myBirthday) {
        int total = myBirthday.collectFromEveryone(player, gameEngine);
        logMessage(player.getName() + " celebrates birthday, received " + total + "M total (each pays "
                + MyBirthday.GIFT_AMOUNT + "M, can use properties if insufficient)");
        showStatus("All players paid " + total + "M total", false);
        return total > 0 ? ActionEffectResult.SUCCESS : ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveDoubleTheRent(Player player, DoubleTheRent doubleRent) {
        if (gameEngine.getRemainingPlays() < 2) {
            showStatus("You need 2 plays remaining to use Double the Rent with a Rent card", true);
            return ActionEffectResult.FAILED;
        }

        List<RentCard> rentOptions = findPlayableRentCards(player, doubleRent);
        if (rentOptions.isEmpty()) {
            showStatus("No playable Rent card in your hand", true);
            return ActionEffectResult.FAILED;
        }

        Optional<RentCard> rentChoice = promptSelectRentCard(rentOptions);
        if (rentChoice.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        RentCard rentCard = rentChoice.get();

        List<Color> colorOptions = rentCard.getChargeableColors(player);
        Color chargeColor;
        if (rentCard.isAllColors() && colorOptions.isEmpty()) {
            Optional<Color> picked = promptSelectRentColor(player, rentCard, Arrays.asList(Color.values()));
            if (picked.isEmpty()) {
                return ActionEffectResult.CANCELLED;
            }
            chargeColor = picked.get();
        } else if (colorOptions.size() == 1) {
            chargeColor = colorOptions.get(0);
        } else {
            Optional<Color> picked = promptSelectRentColor(player, rentCard, colorOptions);
            if (picked.isEmpty()) {
                return ActionEffectResult.CANCELLED;
            }
            chargeColor = picked.get();
        }

        int rent = rentCard.calculateRent(player, chargeColor);
        if (rent <= 0) {
            showStatus("No properties of this color, rent is 0", true);
            return ActionEffectResult.FAILED;
        }

        int rentPerPlayer = rent * 2;
        int total = rentCard.collectFromAll(player, gameEngine, chargeColor, rentPerPlayer);
        player.removeFromHand(rentCard);
        gameEngine.getDiscardPile().addCard(rentCard);

        logMessage(player.getName() + " used Double the Rent with [" + rentCard.getName() + "] to collect "
                + chargeColor + " rent " + rentPerPlayer + "M/player (double rent), total " + total + "M");
        showStatus("Double rent collected " + chargeColor + " from all players (total " + total + "M)", false);
        return ActionEffectResult.SUCCESS;
    }

    private List<RentCard> findPlayableRentCards(Player player, ActionCard excluding) {
        List<RentCard> options = new ArrayList<>();
        for (Card card : player.getHand()) {
            if (card == excluding || !(card instanceof RentCard rent)) {
                continue;
            }
            if (rent.canPlay(player)) {
                options.add(rent);
            }
        }
        return options;
    }

    private Optional<RentCard> promptSelectRentCard(List<RentCard> rentCards) {
        return showStyledChoiceDialog(
                "Choose Rent Card",
                "Double the Rent",
                "Select a Rent card to play at double value (uses 2 plays):",
                rentCards,
                rent -> rent.getName() + " (bank " + rent.getBankValueM() + "M)",
                rent -> null);
    }

    private ActionEffectResult resolveSlyDeal(Player player, SlyDeal slyDeal) {
        Optional<Player> target = promptSelectOpponent(player, "Sly Deal: Select player to steal property from");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (tryRespondWithJustSayNo(target.get(), player, "Sly Deal (steal one property)")) {
            return ActionEffectResult.CANCELLED;
        }
        List<PropertyCard> stealable = PropertyRules.getPropertiesOutsideCompleteSets(target.get());
        if (stealable.isEmpty()) {
            showStatus(target.get().getName() + " has no stealable properties (complete sets cannot be stolen)", true);
            return ActionEffectResult.FAILED;
        }
        Optional<PropertyCard> property = promptSelectProperty(stealable,
                "Select property to steal", target.get().getName() + "'s stealable properties (not in complete sets)");
        if (property.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (slyDeal.stealProperty(player, target.get(), property.get())) {
            logMessage(player.getName() + " stole " + property.get().getName()
                    + " from " + target.get().getName());
            return ActionEffectResult.SUCCESS;
        }
        return ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveForcedDeal(Player player, ForcedDeal forcedDeal) {
        Optional<Player> target = promptSelectOpponent(player, "Forced Deal: Select player to exchange properties with");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (tryRespondWithJustSayNo(target.get(), player, "Forced Deal (exchange properties)")) {
            return ActionEffectResult.CANCELLED;
        }
        List<PropertyCard> myProps = player.getAllProperties();
        if (myProps.isEmpty()) {
            showStatus("You have no properties to exchange", true);
            return ActionEffectResult.FAILED;
        }
        List<PropertyCard> theirSwappable = PropertyRules.getPropertiesOutsideCompleteSets(target.get());
        if (theirSwappable.isEmpty()) {
            showStatus(target.get().getName() + " has no exchangeable properties (complete sets cannot be exchanged)", true);
            return ActionEffectResult.FAILED;
        }
        Optional<PropertyCard> mine = promptSelectProperty(myProps,
                "Select your property to exchange", "Your properties");
        if (mine.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        Optional<PropertyCard> theirs = promptSelectProperty(theirSwappable,
                "Select opponent's property to exchange", target.get().getName() + "'s exchangeable properties (cannot be from complete sets)");
        if (theirs.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (forcedDeal.swapProperties(player, mine.get(), target.get(), theirs.get())) {
            logMessage(player.getName() + " exchanged properties with " + target.get().getName() + ": "
                    + mine.get().getName() + " ↔ " + theirs.get().getName());
            return ActionEffectResult.SUCCESS;
        }
        return ActionEffectResult.FAILED;
    }

    /**
     * If the defending hand has a Just Say No and chooses to play, the effect of this action will be cancelled.
     */
        private boolean tryRespondWithJustSayNo(Player defender, Player attacker, String actionName) {
        JustSayNo justSayNo = findJustSayNoInHand(defender);
        if (justSayNo == null) {
            return false;
        }
        ButtonType noBtn = new ButtonType("Reject (Just Say No)");
        ButtonType allowBtn = new ButtonType("Allow");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> choice = showStyledButtonDialog(
                "Just Say No",
                defender.getName() + ": Do you want to reject " + attacker.getName() + "'s action?",
                actionName + "\n\nSelect 'Reject' to play Just Say No and cancel this effect.",
                noBtn, allowBtn, cancelBtn);
        if (choice.isEmpty() || choice.get() == cancelBtn || choice.get() == allowBtn) {
            return false;
        }

        String justSayNoId = justSayNo.getInstanceId();
        if (!defender.removeFromHandById(justSayNoId)) {
            return false;
        }
        gameEngine.getDiscardPile().addCard(justSayNo);
        logMessage(defender.getName() + " played Just Say No, cancelling " + attacker.getName() + "'s [" + actionName + "]");
        showStatus(defender.getName() + " used Just Say No to reject this action", false);
        return true;
    }

    private JustSayNo findJustSayNoInHand(Player player) {
        for (Card card : player.getHand()) {
            if (card instanceof JustSayNo js) {
                return js;
            }
        }
        return null;
    }

        private Optional<PropertyCard> promptSelectProperty(List<PropertyCard> properties,
                                                          String title, String header) {
        return showStyledChoiceDialog(title, header, "Select a property:", properties,
                p -> p.getName() + " (" + p.getColor() + ", " + p.getPrice() + "M)",
                p -> "-fx-border-color: " + cssColorFor(p.getColor() == null ? Color.BROWN : p.getColor()) + ";");
    }

        private Optional<Color> promptSelectRentColor(Player player, RentCard rentCard, List<Color> options) {
        List<Color> colors = new ArrayList<>();
        for (Color color : options) {
            colors.add(color);
        }
        if (colors.isEmpty()) {
            return Optional.empty();
        }
        return showStyledChoiceDialog("Select Rent Color", "Select which property set to collect rent from", "Color (count → rent):", colors,
                color -> color + "  ·  " + rentCard.countProperties(player, color) + " cards → "
                        + rentCard.calculateRent(player, color) + "M",
                color -> "-fx-background-color: " + cssColorFor(color) + "; -fx-text-fill: " + textColorFor(color) + ";");
    }

    private ActionEffectResult resolveDealBreaker(Player player, DealBreaker dealBreaker) {
        Optional<Player> target = promptSelectOpponentWithCompleteSets(player);
        if (target.isEmpty()) {
            showStatus("No player currently has a complete property set to steal", true);
            return ActionEffectResult.CANCELLED;
        }
        Player opponent = target.get();
        Optional<Color> color = promptSelectCompleteSetOnPlayer(opponent);
        if (color.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (tryRespondWithJustSayNo(opponent, player, "Deal Breaker(steal complete set)")) {
            return ActionEffectResult.CANCELLED;
        }
        if (!dealBreaker.useOnTarget(player, opponent, color.get())) {
            showStatus("Steal failed", true);
            return ActionEffectResult.FAILED;
        }
        logMessage(player.getName() + " stole complete " + color.get() + " set from " + color.get());
        return ActionEffectResult.SUCCESS;
    }

        private Optional<Player> promptSelectOpponentWithCompleteSets(Player current) {
        List<Player> valid = new ArrayList<>();
        for (Player p : gameEngine.getPlayers()) {
            if (!p.equals(current) && hasAnyCompleteSet(p)) {
                valid.add(p);
            }
        }
        return showStyledChoiceDialog("Deal Breaker", "Select player to steal complete set from", "Only showing players with complete sets:", valid,
                Player::getName, p -> null);
    }

    private void playWildPropertyCard(Player player, WildpropertyCard wild) {
        if (wild.isBankable()) {
            Optional<ActionPlayChoice> choice = promptWildPropertyChoice(wild);
            if (choice.isEmpty()) {
                selectedCard = wild;
                pendingPlayedCardView = null;
                return;
            }
            if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
                player.removeFromHand(wild);
                wild.depositToBank(player);
                logMessage(player.getName() + " deposited wild property [" + wild.getName()
                        + "] into bank (" + wild.getBankValueM() + "M)");
                showStatus("Wild card deposited to bank: " + wild.getBankValueM() + "M", false);
                completePlayStep(player, wild, true);
                return;
            }
        }

        Optional<Color> color = promptSelectWildColor(wild);
        if (color.isEmpty()) {
            selectedCard = wild;
            pendingPlayedCardView = null;
            showStatus("Cancelled, wild card kept in hand", false);
            updateUI();
            return;
        }

        wild.setChosenColor(color.get());
        player.removeFromHand(wild);
        wild.use(player, gameEngine);
        logMessage(player.getName() + " played wild property [" + wild.getName() + "] as " + color.get());
        showStatus("Wild property placed as " + color.get() + " in property area", false);
        completePlayStep(player, wild, false);
    }

        private Optional<ActionPlayChoice> promptWildPropertyChoice(WildpropertyCard wild) {
        ButtonType useBtn = new ButtonType("Play as Property");
        ButtonType bankBtn = new ButtonType("Deposit to Bank (" + wild.getBankValueM() + "M)");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> result = showStyledButtonDialog(
                "Wild Property Card",
                wild.getName() + " — Bank value " + wild.getBankValueM() + "M",
                "Play as property (choose a color), or deposit to bank for "
                        + wild.getBankValueM() + "M?",
                useBtn, bankBtn, cancelBtn);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        if (result.get() == useBtn) {
            return Optional.of(ActionPlayChoice.USE_EFFECT);
        }
        if (result.get() == bankBtn) {
            return Optional.of(ActionPlayChoice.DEPOSIT_BANK);
        }
        return Optional.empty();
    }

        private Optional<Color> promptSelectWildColor(WildpropertyCard wild) {
        List<Color> options = wild.getAvailableColors();
        if (options.isEmpty()) {
            return Optional.empty();
        }
        return showWildPropertyColorDialog(wild);
    }

    private Optional<Color> showWildPropertyColorDialog(WildpropertyCard wild) {
        List<Color> colors = wild.getAvailableColors();
        if (colors.isEmpty()) {
            return Optional.empty();
        }
        int bankValue = wild.getBankValueM();
        String bankHint = wild.isBankable()
                ? "Deposit to bank is always " + bankValue + "M (not affected by color chosen)."
                : "This wild card cannot be deposited to bank.";
        return showStyledChoiceDialog(
                "Wild Property Color",
                wild.getName(),
                "Choose a color to play as property.\n" + bankHint,
                colors,
                color -> color + "  —  play as " + color + " property",
                color -> "-fx-background-color: " + cssColorFor(color) + ";"
                        + "-fx-text-fill: " + textColorFor(color) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
    }

    private boolean hasAnyCompleteSet(Player player) {
        for (Color color : Color.values()) {
            if (player.hasCompleteSet(color)) {
                return true;
            }
        }
        return false;
    }

        private Optional<Player> promptSelectOpponent(Player current, String title) {
        List<Player> opponents = new ArrayList<>();
        for (Player p : gameEngine.getPlayers()) {
            if (!p.equals(current)) {
                opponents.add(p);
            }
        }
        return showStyledChoiceDialog(title, title, "Select player:", opponents, Player::getName, p -> null);
    }

        private Optional<Color> promptSelectCompleteSetOnPlayer(Player target) {
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (target.hasCompleteSet(color)) {
                options.add(color);
            }
        }
        return showColorChoiceDialog("Select Set", target.getName() + "'s complete sets", "Which color to steal?", options);
    }

        private Optional<Color> promptSelectOwnCompleteSet(Player player, String title) {
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (player.hasCompleteSet(color)) {
                options.add(color);
            }
        }
        return showColorChoiceDialog(title, title, "Select color set:", options);
    }

    /** Force switch to the next player after playing 3 cards */
    
    private void loadAvatarImage() {
        try {
            avatarImage = new Image(Objects.requireNonNull(
                    getClass().getResourceAsStream("/ui/avatar.png")));
        } catch (Exception ex) {
            avatarImage = null;
        }
    }

    private ImageView createAvatarView(double size) {
        ImageView view = new ImageView();
        if (avatarImage != null) {
            view.setImage(avatarImage);
        }
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        Circle clip = new Circle(size / 2, size / 2, size / 2);
        view.setClip(clip);
        view.getStyleClass().add("player-avatar");
        return view;
    }

    private void styleDialog(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        try {
            String css = Objects.requireNonNull(getClass().getResource("/ui/game-theme.css")).toExternalForm();
            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        } catch (Exception ignored) {
        }
        pane.getStyleClass().add("game-dialog");
        if (statusMessage != null && statusMessage.getScene() != null) {
            dialog.initOwner(statusMessage.getScene().getWindow());
        }
    }

    private Optional<ButtonType> showStyledButtonDialog(String title, String header, String content, ButtonType... buttons) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().setAll(buttons);
        VBox body = new VBox(10);
        body.getStyleClass().add("dialog-body");
        Label headerLabel = new Label(header);
        headerLabel.getStyleClass().add("dialog-header-label");
        Label contentLabel = new Label(content == null ? "" : content);
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("dialog-content-label");
        body.getChildren().addAll(headerLabel, contentLabel);
        pane.setContent(body);
        dialog.setResultConverter(button -> button);
        styleDialog(dialog);
        return dialog.showAndWait();
    }

    private <T> Optional<T> showStyledChoiceDialog(String title, String header, String prompt,
                                                    List<T> options, Function<T, String> labeler,
                                                    Function<T, String> colorStyleProvider) {
        if (options == null || options.isEmpty()) {
            return Optional.empty();
        }
        Dialog<T> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(cancel);
        dialog.setResultConverter(button -> null);

        VBox body = new VBox(12);
        body.getStyleClass().add("dialog-body");
        Label headerLabel = new Label(header);
        headerLabel.getStyleClass().add("dialog-header-label");
        Label promptLabel = new Label(prompt == null ? "" : prompt);
        promptLabel.setWrapText(true);
        promptLabel.getStyleClass().add("dialog-content-label");
        VBox choices = new VBox(8);
        choices.getStyleClass().add("dialog-choice-list");
        for (T option : options) {
            Button button = new Button(labeler.apply(option));
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("dialog-choice-button");
            String extraStyle = colorStyleProvider == null ? null : colorStyleProvider.apply(option);
            if (extraStyle != null && !extraStyle.isBlank()) {
                button.setStyle(extraStyle);
            }
            button.setOnAction(e -> {
                dialog.setResult(option);
                dialog.close();
            });
            choices.getChildren().add(button);
        }
        body.getChildren().addAll(headerLabel, promptLabel, choices);
        pane.setContent(body);
        styleDialog(dialog);
        return dialog.showAndWait();
    }

    private Optional<Color> showColorChoiceDialog(String title, String header, String prompt, List<Color> colors) {
        return showStyledChoiceDialog(title, header, prompt, colors,
                color -> color + "  -  " + color.getSetSize() + " cards to complete",
                color -> "-fx-background-color: " + cssColorFor(color) + ";"
                        + "-fx-text-fill: " + textColorFor(color) + ";"
                        + "-fx-border-color: rgba(255,255,255,0.55);");
    }

    private String cssColorFor(Color color) {
        return switch (color) {
            case BROWN -> "#8B5A2B";
            case DARK_BLUE -> "#174EA6";
            case GREEN -> "#1B7F43";
            case ORANGE -> "#F2994A";
            case RED -> "#D64545";
            case YELLOW -> "#F2C94C";
            case BLACK -> "#2D3436";
            case LIGHT_BLUE -> "#7EC8E3";
            case LIGHT_GREEN -> "#6FCF97";
            case PINK -> "#E84393";
        };
    }

    private String textColorFor(Color color) {
        return switch (color) {
            case YELLOW, LIGHT_BLUE, LIGHT_GREEN, ORANGE -> "#1F2A2E";
            default -> "white";
        };
    }

    private void unlockAchievement(String achievementId) {
        AchievementUi.unlockAndShow(achievementId, statusMessage);
    }

private void forceEndTurn() {
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
        Optional<ButtonType> result = showStyledButtonDialog(
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
        if (playersList == null) {
            return;
        }
        playersList.getChildren().clear();

        if (players == null) {
            return;
        }
        for (Player player : players) {
            boolean current = gameEngine != null && player.equals(gameEngine.getCurrentPlayer());
            playersList.getChildren().add(createPlayerInfoBox(player, current));
        }
    }
    
        private VBox createPlayerInfoBox(Player player, boolean isCurrent) {
        VBox box = new VBox(7);
        box.getStyleClass().add("player-info-card");
        if (isCurrent) {
            box.getStyleClass().add("player-info-current");
        }

        HBox header = new HBox(9);
        header.setAlignment(Pos.CENTER_LEFT);
        ImageView avatar = createAvatarView(42);
        Label nameLabel = new Label(player.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        header.getChildren().addAll(avatar, nameLabel);

        int handSize = player.getHand().size();

        Label handCountLabel = new Label("Hand: " + handSize + " cards");
        Label propertyCountLabel = new Label("Properties: " + propertyCountFor(player));
        Label bankLabel = new Label("Bank: " + bankTotalFor(player) + "M");

        box.getChildren().addAll(header, handCountLabel, propertyCountLabel, bankLabel);
        return box;
    }

    private int propertyCountFor(Player player) {
        return player.getAllProperties().size();
    }

    private int bankTotalFor(Player player) {
        return player.getBankTotalValue();
    }
    
    private void updateCurrentPlayerDisplay() {
        if (currentPlayerLabel == null) {
            return;
        }
        if (currentPlayer == null || gameEngine == null) {
            return;
        }
        String drawStatus = gameEngine.hasDrawnThisTurn() ? "Drew cards" : "Hasn't drawn";
        currentPlayerLabel.setText("Current Player: " + currentPlayer.getName()
                + " | " + drawStatus
                + " | Remaining plays: " + gameEngine.getRemainingPlays() + "/"
                + GameEngine.MAX_PLAYS_PER_TURN);
    }
    
    private void updatePlayerHand() {
        if (playerHand == null) {
            return;
        }
        playerHand.getChildren().clear();

        if (currentPlayer == null) {
            return;
        }

        List<Card> hand = getHandCardsForView();
        boolean canSelect = isMyActionTurn() && gameEngine.hasDrawnThisTurn();
        boolean canPlay = canSelect && canPlayFromView();
        CardView.CardMetrics metrics = computeHandMetrics(hand.size());

        for (Card card : hand) {
            StackPane slot = CardView.wrapInSlot(card, canSelect, metrics);
            CardView cardView = CardView.getCardView(slot);
            if (selectedCard != null && selectedCard.equals(card) && cardView != null) {
                cardView.setSelected(true);
                selectedCardView = cardView;
            }
            if (canSelect && cardView != null) {
                slot.setOnMouseClicked(event -> {
                    if (event.getButton() != MouseButton.PRIMARY) {
                        return;
                    }
                    selectCard(card, cardView);
                    if (event.getClickCount() == 2 && canPlay) {
                        event.consume();
                        cardView.playActivationAnimation(() -> playCardFromDoubleClick(card, cardView));
                    }
                });
            }
            playerHand.getChildren().add(slot);
        }
    }

    private void playCardFromDoubleClick(Card card, CardView cardView) {
        if (!getHandCardsForView().contains(card)) {
            return;
        }
        selectCard(card, cardView);
        playSelectedCard();
    }

    /** Single line display of hand: When the width is not enough, the face of the hand will be proportionally reduced (including hover size) */
    private CardView.CardMetrics computeHandMetrics(int cardCount) {
        if (cardCount <= 0) {
            return CardView.HAND;
        }
        double available = 600;
        if (playerHandScroll != null && playerHandScroll.getViewportBounds().getWidth() > 0) {
            available = playerHandScroll.getViewportBounds().getWidth() - 40;
        }
        double gap = 10;
        double total = cardCount * CardView.HAND.slotW() + (cardCount - 1) * gap;
        if (total <= available) {
            return CardView.HAND;
        }
        double factor = Math.max(0.6, available / total);
        return CardView.HAND.scaled(factor);
    }
    
    private void selectCard(Card card, CardView cardView) {
        for (javafx.scene.Node node : playerHand.getChildren()) {
            CardView cv = node instanceof StackPane sp ? CardView.getCardView(sp) : null;
            if (cv != null) {
                cv.setSelected(false);
            }
        }

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
            showStatus("已选择：" + card.getName() + "，双击出牌，或点击 Discard Selected Card 丢弃", false);
        }
        updateButtonStates();
    }
    
    private void updatePlayerBank() {
        if (playerBank == null) {
            return;
        }
        playerBank.getChildren().clear();

        if (currentPlayer == null) {
            return;
        }

        int total = getBankTotalForView();
        if (bankTotalLabel != null) {
            bankTotalLabel.setText(total + "M");
        }

        List<Card> bankCards = getBankCardsForView();
        for (Card card : bankCards) {
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
        if (allPlayersPropertiesPanel == null) {
            return;
        }
        allPlayersPropertiesPanel.getChildren().clear();

        List<PlayerBoardView> views = getPublicPlayerViews();
        if (views.isEmpty()) {
            Label empty = new Label("(No player properties yet)");
            empty.setStyle("-fx-text-fill: #7f8c8d;");
            allPlayersPropertiesPanel.getChildren().add(empty);
            return;
        }

        double rowHeight = computePropertyRowHeight(views.size());
        lastPropertyRowHeight = rowHeight;
        CardView.CardMetrics propertyMetrics = PublicPropertyBoardLayout.cardMetricsForRow(rowHeight);
        double avatarSize = Math.min(38, Math.max(24, rowHeight - 28));

        for (PlayerBoardView view : views) {
            VBox playerBlock = new VBox(6);
            playerBlock.setMaxWidth(Double.MAX_VALUE);
            playerBlock.setMinHeight(0);
            playerBlock.setMaxHeight(Double.MAX_VALUE);
            boolean isTurn = isTurnSeat(view.seat);
            playerBlock.getStyleClass().add("player-public-block");
            if (isTurn) {
                playerBlock.getStyleClass().add("player-public-block-current");
            }

            HBox titleRow = new HBox(9);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            ImageView avatar = createAvatarView(avatarSize);
            Label title = new Label((isTurn ? "▶ " : "") + view.name + "  |  Hand: " + view.handSize + " cards  |  Bank: " + view.bankTotal + "M");
            title.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #103c2a;");
            titleRow.getChildren().addAll(avatar, title);

            FlowPane props = new FlowPane(10, 10);
            props.setPrefWrapLength(Math.max(320, resolvePublicBoardRowWidth() - 40));
            props.setMaxWidth(Double.MAX_VALUE);
            props.setMaxHeight(propertyMetrics.slotH() + 56);
            if (view.properties.isEmpty()) {
                props.getChildren().add(new Label("(No properties)"));
            } else {
                Map<Color, List<Card>> byColor = groupPropertiesByColor(view.properties);
                for (Map.Entry<Color, List<Card>> entry : byColor.entrySet()) {
                    props.getChildren().add(PublicPropertySetView.build(entry.getKey(), entry.getValue(), propertyMetrics));
                }
            }

            ScrollPane propsScroll = new ScrollPane(props);
            propsScroll.setFitToHeight(true);
            propsScroll.setMinHeight(0);
            propsScroll.setMaxHeight(Double.MAX_VALUE);
            propsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            propsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            propsScroll.setPannable(true);
            propsScroll.getStyleClass().add("transparent-scroll");
            VBox.setVgrow(propsScroll, Priority.ALWAYS);

            playerBlock.getChildren().addAll(titleRow, propsScroll);
            allPlayersPropertiesPanel.getChildren().add(playerBlock);
        }
        PublicPropertyBoardLayout.applyEqualRows(allPlayersPropertiesPanel, views.size());
    }

    private double resolvePublicBoardRowWidth() {
        if (allPlayersPropertiesPanel == null) {
            return 640;
        }
        double width = allPlayersPropertiesPanel.getWidth();
        if (width > 0) {
            return width;
        }
        if (publicBoardPanel != null && publicBoardPanel.getWidth() > 0) {
            return publicBoardPanel.getWidth();
        }
        return 640;
    }

    private void updateAllPlayersBankBar() {
        if (allPlayersBankBar == null || gameEngine == null) {
            return;
        }
        allPlayersBankBar.getChildren().clear();
        int currentSeat = getCurrentTurnSeat();
        for (int i = 0; i < gameEngine.getPlayers().size(); i++) {
            Player player = gameEngine.getPlayers().get(i);
            allPlayersBankBar.getChildren().add(createBankPill(
                    player.getName(), player.getBankTotalValue(), i == currentSeat));
        }
    }

    private VBox createBankPill(String name, int total, boolean isCurrent) {
        VBox pill = new VBox(2);
        pill.getStyleClass().add("bank-pill");
        if (isCurrent) {
            pill.getStyleClass().add("bank-pill-current");
        }
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("bank-pill-label");
        Label valueLabel = new Label(total + "M");
        valueLabel.getStyleClass().add("bank-pill-value");
        pill.getChildren().addAll(nameLabel, valueLabel);
        return pill;
    }

    private Map<Color, List<Card>> groupPropertiesByColor(List<Card> properties) {
        Map<Color, List<Card>> byColor = new LinkedHashMap<>();
        for (Card card : properties) {
            Color color = card.getColor() != null ? card.getColor() : Color.BROWN;
            byColor.computeIfAbsent(color, k -> new ArrayList<>()).add(card);
        }
        return byColor;
    }

    private void updatePileCounts() {
        if (gameStatusText == null) {
            return;
        }
        if (gameEngine == null || gameEngine.isGameOver()) {
            return;
        }
        int draw = gameEngine.getDeck().size();
        int discard = gameEngine.getDiscardPile().size();
        gameStatusText.setText("Draw pile: " + draw + "  |  Discard pile: " + discard);
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
        Platform.runLater(() -> {
            statusMessage.setText(message);
            if (isError) {
                statusMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14px;");
            } else {
                statusMessage.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            }
            FadeTransition fade = new FadeTransition(Duration.millis(220), statusMessage);
            fade.setFromValue(0.25);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_OUT);
            fade.play();
        });
    }
    
        private void showGameOver(Player winner) {
        Platform.runLater(() -> {
            showStyledButtonDialog("Game Over", "Congratulations!", winner.getName() + " wins the game!",
                    new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
            gameStatusText.setText("Game Over - " + winner.getName() + " Wins!");
            logMessage("=== GAME OVER === " + winner.getName() + " wins!");
            drawCardBtn.setDisable(true);
            discardCardBtn.setDisable(true);
            endTurnBtn.setDisable(true);
        });
    }

    private void afterStateChange() {
        updateUI();
    }

    private static final class PlayerBoardView {
        int seat;
        String name;
        int handSize;
        int bankTotal;
        List<Card> properties = new ArrayList<>();
    }

    private List<Card> getHandCardsForView() {
        return currentPlayer != null ? currentPlayer.getHand() : List.of();
    }

    private List<Card> getBankCardsForView() {
        return currentPlayer != null ? currentPlayer.getBank() : List.of();
    }

    private int getBankTotalForView() {
        return currentPlayer != null ? currentPlayer.getBankTotalValue() : 0;
    }

    private boolean canPlayFromView() {
        return gameEngine != null
                && gameEngine.hasDrawnThisTurn()
                && gameEngine.canPlayCard();
    }

    private List<PlayerBoardView> getPublicPlayerViews() {
        if (gameEngine == null) {
            return List.of();
        }
        List<PlayerBoardView> list = new ArrayList<>();
        for (int i = 0; i < gameEngine.getPlayers().size(); i++) {
            Player p = gameEngine.getPlayers().get(i);
            PlayerBoardView v = new PlayerBoardView();
            v.seat = i;
            v.name = p.getName();
            v.handSize = p.getHandSize();
            v.bankTotal = p.getBankTotalValue();
            v.properties.addAll(p.getAllProperties());
            list.add(v);
        }
        return list;
    }

    private int getCurrentTurnSeat() {
        return gameEngine != null ? gameEngine.getCurrentPlayerIndex() : 0;
    }

    private boolean isTurnSeat(int seat) {
        return seat == getCurrentTurnSeat();
    }

    private String playerNameAt(int seat) {
        if (gameEngine != null && seat >= 0 && seat < gameEngine.getPlayers().size()) {
            return gameEngine.getPlayers().get(seat).getName();
        }
        return "Player " + (seat + 1);
    }
}
