package controller;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import engine.PropertyRules;
import javafx.application.Platform;
import javafx.animation.TranslateTransition;
import sync.*;
import ui.CardView;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import javafx.util.Duration;

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
    private TilePane allPlayersPropertiesPanel;

    @FXML
    private Label turnBankLabel;
    
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
    private Button playCardBtn;
    
    @FXML
    private Button endTurnBtn;
    
    @FXML
    private Button newGameBtn;
    
    private GameEngine gameEngine;
    private Deck deck;
    private List<Player> players;
    private Player currentPlayer;
    private Card selectedCard;
    private Random random;

    private enum SessionMode { LOCAL, HOST, CLIENT }
    private SessionMode sessionMode = SessionMode.LOCAL;
    private RoomFolder roomFolder;
    private int localSeat;
    private RoomSyncWatcher roomWatcher;
    private final List<String> roomLogLines = new ArrayList<>();
    private List<Card> viewHand = new ArrayList<>();
    private List<Card> viewBank = new ArrayList<>();
    private RoomPublicSnapshot remotePublic;
    private long lastSeenVersion = -1;
    private static final double HAND_DOCK_HEIGHT = 266;
    private static final double HAND_DOCK_PEEK = 54;
    private boolean handDockExpanded = false;
    private Image avatarImage;
    private final Set<String> unlockedAchievements = new HashSet<>();
    
    @FXML
    public void initialize() {
        random = new Random();
        loadAvatarImage();
        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, oldW, newW) -> {
                if (!viewHand.isEmpty() || currentPlayer != null) {
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
        stopSync();
        sessionMode = SessionMode.LOCAL;
        roomFolder = null;
        initializeGame();
    }

    public void startRoomHost(RoomFolder folder, int seat, List<String> playerNames) throws Exception {
        stopSync();
        sessionMode = SessionMode.HOST;
        roomFolder = folder;
        localSeat = seat;
        initializeGameWithPlayers(playerNames);
        RoomStorage.markStarted(folder);
        publishRoomState();
        startRoomWatch();
        pullRemoteStateQuiet();
    }

    public void startRoomClient(RoomFolder folder, int seat) {
        stopSync();
        sessionMode = SessionMode.CLIENT;
        roomFolder = folder;
        localSeat = seat;
        gameEngine = null;
        players = new ArrayList<>();
        remotePublic = null;
        viewHand = new ArrayList<>();
        viewBank = new ArrayList<>();
        startRoomWatch();
        pullRemoteStateQuiet();
    }
    
    private void setupButtonActions() {
        if (drawCardBtn != null) {
            drawCardBtn.setOnAction(e -> onDrawCardsClick());
        }
        if (playCardBtn != null) {
            playCardBtn.setOnAction(e -> onPlayCardClick());
        }
        if (endTurnBtn != null) {
            endTurnBtn.setOnAction(e -> onEndTurnClick());
        }
        if (newGameBtn != null) {
            newGameBtn.setOnAction(e -> onNewGameClick());
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
        handDock.setOnMouseEntered(e -> setHandDockExpanded(true, true));
        handDock.setOnMouseExited(e -> setHandDockExpanded(false, true));
    }

private void setHandDockExpanded(boolean expanded, boolean animate) {
        if (handDock == null || handDockExpanded == expanded && animate) {
            return;
        }
        handDockExpanded = expanded;
        double collapsedY = HAND_DOCK_HEIGHT - HAND_DOCK_PEEK;
        double targetY = expanded ? 0 : collapsedY;
        if (handDockHint != null) {
            handDockHint.setText(expanded
                    ? "Mouse out to tuck hand back · Double-click to play"
                    : "Hover here to expand · Double-click to play");
        }
        if (!animate) {
            handDock.setTranslateY(targetY);
            return;
        }
        TranslateTransition transition = new TranslateTransition(Duration.millis(170), handDock);
        transition.setToY(targetY);
        transition.play();
    }

private void setupPublicBoardSizing() {
        if (allPlayersPropertiesPanel == null) {
            return;
        }
        allPlayersPropertiesPanel.widthProperty().addListener((obs, oldW, newW) -> {
            double width = newW.doubleValue();
            int columns = width > 1060 ? 2 : 1;
            allPlayersPropertiesPanel.setPrefColumns(columns);
            allPlayersPropertiesPanel.setPrefTileWidth(Math.max(420, (width - 32) / columns));
        });
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
        roomLogLines.clear();
        resetAchievements();
        logMessage("Players: " + String.join(", ", names));
        updateUI();
    }
    
    @FXML
    private void onDrawCardsClick() {
        if (!isMyActionTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (sessionMode == SessionMode.CLIENT) {
            submitRoomCommand("DRAW", null, null, null);
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
        unlockAchievement("first-draw", "First Draw", "First successful draw: You have initiated the achievement system test for this game!");
        showStatus("Two cards have already been drawn (cannot be drawn again in this round). Remaining number of available card games: "
                + gameEngine.getRemainingPlays(), false);
        afterStateChange();
    }
    
    @FXML
    private void onPlayCardClick() {
        if (!isMyActionTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("Please first click on the hand to select a card, then click on「Play」", true);
            return;
        }
        if (sessionMode == SessionMode.CLIENT) {
            playSelectedCardAsClient();
            return;
        }
        playSelectedCard();
    }
    
    private void playSelectedCard() {
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
    }

    private void completePlayStep(Player player, Card played, boolean depositedToBank) {
        gameEngine.recordCardPlayed();

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
            return resolveDoubleTheRent(doubleRent);
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

    private ActionEffectResult resolveDoubleTheRent(DoubleTheRent doubleRent) {
        if (!doubleRent.activateForNextRent(gameEngine)) {
            showStatus("Double rent already activated this round, please play a Rent card first", true);
            return ActionEffectResult.FAILED;
        }
        showStatus("Double rent activated: play a Rent card this round", false);
        logMessage("Double rent activated, next Rent will be doubled");
        return ActionEffectResult.SUCCESS;
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

        defender.removeFromHand(justSayNo);
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
                wild.getName() + " — Can deposit to bank for " + wild.getBankValueM() + "M",
                "Play as property (select color), or deposit to bank?",
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
        return showColorChoiceDialog("Wild Property Color", wild.getName(), "Select color for property:", options);
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
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
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
                color -> color + "  ·  " + color.getSetSize() + " 张成套",
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

    private void resetAchievements() {
        unlockedAchievements.clear();
    }

    private void unlockAchievement(String id, String title, String description) {
        if (!unlockedAchievements.add(id)) {
            return;
        }
        logMessage("🏆 Achievement unlocked: " + title);
        showAchievementDialog(title, description);
    }

    private void showAchievementDialog(String title, String description) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Achievement Unlocked");
        ButtonType ok = new ButtonType("Nice!", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(ok);
        HBox body = new HBox(14);
        body.setAlignment(Pos.CENTER_LEFT);
        body.getStyleClass().add("achievement-dialog-body");
        Label icon = new Label("🏆");
        icon.getStyleClass().add("achievement-icon");
        VBox textBox = new VBox(6);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("achievement-title");
        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("achievement-description");
        textBox.getChildren().addAll(titleLabel, descLabel);
        body.getChildren().addAll(icon, textBox);
        dialog.getDialogPane().setContent(body);
        styleDialog(dialog);
        dialog.showAndWait();
    }

private void forceEndTurn() {
        Player ending = gameEngine.getCurrentPlayer();
        List<Card> discarded = gameEngine.enforceHandSizeLimit(ending);
        if (!discarded.isEmpty()) {
            logMessage(ending.getName() + " exceeded hand limit. Discarded " + discarded.size() + " card(s) to reduce hand to " + GameEngine.MAX_HAND_SIZE + " cards");
            showStatus(ending.getName() + " had too many cards! Automatically discarded " + discarded.size() + " card(s)", false);
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
        if (sessionMode == SessionMode.CLIENT) {
            submitRoomCommand("END_TURN", null, null, null);
            return;
        }
        endCurrentTurn();
    }
    
    private void endCurrentTurn() {
        Player player = gameEngine.getCurrentPlayer();
        List<Card> discarded = gameEngine.enforceHandSizeLimit(player);
        if (!discarded.isEmpty()) {
            logMessage(player.getName() + " exceeded hand limit. Discarded " + discarded.size() + " card(s) to reduce hand to " + GameEngine.MAX_HAND_SIZE + " cards");
            showStatus(player.getName() + " had too many cards! Automatically discarded " + discarded.size() + " card(s)", false);
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
        if (sessionMode == SessionMode.CLIENT) {
            return remotePublic != null
                    && remotePublic.currentPlayerIndex == localSeat
                    && !remotePublic.gameOver;
        }
        if (sessionMode == SessionMode.HOST) {
            return gameEngine != null
                    && gameEngine.getCurrentPlayerIndex() == localSeat
                    && !gameEngine.isGameOver();
        }
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
            updateTurnBankLabel();
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

        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            for (PlayerPublicSnapshot view : remotePublic.players) {
                Player stub = new Player(view.name);
                playersList.getChildren().add(createPlayerInfoBox(stub, isTurnSeat(view.seat)));
            }
            return;
        }

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

        int handSize = sessionMode == SessionMode.CLIENT && remotePublic != null
                ? handSizeFor(player.getName()) : player.getHand().size();

        Label handCountLabel = new Label("Hand: " + handSize + " cards");
        Label propertyCountLabel = new Label("Properties: " + propertyCountFor(player));
        Label bankLabel = new Label("Bank: " + bankTotalFor(player) + "M");

        box.getChildren().addAll(header, handCountLabel, propertyCountLabel, bankLabel);
        return box;
    }

    private int handSizeFor(String name) {
        if (remotePublic == null) {
            return 0;
        }
        for (PlayerPublicSnapshot p : remotePublic.players) {
            if (p.name.equals(name)) {
                return p.handSize;
            }
        }
        return 0;
    }

    private int propertyCountFor(Player player) {
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            for (PlayerPublicSnapshot p : remotePublic.players) {
                if (p.name.equals(player.getName())) {
                    return p.properties.size();
                }
            }
        }
        return player.getAllProperties().size();
    }

    private int bankTotalFor(Player player) {
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            for (PlayerPublicSnapshot p : remotePublic.players) {
                if (p.name.equals(player.getName())) {
                    return p.bankTotal;
                }
            }
        }
        return player.getBankTotalValue();
    }
    
    private void updateCurrentPlayerDisplay() {
        if (currentPlayerLabel == null) {
            return;
        }
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            String name = playerNameAt(remotePublic.currentPlayerIndex);
            String drawStatus = remotePublic.hasDrawnThisTurn ? "Drew cards" : "Hasn't drawn";
            currentPlayerLabel.setText("Current Player: " + name
                    + " | " + drawStatus
                    + " | Remaining plays: " + remotePublic.remainingPlays + "/3"
                    + (remotePublic.gameOver ? " | Game Over" : ""));
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

        if (currentPlayer == null && sessionMode != SessionMode.CLIENT) {
            return;
        }

        List<Card> hand = getHandCardsForView();
        boolean handClickable = isMyActionTurn() && canPlayFromView();
        CardView.CardMetrics metrics = computeHandMetrics(hand.size());

        for (Card card : hand) {
            StackPane slot = CardView.wrapInSlot(card, handClickable, metrics);
            CardView cardView = CardView.getCardView(slot);
            if (handClickable && cardView != null) {
                slot.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        selectCard(card, cardView);
                        playSelectedCard();
                    } else {
                        selectCard(card, cardView);
                    }
                });
            }
            playerHand.getChildren().add(slot);
        }
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
            showStatus("Selected: " + card.getName() + ", click 'Play' or double-click to play", false);
        }
        updateButtonStates();
    }
    
    private void updatePlayerBank() {
        if (playerBank == null) {
            return;
        }
        playerBank.getChildren().clear();

        if (currentPlayer == null && sessionMode != SessionMode.CLIENT) {
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

        List<PlayerPublicSnapshot> views = getPublicPlayerViews();
        if (views.isEmpty()) {
            Label empty = new Label("(No player properties yet)");
            empty.setStyle("-fx-text-fill: #7f8c8d;");
            allPlayersPropertiesPanel.getChildren().add(empty);
            return;
        }

        for (PlayerPublicSnapshot view : views) {
            VBox playerBlock = new VBox(8);
            playerBlock.setMaxWidth(Double.MAX_VALUE);
            boolean isTurn = isTurnSeat(view.seat);
            playerBlock.getStyleClass().add("player-public-block");
            if (isTurn) {
                playerBlock.getStyleClass().add("player-public-block-current");
            }

            HBox titleRow = new HBox(9);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            ImageView avatar = createAvatarView(38);
            Label title = new Label((isTurn ? "▶ " : "") + view.name + "  |  Hand: " + view.handSize + " cards  |  Bank: " + view.bankTotal + "M");
            title.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #103c2a;");
            titleRow.getChildren().addAll(avatar, title);

            FlowPane props = new FlowPane(13, 13);
            props.setPrefWrapLength(640);
            props.setMaxWidth(Double.MAX_VALUE);
            if (view.properties.isEmpty()) {
                props.getChildren().add(new Label("(No properties)"));
            } else {
                Map<Color, List<Card>> byColor = groupPropertiesByColor(
                        CardSnapshotMapper.fromSnapshots(view.properties));
                for (Map.Entry<Color, List<Card>> entry : byColor.entrySet()) {
                    HBox set = buildPropertyColorSet(entry.getKey(), entry.getValue());
                    props.getChildren().add(set);
                }
            }
            playerBlock.getChildren().addAll(titleRow, props);
            allPlayersPropertiesPanel.getChildren().add(playerBlock);
        }
    }

    private void updateTurnBankLabel() {
        if (turnBankLabel == null) {
            return;
        }
        int seat = getCurrentTurnSeat();
        int total = 0;
        String name = playerNameAt(seat);
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            for (PlayerPublicSnapshot p : remotePublic.players) {
                if (p.seat == seat) {
                    total = p.bankTotal;
                    break;
                }
            }
        } else if (gameEngine != null && seat >= 0 && seat < gameEngine.getPlayers().size()) {
            total = gameEngine.getPlayers().get(seat).getBankTotalValue();
        }
        turnBankLabel.setText(name + ": " + total + "M");
    }

    private HBox buildPropertyColorSet(Color color, List<Card> cards) {
        HBox colorSet = new HBox(10);
        colorSet.setAlignment(Pos.CENTER_LEFT);
        colorSet.getStyleClass().add("property-set");
        if (cards.size() >= color.getSetSize()) {
            colorSet.getStyleClass().add("property-set-complete");
        }
        Label groupLabel = new Label(color + "\n" + cards.size() + "/" + color.getSetSize());
        groupLabel.setStyle("-fx-font-weight: 900; -fx-font-size: 12px; -fx-text-fill: #25342d;");
        groupLabel.setMinWidth(64);
        HBox row = new HBox(8);
        for (Card card : cards) {
            row.getChildren().add(CardView.wrapInSlot(card, false, CardView.PUBLIC));
        }
        colorSet.getChildren().addAll(groupLabel, row);
        return colorSet;
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
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            gameStatusText.setText("Draw pile: " + remotePublic.drawPileSize
                    + "  |  Discard pile: " + remotePublic.discardPileSize);
            if (remotePublic.gameOver && remotePublic.winnerName != null) {
                gameStatusText.setText("Winner: " + remotePublic.winnerName);
            }
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
        boolean canDraw;
        boolean canPlayCard;
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            canDraw = isMyActionTurn() && !remotePublic.hasDrawnThisTurn;
            canPlayCard = isMyActionTurn() && remotePublic.remainingPlays > 0;
        } else if (gameEngine != null) {
            canDraw = isMyActionTurn() && gameEngine.canDrawCards();
            canPlayCard = isMyActionTurn() && gameEngine.canPlayCard();
        } else {
            canDraw = false;
            canPlayCard = false;
        }

        drawCardBtn.setDisable(!canDraw);
        playCardBtn.setDisable(!canPlayCard || selectedCard == null);
        endTurnBtn.setDisable(!isMyActionTurn());
    }


    private void logMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String line = "[" + timestamp + "] " + message;
            gameLog.appendText(line + "\n");
            gameLog.setScrollTop(Double.MAX_VALUE);
            roomLogLines.add(line);
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
        });
    }
    
        private void showGameOver(Player winner) {
        Platform.runLater(() -> {
            showStyledButtonDialog("Game Over", "Congratulations!", winner.getName() + " wins the game!",
                    new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
            gameStatusText.setText("Game Over - " + winner.getName() + " Wins!");
            logMessage("=== GAME OVER === " + winner.getName() + " wins!");
            drawCardBtn.setDisable(true);
            playCardBtn.setDisable(true);
            endTurnBtn.setDisable(true);
        });
    }

    private void afterStateChange() {
        updateUI();
        if (sessionMode == SessionMode.HOST && roomFolder != null) {
            publishRoomState();
        }
    }

    private void startRoomWatch() {
        stopRoomWatch();
        if (roomFolder == null) {
            return;
        }
        try {
            roomWatcher = new RoomSyncWatcher();
            roomWatcher.start(roomFolder, this::onRoomFilesChanged);
        } catch (IOException e) {
            showStatus("Unable to listen to room folder: " + e.getMessage(), true);
        }
    }

    private void stopRoomWatch() {
        if (roomWatcher != null) {
            roomWatcher.close();
            roomWatcher = null;
        }
    }

    private void onRoomFilesChanged() {
        Platform.runLater(() -> {
            if (roomFolder == null) {
                return;
            }
            try {
                if (sessionMode == SessionMode.HOST) {
                    drainAndProcessCommands();
                } else if (sessionMode == SessionMode.CLIENT) {
                    pullRemoteStateQuiet();
                }
            } catch (Exception ex) {
                showStatus("Synchronization failed: " + ex.getMessage(), true);
            }
        });
    }

    private void drainAndProcessCommands() throws Exception {
        for (RoomCommand cmd : RoomStorage.drainCommands(roomFolder)) {
            handleRoomCommand(cmd);
        }
    }

    private void startSyncTimer() {
        startRoomWatch();
    }

    private void stopSync() {
        stopRoomWatch();
    }

    private void publishRoomState() {
        if (gameEngine == null || roomFolder == null) {
            return;
        }
        try {
            RoomPublicSnapshot pub = RoomSnapshotBuilder.buildPublic(gameEngine, roomLogLines);
            RoomStorage.writeSnapshots(roomFolder, pub, RoomSnapshotBuilder.buildAllPrivate(gameEngine));
            lastSeenVersion = pub.version;
        } catch (Exception ex) {
            showStatus("Failed to save room status: " + ex.getMessage(), true);
        }
    }

    private List<String> collectNewLogLines() {
        return new ArrayList<>(roomLogLines);
    }

    private void pullRemoteStateQuiet() {
        try {
            long version = RoomStorage.peekVersion(roomFolder);
            if (version <= lastSeenVersion) {
                return;
            }
            RoomPublicSnapshot pub = RoomStorage.readPublic(roomFolder);
            PlayerPrivateSnapshot priv = RoomStorage.readPrivate(roomFolder, localSeat);
            if (pub == null || priv == null) {
                return;
            }
            lastSeenVersion = version;
            remotePublic = pub;
            viewHand = CardSnapshotMapper.fromSnapshots(priv.hand);
            viewBank = CardSnapshotMapper.fromSnapshots(priv.bank);
            mergeRemoteLog(pub.logLines);
            updateUI();
        } catch (Exception ignored) {
        }
    }

    private void mergeRemoteLog(List<String> lines) {
        if (lines == null || gameLog == null) {
            return;
        }
        int start = Math.min(roomLogLines.size(), lines.size());
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            roomLogLines.add(line);
            gameLog.appendText(line + "\n");
        }
        gameLog.setScrollTop(Double.MAX_VALUE);
    }

    private void submitRoomCommand(String action, String cardId, String mode, String color) {
        if (roomFolder == null) {
            return;
        }
        try {
            RoomCommand cmd = new RoomCommand();
            cmd.seat = localSeat;
            cmd.action = action;
            cmd.cardId = cardId;
            cmd.mode = mode;
            cmd.color = color;
            RoomStorage.submitCommand(roomFolder, cmd);
            showStatus("Operation submitted, waiting for Host sync…", false);
        } catch (IOException e) {
            showStatus("Submission failed: " + e.getMessage(), true);
        }
    }

    private void handleRoomCommand(RoomCommand cmd) throws Exception {
        if (gameEngine == null || cmd == null || cmd.seat != gameEngine.getCurrentPlayerIndex()) {
            return;
        }
        switch (cmd.action) {
            case "DRAW" -> drawCardsFromPile();
            case "END_TURN" -> endCurrentTurn();
            case "PLAY" -> playCardById(cmd.cardId, cmd.mode, cmd.color);
            default -> {
            }
        }
    }

    private void playCardById(String cardId, String mode, String color) {
        if (cardId == null || gameEngine == null) {
            return;
        }
        Player player = gameEngine.getCurrentPlayer();
        Card card = player.findInHandById(cardId);
        if (card == null) {
            return;
        }
        selectedCard = card;
        if (card instanceof WildpropertyCard wild) {
            if ("BANK".equalsIgnoreCase(mode)) {
                if (wild.isBankable()) {
                    player.removeFromHand(wild);
                    wild.depositToBank(player);
                    completePlayStep(player, wild, true);
                }
            } else if (color != null) {
                Color c = CardSnapshotMapper.parseColor(color);
                if (c != null) {
                    wild.setChosenColor(c);
                    player.removeFromHand(wild);
                    wild.use(player, gameEngine);
                    completePlayStep(player, wild, false);
                }
            }
            return;
        }
        if (card instanceof ActionCard action && "BANK".equalsIgnoreCase(mode)) {
            player.removeFromHand(action);
            action.depositToBank(player);
            completePlayStep(player, action, true);
            return;
        }
        if (card instanceof MoneyCard || card instanceof PropertyCard) {
            card.use(player, gameEngine);
            player.removeFromHand(card);
            completePlayStep(player, card, false);
        }
    }

    private void playSelectedCardAsClient() {
        Card card = selectedCard;
        selectedCard = null;
        if (card instanceof WildpropertyCard wild) {
            if (wild.isBankable()) {
                ButtonType asProp = new ButtonType("Play as Property");
                ButtonType asBank = new ButtonType("Deposit to Bank");
                ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                Optional<ButtonType> r = showStyledButtonDialog("Wild Property Card", wild.getName(),
                        "Choose to play as property or deposit to bank.", asProp, asBank, cancel);
                if (r.isEmpty() || r.get() == cancel) {
                    selectedCard = wild;
                    return;
                }
                if (r.get() == asBank) {
                    submitRoomCommand("PLAY", wild.getInstanceId(), "BANK", null);
                    return;
                }
            }
            List<Color> colors = wild.getAvailableColors();
            if (!colors.isEmpty()) {
                Optional<Color> c = showColorChoiceDialog("Wild Property Color", wild.getName(),
                        "Select color for property:", colors);
                if (c.isPresent()) {
                    submitRoomCommand("PLAY", wild.getInstanceId(), "PROPERTY", c.get().name());
                } else {
                    selectedCard = wild;
                }
            }
            return;
        }
        if (card instanceof ActionCard || card instanceof RentCard) {
            ButtonType bank = new ButtonType("Deposit to Bank");
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            Optional<ButtonType> r = showStyledButtonDialog("Action/Rent Card", card.getName(),
                    "Pseudo-online: Please deposit action/rent cards to bank first", bank, cancel);
            if (r.isPresent() && r.get() == bank) {
                submitRoomCommand("PLAY", card.getInstanceId(), "BANK", null);
            } else {
                selectedCard = card;
            }
            return;
        }
        submitRoomCommand("PLAY", card.getInstanceId(), "PLAY", null);
    }

    private List<Card> getHandCardsForView() {
        if (sessionMode == SessionMode.CLIENT) {
            return viewHand;
        }
        if (sessionMode == SessionMode.HOST || sessionMode == SessionMode.LOCAL) {
            if (sessionMode == SessionMode.LOCAL && gameEngine != null) {
                return gameEngine.getCurrentPlayer().getHand();
            }
            if (sessionMode == SessionMode.HOST && gameEngine != null && localSeat >= 0) {
                return gameEngine.getPlayers().get(localSeat).getHand();
            }
        }
        return currentPlayer != null ? currentPlayer.getHand() : List.of();
    }

    private List<Card> getBankCardsForView() {
        if (sessionMode == SessionMode.CLIENT) {
            return viewBank;
        }
        if (gameEngine != null && localSeat >= 0 && sessionMode != SessionMode.LOCAL) {
            return gameEngine.getPlayers().get(localSeat).getBank();
        }
        return currentPlayer != null ? currentPlayer.getBank() : List.of();
    }

    private int getBankTotalForView() {
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            for (PlayerPublicSnapshot p : remotePublic.players) {
                if (p.seat == localSeat) {
                    return p.bankTotal;
                }
            }
        }
        if (gameEngine != null && localSeat >= 0 && sessionMode != SessionMode.LOCAL) {
            return gameEngine.getPlayers().get(localSeat).getBankTotalValue();
        }
        return currentPlayer != null ? currentPlayer.getBankTotalValue() : 0;
    }

    private boolean canPlayFromView() {
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            return remotePublic.remainingPlays > 0;
        }
        return gameEngine != null && gameEngine.canPlayCard();
    }

    private List<PlayerPublicSnapshot> getPublicPlayerViews() {
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            return remotePublic.players;
        }
        if (gameEngine == null) {
            return List.of();
        }
        List<PlayerPublicSnapshot> list = new ArrayList<>();
        for (int i = 0; i < gameEngine.getPlayers().size(); i++) {
            Player p = gameEngine.getPlayers().get(i);
            PlayerPublicSnapshot v = new PlayerPublicSnapshot();
            v.seat = i;
            v.name = p.getName();
            v.handSize = p.getHandSize();
            v.bankTotal = p.getBankTotalValue();
            for (PropertyCard property : p.getAllProperties()) {
                v.properties.add(CardSnapshotMapper.toSnapshot(property));
            }
            list.add(v);
        }
        return list;
    }

    private int getCurrentTurnSeat() {
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            return remotePublic.currentPlayerIndex;
        }
        if (gameEngine != null) {
            return gameEngine.getCurrentPlayerIndex();
        }
        return 0;
    }

    private boolean isTurnSeat(int seat) {
        return seat == getCurrentTurnSeat();
    }

    private String playerNameAt(int seat) {
        if (sessionMode == SessionMode.CLIENT && remotePublic != null) {
            for (PlayerPublicSnapshot p : remotePublic.players) {
                if (p.seat == seat) {
                    return p.name;
                }
            }
        }
        if (gameEngine != null && seat >= 0 && seat < gameEngine.getPlayers().size()) {
            return gameEngine.getPlayers().get(seat).getName();
        }
        return "Player " + (seat + 1);
    }
}
