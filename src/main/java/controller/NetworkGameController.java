package controller;

import engine.PaymentTransfer;
import engine.PropertyRules;
import engine.RentTable;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import model.achievement.AchievementManager;
import model.card.*;
import model.card.actionCard.*;
import model.enums.Color;
import model.player.Player;
import network.CardMapper;
import network.client.NetworkClient;
import network.protocol.ClientMessage;
import network.protocol.GameStateDto;
import network.protocol.InteractionPromptDto;
import network.protocol.MessageTypes;
import network.protocol.PlayerViewDto;
import network.protocol.ServerMessage;
import network.server.PendingActionResolution;
import ui.CardView;
import ui.AchievementUi;

import java.util.*;
import java.util.function.Function;

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
    @FXML private TilePane allPlayersPropertiesPanel;
    @FXML private HBox allPlayersBankBar;
    @FXML private ScrollPane playerHandScroll;
    @FXML private HBox playerHand;
    @FXML private FlowPane playerBank;
    @FXML private Label bankTotalLabel;
    @FXML private TextArea gameLog;
    @FXML private Button drawCardBtn;
    @FXML private Button playCardBtn;
    @FXML private Button endTurnBtn;
    @FXML private Button newGameBtn;
    @FXML private Button achievementBtn;

    private static final double HAND_DOCK_HEIGHT = 266;
    private static final double HAND_DOCK_PEEK = 54;
    private static final int MAX_PLAYS_PER_TURN = 3;

    private NetworkClient client;
    private int localSeat = -1;
    private GameStateDto state;
    private List<Card> myHand = new ArrayList<>();
    private List<Card> myBank = new ArrayList<>();
    private Card selectedCard;
    private CardView selectedCardView;
    private int mergedLogSize;
    private boolean handDockExpanded = false;
    private Image avatarImage;

    private enum ActionPlayChoice {
        USE_EFFECT, DEPOSIT_BANK
    }

    @FXML
    public void initialize() {
        loadAvatarImage();
        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, oldW, newW) -> {
                if (state != null) {
                    updateHand();
                }
            });
        }
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
        if (playCardBtn != null) playCardBtn.setOnAction(e -> onPlay());
        if (endTurnBtn != null) endTurnBtn.setOnAction(e -> onEndTurn());
        if (achievementBtn != null) achievementBtn.setOnAction(e -> AchievementUi.showLibraryDialog(statusMessage));
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
        javafx.scene.Parent parent = allPlayersPropertiesPanel.getParent();
        if (parent instanceof ScrollPane scrollPane) {
            scrollPane.setFitToWidth(true);
            scrollPane.setPannable(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        }
        allPlayersPropertiesPanel.widthProperty().addListener((obs, oldW, newW) -> {
            double width = newW.doubleValue();
            int columns = width > 1060 ? 2 : 1;
            allPlayersPropertiesPanel.setPrefColumns(columns);
            allPlayersPropertiesPanel.setPrefTileWidth(Math.max(420, (width - 32) / columns));
        });
        allPlayersPropertiesPanel.setTileAlignment(Pos.TOP_LEFT);
        allPlayersPropertiesPanel.setSnapToPixel(true);
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
        Optional<ButtonType> choice = showStyledButtonDialog(
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
        Optional<Card> chosen = showStyledChoiceDialog(
                "Choose Payment Asset",
                "You must pay " + prompt.remainingDue + "M",
                prompt.actionName + "\nChoose one bank card or property to pay. Extra value is not returned.",
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
            return "-fx-border-color: " + cssColorFor(propertyCard.getColor()) + ";";
        }
        if (card instanceof model.card.MoneyCard) {
            return "-fx-border-color: #27ae60;";
        }
        return "-fx-border-color: #d64545;";
    }

    private void applyState(GameStateDto newState) {
        if (newState == null) {
            showStatus("等待服务器同步游戏状态…", false);
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
        this.state = newState;
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
        client.endTurn();
    }

    private void onPlay() {
        if (!isMyTurn()) {
            showStatus("It's not your turn", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("Select a card first", true);
            return;
        }
        playSelectedCard();
    }

    private void playSelectedCard() {
        Card card = selectedCard;
        selectedCard = null;
        selectedCardView = null;
        ClientMessage msg = new ClientMessage();
        msg.cardId = card.getInstanceId();

        if (card instanceof WildpropertyCard wild) {
            playWild(wild, msg);
            return;
        }
        if (card instanceof ActionCard action) {
            playAction(action, msg);
            return;
        }
        sendPlayCard(msg);
    }

    private void sendPlayCard(ClientMessage msg) {
        client.playCard(msg);
        AchievementUi.unlockAndShow(AchievementManager.FIRST_PLAY, statusMessage);
    }

    private void playWild(WildpropertyCard wild, ClientMessage msg) {
        if (wild.isBankable()) {
            Optional<ActionPlayChoice> choice = promptWildPropertyChoice(wild);
            if (choice.isEmpty()) {
                selectedCard = wild;
                return;
            }
            if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
                msg.mode = "BANK";
                sendPlayCard(msg);
                return;
            }
        }
        List<Color> colors = wild.getAvailableColors();
        if (colors.isEmpty()) {
            showStatus("No color available", true);
            selectedCard = wild;
            return;
        }
        Optional<Color> color = promptSelectWildColor(wild);
        if (color.isEmpty()) {
            selectedCard = wild;
            showStatus("Cancelled, wild card kept in hand", false);
            return;
        }
        msg.mode = "PROPERTY";
        msg.color = color.get().name();
        sendPlayCard(msg);
    }

    private void playAction(ActionCard action, ClientMessage msg) {
        Optional<ActionPlayChoice> choice = promptActionCardChoice(action);
        if (choice.isEmpty()) {
            selectedCard = action;
            return;
        }
        if (choice.get() == ActionPlayChoice.DEPOSIT_BANK) {
            msg.mode = "BANK";
            sendPlayCard(msg);
            return;
        }

        msg.mode = "EFFECT";
        if (!fillActionEffectMessage(action, msg)) {
            selectedCard = action;
            return;
        }
        sendPlayCard(msg);
    }

    private boolean fillActionEffectMessage(ActionCard action, ClientMessage msg) {
        if (action instanceof RentCard rentCard) {
            Optional<Color> color = promptRentColor(rentCard);
            if (color.isEmpty()) {
                return false;
            }
            msg.color = color.get().name();
            return true;
        }
        if (action instanceof DebtCollector) {
            Optional<Integer> target = promptOpponentSeat("Debt Collector: Select player to collect 5M from");
            if (target.isEmpty()) {
                return false;
            }
            msg.targetSeat = target.get();
            return true;
        }
        if (action instanceof House) {
            List<Color> options = getHouseEligibleColors();
            if (options.isEmpty()) {
                showStatus("No complete set available for a House", true);
                return false;
            }
            Optional<Color> color = showColorChoiceDialog(
                    "Select a complete set to add House",
                    "Select a complete set to add House",
                    "Select color set:", options);
            if (color.isEmpty()) {
                return false;
            }
            msg.color = color.get().name();
            return true;
        }
        if (action instanceof Hotel) {
            List<Color> options = getHotelEligibleColors();
            if (options.isEmpty()) {
                showStatus("Need a complete set with a House before adding a Hotel", true);
                return false;
            }
            Optional<Color> color = showColorChoiceDialog(
                    "Select a complete set to add Hotel",
                    "Select a complete set to add Hotel",
                    "Select color set:", options);
            if (color.isEmpty()) {
                return false;
            }
            msg.color = color.get().name();
            return true;
        }
        if (action instanceof SlyDeal) {
            Optional<Integer> target = promptOpponentSeat("Sly Deal: Select player to steal property from");
            if (target.isEmpty()) {
                return false;
            }
            List<PropertyCard> stealable = getStealablePropertiesForSeat(target.get());
            if (stealable.isEmpty()) {
                showStatus(playerNameAt(target.get()) + " has no stealable properties", true);
                return false;
            }
            Optional<PropertyCard> property = promptSelectProperty(
                    stealable,
                    "Select property to steal",
                    playerNameAt(target.get()) + "'s stealable properties (not in complete sets)");
            if (property.isEmpty()) {
                return false;
            }
            msg.targetSeat = target.get();
            msg.targetCardId = property.get().getInstanceId();
            return true;
        }
        if (action instanceof ForcedDeal) {
            Optional<Integer> target = promptOpponentSeat("Forced Deal: Select player to exchange properties with");
            if (target.isEmpty()) {
                return false;
            }
            List<PropertyCard> myProps = getPropertiesForSeat(localSeat);
            if (myProps.isEmpty()) {
                showStatus("You have no properties to exchange", true);
                return false;
            }
            List<PropertyCard> theirProps = getStealablePropertiesForSeat(target.get());
            if (theirProps.isEmpty()) {
                showStatus(playerNameAt(target.get()) + " has no exchangeable properties", true);
                return false;
            }
            Optional<PropertyCard> mine = promptSelectProperty(
                    myProps, "Select your property to exchange", "Your properties");
            if (mine.isEmpty()) {
                return false;
            }
            Optional<PropertyCard> theirs = promptSelectProperty(
                    theirProps,
                    "Select opponent's property to exchange",
                    playerNameAt(target.get()) + "'s exchangeable properties");
            if (theirs.isEmpty()) {
                return false;
            }
            msg.targetSeat = target.get();
            msg.targetCardId = mine.get().getInstanceId();
            msg.secondCardId = theirs.get().getInstanceId();
            return true;
        }
        if (action instanceof DealBreaker) {
            Optional<Integer> target = promptOpponentWithCompleteSets();
            if (target.isEmpty()) {
                showStatus("No player currently has a complete property set to steal", true);
                return false;
            }
            List<Color> completeSets = getCompleteSetColorsForSeat(target.get());
            Optional<Color> color = showColorChoiceDialog(
                    "Select Set",
                    playerNameAt(target.get()) + "'s complete sets",
                    "Which color to steal?", completeSets);
            if (color.isEmpty()) {
                return false;
            }
            msg.targetSeat = target.get();
            msg.color = color.get().name();
            return true;
        }
        if (action instanceof DoubleTheRent doubleRent) {
            return fillDoubleRentMessage(doubleRent, msg);
        }
        return true;
    }

    private boolean fillDoubleRentMessage(DoubleTheRent doubleRent, ClientMessage msg) {
        if (state == null || state.remainingPlays < 2) {
            showStatus("You need 2 plays remaining to use Double the Rent with a Rent card", true);
            return false;
        }
        List<RentCard> rentOptions = findPlayableRentCards(doubleRent);
        if (rentOptions.isEmpty()) {
            showStatus("No playable Rent card in your hand", true);
            return false;
        }
        Optional<RentCard> rentChoice = showStyledChoiceDialog(
                "Choose Rent Card",
                "Double the Rent",
                "Select a Rent card to play at double value (uses 2 plays):",
                rentOptions,
                rent -> rent.getName() + " (bank " + rent.getBankValueM() + "M)",
                rent -> null);
        if (rentChoice.isEmpty()) {
            return false;
        }
        Optional<Color> color = promptRentColor(rentChoice.get());
        if (color.isEmpty()) {
            return false;
        }
        msg.mode = "DOUBLE_RENT";
        msg.secondCardId = rentChoice.get().getInstanceId();
        msg.color = color.get().name();
        return true;
    }

    private List<RentCard> findPlayableRentCards(ActionCard excluding) {
        List<RentCard> options = new ArrayList<>();
        for (Card card : myHand) {
            if (card == excluding || card == null || !(card instanceof RentCard rent)) {
                continue;
            }
            if (rent.canPlay(playerViewFromSeat(localSeat))) {
                options.add(rent);
            }
        }
        return options;
    }

    private Optional<PropertyCard> promptSelectProperty(List<PropertyCard> properties,
                                                        String title, String header) {
        return showStyledChoiceDialog(title, header, "Select a property:", properties,
                p -> p.getName() + " (" + p.getColor() + ", " + p.getPrice() + "M)",
                p -> "-fx-border-color: " + cssColorFor(
                        p.getColor() == null ? Color.BROWN : p.getColor()) + ";");
    }

    private Optional<Integer> promptOpponentWithCompleteSets() {
        if (state == null) {
            return Optional.empty();
        }
        List<PlayerViewDto> valid = new ArrayList<>();
        for (PlayerViewDto p : state.players) {
            if (p.seat != localSeat && hasAnyCompleteSet(p.seat)) {
                valid.add(p);
            }
        }
        if (valid.isEmpty()) {
            return Optional.empty();
        }
        Optional<PlayerViewDto> picked = showStyledChoiceDialog(
                "Deal Breaker",
                "Select player to steal complete set from",
                "Only showing players with complete sets:",
                valid,
                p -> p.name,
                p -> null);
        return picked.map(p -> p.seat);
    }

    private List<PropertyCard> getPropertiesForSeat(int seat) {
        List<PropertyCard> props = new ArrayList<>();
        if (state == null) {
            return props;
        }
        for (PlayerViewDto p : state.players) {
            if (p.seat != seat) {
                continue;
            }
            for (var dto : p.properties) {
                Card card = CardMapper.fromDto(dto);
                if (card instanceof PropertyCard property) {
                    props.add(property);
                }
            }
            break;
        }
        return props;
    }

    private List<PropertyCard> getStealablePropertiesForSeat(int seat) {
        Player view = playerViewFromSeat(seat);
        return PropertyRules.getPropertiesOutsideCompleteSets(view);
    }

    private Player playerViewFromSeat(int seat) {
        Player view = new Player("view");
        for (PropertyCard property : getPropertiesForSeat(seat)) {
            view.addProperty(property);
        }
        return view;
    }

    private boolean hasAnyCompleteSet(int seat) {
        Player view = playerViewFromSeat(seat);
        for (Color color : Color.values()) {
            if (view.hasCompleteSet(color)) {
                return true;
            }
        }
        return false;
    }

    private List<Color> getCompleteSetColorsForSeat(int seat) {
        Player view = playerViewFromSeat(seat);
        List<Color> complete = new ArrayList<>();
        for (Color color : Color.values()) {
            if (view.hasCompleteSet(color)) {
                complete.add(color);
            }
        }
        return complete;
    }

    private List<Color> getHouseEligibleColors() {
        Player view = playerViewFromSeat(localSeat);
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (view.hasCompleteSet(color) && !hasImprovement(localSeat, "House+", color)) {
                options.add(color);
            }
        }
        return options;
    }

    private List<Color> getHotelEligibleColors() {
        Player view = playerViewFromSeat(localSeat);
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (view.hasCompleteSet(color)
                    && hasImprovement(localSeat, "House+", color)
                    && !hasImprovement(localSeat, "Hotel+", color)) {
                options.add(color);
            }
        }
        return options;
    }

    private boolean hasImprovement(int seat, String prefix, Color color) {
        return getPropertiesForSeat(seat).stream()
                .anyMatch(p -> (prefix + color).equals(p.getName()));
    }

    private String playerNameAt(int seat) {
        if (state == null || state.players == null) {
            return "Player";
        }
        for (PlayerViewDto p : state.players) {
            if (p.seat == seat) {
                return p.name;
            }
        }
        return "Player";
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

    private Optional<Color> promptRentColor(RentCard rentCard) {
        Map<Color, Integer> counts = propertyCountsForLocalSeat();
        List<Color> options = new ArrayList<>();
        if (rentCard.isAllColors()) {
            for (Color c : Color.values()) {
                if (counts.getOrDefault(c, 0) > 0) options.add(c);
            }
        } else {
            for (Color c : rentCard.getApplicableColors()) {
                if (counts.getOrDefault(c, 0) > 0) options.add(c);
            }
        }
        if (options.isEmpty()) {
            showStatus("No valid rent color", true);
            return Optional.empty();
        }
        if (options.size() == 1) {
            return Optional.of(options.get(0));
        }
        return showStyledChoiceDialog(
                "Select Rent Color",
                "Select which property set to collect rent from",
                "Color (count → rent):",
                options,
                color -> color + "  ·  " + counts.getOrDefault(color, 0) + " cards → "
                        + RentTable.getRent(color, counts.getOrDefault(color, 0)) + "M",
                color -> "-fx-background-color: " + cssColorFor(color) + "; -fx-text-fill: " + textColorFor(color) + ";");
    }

    private Optional<Integer> promptOpponentSeat(String title) {
        if (state == null) return Optional.empty();
        List<PlayerViewDto> opponents = new ArrayList<>();
        for (PlayerViewDto p : state.players) {
            if (p.seat != localSeat) {
                opponents.add(p);
            }
        }
        if (opponents.isEmpty()) return Optional.empty();
        Optional<PlayerViewDto> picked = showStyledChoiceDialog(
                title, title, "Select player:", opponents, p -> p.name, p -> null);
        return picked.map(p -> p.seat);
    }


    private Map<Color, Integer> propertyCountsForLocalSeat() {
        Map<Color, Integer> counts = new EnumMap<>(Color.class);
        if (state == null) return counts;
        for (PlayerViewDto p : state.players) {
            if (p.seat != localSeat) continue;
            for (var dto : p.properties) {
                Color c = CardMapper.parseColor(dto.color);
                if (c != null) counts.merge(c, 1, Integer::sum);
            }
        }
        return counts;
    }

    private boolean isMyTurn() {
        return state != null && !state.gameOver && state.currentPlayerIndex == localSeat;
    }

    private void updateUi() {
        if (state == null) return;
        updatePlayersList();
        updateAllPlayersProperties();
        updateHand();
        updateBank();
        updateLabels();
        updateButtons();
    }

    private void updatePlayersList() {
        if (playersList == null) return;
        playersList.getChildren().clear();
        for (PlayerViewDto p : state.players) {
            boolean current = p.seat == state.currentPlayerIndex;
            playersList.getChildren().add(createPlayerInfoBox(p, current));
        }
    }

    private VBox createPlayerInfoBox(PlayerViewDto player, boolean isCurrent) {
        VBox box = new VBox(7);
        box.getStyleClass().add("player-info-card");
        if (isCurrent) {
            box.getStyleClass().add("player-info-current");
        }

        HBox header = new HBox(9);
        header.setAlignment(Pos.CENTER_LEFT);
        ImageView avatar = createAvatarView(42);
        String displayName = player.name + (player.seat == localSeat ? " (You)" : "");
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        header.getChildren().addAll(avatar, nameLabel);

        Label handCountLabel = new Label("Hand: " + player.handSize + " cards");
        Label propertyCountLabel = new Label("Properties: " + player.properties.size());
        Label bankLabel = new Label("Bank: " + player.bankTotal + "M");

        box.getChildren().addAll(header, handCountLabel, propertyCountLabel, bankLabel);
        return box;
    }

    private void updateAllPlayersProperties() {
        if (allPlayersPropertiesPanel == null) return;
        allPlayersPropertiesPanel.getChildren().clear();

        if (state.players.isEmpty()) {
            Label empty = new Label("(No player properties yet)");
            empty.setStyle("-fx-text-fill: #7f8c8d;");
            allPlayersPropertiesPanel.getChildren().add(empty);
            return;
        }

        for (PlayerViewDto p : state.players) {
            VBox playerBlock = new VBox(8);
            playerBlock.setMaxWidth(Double.MAX_VALUE);
            boolean isTurn = p.seat == state.currentPlayerIndex;
            playerBlock.getStyleClass().add("player-public-block");
            if (isTurn) {
                playerBlock.getStyleClass().add("player-public-block-current");
            }

            HBox titleRow = new HBox(9);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            ImageView avatar = createAvatarView(38);
            Label title = new Label((isTurn ? "▶ " : "") + p.name
                    + "  |  Hand: " + p.handSize + " cards  |  Bank: " + p.bankTotal + "M");
            title.setStyle("-fx-font-weight: 900; -fx-font-size: 15px; -fx-text-fill: #103c2a;");
            titleRow.getChildren().addAll(avatar, title);

            FlowPane props = new FlowPane(13, 13);
            props.setPrefWrapLength(640);
            props.setMaxWidth(Double.MAX_VALUE);

            List<Card> properties = new ArrayList<>();
            for (var dto : p.properties) {
                Card card = CardMapper.fromDto(dto);
                if (card != null) {
                    properties.add(card);
                }
            }
            if (properties.isEmpty()) {
                props.getChildren().add(new Label("(No properties)"));
            } else {
                Map<Color, List<Card>> byColor = groupPropertiesByColor(properties);
                for (Map.Entry<Color, List<Card>> entry : byColor.entrySet()) {
                    props.getChildren().add(buildPropertyColorSet(entry.getKey(), entry.getValue()));
                }
            }
            playerBlock.getChildren().addAll(titleRow, props);
            allPlayersPropertiesPanel.getChildren().add(playerBlock);
        }
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

        Pane row = createOverlappedPropertyRow(cards);
        colorSet.getChildren().addAll(groupLabel, row);
        return colorSet;
    }

    private Pane createOverlappedPropertyRow(List<Card> cards) {
        Pane row = new Pane();
        double offset = 30;
        double fanDrop = 5;
        double width = CardView.PUBLIC.slotW() + Math.max(0, cards.size() - 1) * offset + 18;
        double height = CardView.PUBLIC.slotH() + fanDrop + 14;
        row.setMinSize(width, height);
        row.setPrefSize(width, height);
        row.setMaxSize(width, height);

        double middle = (cards.size() - 1) / 2.0;
        for (int i = 0; i < cards.size(); i++) {
            StackPane slot = CardView.wrapInSlot(cards.get(i), false, CardView.PUBLIC);
            slot.setLayoutX(i * offset);
            slot.setLayoutY(i % 2 == 0 ? 0 : fanDrop);
            slot.setRotate((i - middle) * 3.5);
            slot.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED, e -> slot.toFront());
            row.getChildren().add(slot);
        }
        return row;
    }

    private Map<Color, List<Card>> groupPropertiesByColor(List<Card> properties) {
        Map<Color, List<Card>> byColor = new LinkedHashMap<>();
        for (Card card : properties) {
            Color color = card.getColor() != null ? card.getColor() : Color.BROWN;
            byColor.computeIfAbsent(color, k -> new ArrayList<>()).add(card);
        }
        return byColor;
    }

    private void updateHand() {
        if (playerHand == null) return;
        playerHand.getChildren().clear();
        boolean clickable = isMyTurn() && state.remainingPlays > 0;
        CardView.CardMetrics metrics = computeHandMetrics(myHand.size());

        for (Card card : myHand) {
            if (card == null) {
                continue;
            }
            StackPane slot = CardView.wrapInSlot(card, clickable, metrics);
            CardView cv = CardView.getCardView(slot);
            if (selectedCard != null && selectedCard.equals(card) && cv != null) {
                cv.setSelected(true);
                selectedCardView = cv;
            }
            if (clickable && cv != null) {
                slot.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        selectCard(card, cv);
                        onPlay();
                    } else {
                        selectCard(card, cv);
                    }
                });
            }
            playerHand.getChildren().add(slot);
        }
    }

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

    private void selectCard(Card card, CardView cv) {
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
            showStatus("Selected: " + card.getName() + ", click 'Play' or double-click to play", false);
        }
        updateButtons();
    }

    private void updateBank() {
        if (playerBank == null) return;
        playerBank.getChildren().clear();
        int total = 0;
        if (state != null) {
            for (PlayerViewDto p : state.players) {
                if (p.seat == localSeat) total = p.bankTotal;
            }
        }
        if (bankTotalLabel != null) bankTotalLabel.setText(total + "M");
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

    private void updateLabels() {
        if (state == null || state.players == null || state.players.isEmpty()) {
            return;
        }
        int turnIndex = Math.min(Math.max(state.currentPlayerIndex, 0), state.players.size() - 1);
        PlayerViewDto current = state.players.get(turnIndex);
        if (currentPlayerLabel != null) {
            String drawStatus = state.hasDrawnThisTurn ? "Drew cards" : "Hasn't drawn";
            currentPlayerLabel.setText("Current Player: " + current.name
                    + " | " + drawStatus
                    + " | Remaining plays: " + state.remainingPlays + "/" + MAX_PLAYS_PER_TURN);
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
        if (allPlayersBankBar != null) {
            allPlayersBankBar.getChildren().clear();
            for (PlayerViewDto player : state.players) {
                boolean isCurrent = player.seat == state.currentPlayerIndex;
                String displayName = player.name + (player.seat == localSeat ? " (You)" : "");
                allPlayersBankBar.getChildren().add(createBankPill(displayName, player.bankTotal, isCurrent));
            }
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

    private void updateButtons() {
        if (state == null) return;
        boolean myTurn = isMyTurn();
        if (drawCardBtn != null) drawCardBtn.setDisable(!myTurn || state.hasDrawnThisTurn);
        if (playCardBtn != null) playCardBtn.setDisable(!myTurn || state.remainingPlays <= 0 || selectedCard == null);
        if (endTurnBtn != null) endTurnBtn.setDisable(!myTurn);
    }

    private void showStatus(String text, boolean error) {
        if (statusMessage == null) return;
        statusMessage.setText(text);
        statusMessage.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: white;");
    }

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
}
