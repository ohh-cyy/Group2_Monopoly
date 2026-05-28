package controller;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import engine.PropertyRules;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import sync.*;
import ui.CardView;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.io.IOException;
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
    private VBox allPlayersPropertiesPanel;

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
    private Timeline syncTimeline;
    private final List<String> roomLogLines = new ArrayList<>();
    private List<Card> viewHand = new ArrayList<>();
    private List<Card> viewBank = new ArrayList<>();
    private RoomPublicSnapshot remotePublic;
    private long lastSeenVersion = -1;
    
    @FXML
    public void initialize() {
        random = new Random();
        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, oldW, newW) -> {
                if (!viewHand.isEmpty() || currentPlayer != null) {
                    updatePlayerHand();
                }
            });
        }
        setupButtonActions();
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
        startSyncTimer();
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
        startSyncTimer();
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
        logMessage("Players: " + String.join(", ", names));
        updateUI();
    }
    
    @FXML
    private void onDrawCardsClick() {
        if (!isMyActionTurn()) {
            showStatus("还没轮到你", true);
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
            showStatus("本回合已经抽过牌了，不能再抽！", true);
            return;
        }

        Player player = gameEngine.getCurrentPlayer();
        if (!gameEngine.drawCardsForCurrentPlayer()) {
            showStatus("抽牌失败", true);
            return;
        }

        logMessage(player.getName() + " 抽了 2 张牌");
        showStatus("已抽 2 张牌（本回合不能再抽）。剩余可出牌次数: "
                + gameEngine.getRemainingPlays(), false);
        afterStateChange();
    }
    
    @FXML
    private void onPlayCardClick() {
        if (!isMyActionTurn()) {
            showStatus("还没轮到你", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("请先点击手牌选中一张牌，再点「出牌」", true);
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
            showStatus("本回合已打出 3 张牌，不能再出牌！", true);
            return;
        }

        if (selectedCard == null) {
            showStatus("请先点击手牌选中一张牌，再点「出牌」", true);
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
            logMessage(player.getName() + " 将「" + actionCard.getName()
                    + "」存入银行（" + actionCard.getBankValueM() + "M）");
            showStatus("已存入银行 " + actionCard.getBankValueM() + "M", false);
            completePlayStep(player, actionCard, true);
            return;
        }

        ActionEffectResult result = resolveActionCardEffect(player, actionCard);
        if (result == ActionEffectResult.CANCELLED) {
            selectedCard = actionCard;
            showStatus("已取消出牌，行动牌保留在手牌", false);
            updateUI();
            return;
        }

        player.removeFromHand(actionCard);
        gameEngine.getDiscardPile().addCard(actionCard);
        if (result == ActionEffectResult.SUCCESS) {
            logMessage(player.getName() + " 使用「" + actionCard.getName() + "」效果");
            showStatus("已使用效果: " + actionCard.getName(), false);
        } else {
            logMessage(player.getName() + " 使用「" + actionCard.getName() + "」失败，牌进入弃牌堆");
            showStatus("效果未能生效（目标无效等）", true);
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
            logMessage(player.getName() + " 已打出 3 张牌，回合结束");
            forceEndTurn();
            return;
        }

        if (!depositedToBank) {
            showStatus("已打出。本回合还可出 " + gameEngine.getRemainingPlays() + " 张牌", false);
        } else {
            showStatus("已存入银行。本回合还可出 " + gameEngine.getRemainingPlays() + " 张牌", false);
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("行动牌");
        alert.setHeaderText(card.getName() + " — 银行面值 " + card.getBankValueM() + "M");
        alert.setContentText(card.getDescription() + "\n\n选择「使用效果」或「存入银行」？");
        ButtonType useBtn = new ButtonType("使用效果");
        ButtonType bankBtn = new ButtonType("存入银行 (" + card.getBankValueM() + "M)");
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(useBtn, bankBtn, cancelBtn);

        Optional<ButtonType> result = alert.showAndWait();
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
            showStatus("Just Say No 只能在对手对你打出行动牌时响应使用", true);
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
            Optional<Color> color = promptSelectOwnCompleteSet(player, "选择要加盖 House 的完整套组");
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
            Optional<Color> color = promptSelectOwnCompleteSet(player, "选择要加盖 Hotel 的完整套组");
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
            showStatus("你没有适用颜色的地产，无法使用此租金卡", true);
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
            showStatus("该颜色下没有地产，租金为 0", true);
            return ActionEffectResult.FAILED;
        }

        boolean doubled = gameEngine.isRentDoubled();
        int total = rentCard.collectFromAll(player, gameEngine, chargeColor, rent);

        String rentNote = doubled ? "（双倍租金）" : "";
        logMessage(player.getName() + " 使用「" + rentCard.getName() + "」对 " + chargeColor
                + " 收租 " + (doubled ? rent * 2 : rent) + "M/人" + rentNote + "，共收到 " + total + "M");
        showStatus("已向所有玩家收取 " + chargeColor + " 租金" + rentNote + "（合计 " + total + "M）", false);
        return ActionEffectResult.SUCCESS;
    }

    private ActionEffectResult resolveDebtCollector(Player player, DebtCollector debtCollector) {
        Optional<Player> target = promptSelectOpponent(player, "Debt Collector：选择要收取 5M 的玩家");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (tryRespondWithJustSayNo(target.get(), player, "Debt Collector（收取 5M）")) {
            return ActionEffectResult.CANCELLED;
        }
        int paid = debtCollector.collectFrom(player, target.get());
        logMessage(player.getName() + " 从 " + target.get().getName() + " 收到 " + paid + "M");
        return paid > 0 ? ActionEffectResult.SUCCESS : ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveMyBirthday(Player player, MyBirthday myBirthday) {
        int total = myBirthday.collectFromEveryone(player, gameEngine);
        logMessage(player.getName() + " 过生日，共收到 " + total + "M（每人应付 "
                + MyBirthday.GIFT_AMOUNT + "M，不足可用地产抵）");
        showStatus("所有玩家共支付 " + total + "M", false);
        return total > 0 ? ActionEffectResult.SUCCESS : ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveDoubleTheRent(DoubleTheRent doubleRent) {
        if (!doubleRent.activateForNextRent(gameEngine)) {
            showStatus("本回合已激活双倍租金，请先打出一张 Rent 牌", true);
            return ActionEffectResult.FAILED;
        }
        showStatus("已激活双倍租金：请在本回合再打出一张 Rent 牌", false);
        logMessage("双倍租金已激活，下一张 Rent 将双倍收租");
        return ActionEffectResult.SUCCESS;
    }

    private ActionEffectResult resolveSlyDeal(Player player, SlyDeal slyDeal) {
        Optional<Player> target = promptSelectOpponent(player, "Sly Deal：选择要偷取地产的玩家");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (tryRespondWithJustSayNo(target.get(), player, "Sly Deal（偷取一张地产）")) {
            return ActionEffectResult.CANCELLED;
        }
        List<PropertyCard> stealable = PropertyRules.getPropertiesOutsideCompleteSets(target.get());
        if (stealable.isEmpty()) {
            showStatus(target.get().getName() + " 没有可偷的地产（完整套组中的牌不能偷）", true);
            return ActionEffectResult.FAILED;
        }
        Optional<PropertyCard> property = promptSelectProperty(stealable,
                "选择要偷的地产", target.get().getName() + " 的可偷地产（非完整套组）");
        if (property.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (slyDeal.stealProperty(player, target.get(), property.get())) {
            logMessage(player.getName() + " 从 " + target.get().getName()
                    + " 偷走 " + property.get().getName());
            return ActionEffectResult.SUCCESS;
        }
        return ActionEffectResult.FAILED;
    }

    private ActionEffectResult resolveForcedDeal(Player player, ForcedDeal forcedDeal) {
        Optional<Player> target = promptSelectOpponent(player, "Forced Deal：选择交换地产的玩家");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (tryRespondWithJustSayNo(target.get(), player, "Forced Deal（交换地产）")) {
            return ActionEffectResult.CANCELLED;
        }
        List<PropertyCard> myProps = player.getAllProperties();
        if (myProps.isEmpty()) {
            showStatus("你没有地产可交换", true);
            return ActionEffectResult.FAILED;
        }
        List<PropertyCard> theirSwappable = PropertyRules.getPropertiesOutsideCompleteSets(target.get());
        if (theirSwappable.isEmpty()) {
            showStatus(target.get().getName() + " 没有可交换的地产（完整套组中的不能换）", true);
            return ActionEffectResult.FAILED;
        }
        Optional<PropertyCard> mine = promptSelectProperty(myProps,
                "选择你交出的地产", "你的地产");
        if (mine.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        Optional<PropertyCard> theirs = promptSelectProperty(theirSwappable,
                "选择对方交出的地产", target.get().getName() + " 可交换的地产（不能是完整套组里的牌）");
        if (theirs.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (forcedDeal.swapProperties(player, mine.get(), target.get(), theirs.get())) {
            logMessage(player.getName() + " 与 " + target.get().getName() + " 交换地产："
                    + mine.get().getName() + " ↔ " + theirs.get().getName());
            return ActionEffectResult.SUCCESS;
        }
        return ActionEffectResult.FAILED;
    }

    /**
     * 若防守方手牌有 Just Say No 且选择打出，则取消本次行动效果。
     */
    private boolean tryRespondWithJustSayNo(Player defender, Player attacker, String actionName) {
        JustSayNo justSayNo = findJustSayNoInHand(defender);
        if (justSayNo == null) {
            return false;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Just Say No");
        alert.setHeaderText(defender.getName() + "：是否拒绝 " + attacker.getName() + " 的行动？");
        alert.setContentText(actionName + "\n\n选择「拒绝」将打出 Just Say No 并取消该效果。");
        ButtonType noBtn = new ButtonType("拒绝 (Just Say No)");
        ButtonType allowBtn = new ButtonType("允许生效");
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(noBtn, allowBtn, cancelBtn);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == cancelBtn) {
            return false;
        }
        if (choice.get() == allowBtn) {
            return false;
        }

        defender.removeFromHand(justSayNo);
        gameEngine.getDiscardPile().addCard(justSayNo);
        logMessage(defender.getName() + " 打出 Just Say No，取消 " + attacker.getName() + " 的「" + actionName + "」");
        showStatus(defender.getName() + " 使用 Just Say No 拒绝了该行动", false);
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
        if (properties.isEmpty()) {
            return Optional.empty();
        }
        List<String> labels = new ArrayList<>();
        for (PropertyCard p : properties) {
            labels.add(p.getName() + " (" + p.getColor() + ", " + p.getPrice() + "M)");
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("选择一张地产：");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        int idx = labels.indexOf(result.get());
        return Optional.of(properties.get(idx));
    }

    private Optional<Color> promptSelectRentColor(Player player, RentCard rentCard, List<Color> options) {
        List<String> labels = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        for (Color color : options) {
            int count = rentCard.countProperties(player, color);
            int rent = rentCard.calculateRent(player, color);
            labels.add(color + " (" + count + " 张 → " + rent + "M)");
            colors.add(color);
        }
        if (colors.isEmpty()) {
            return Optional.empty();
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels.get(0), labels);
        dialog.setTitle("选择收租颜色");
        dialog.setHeaderText("选择要按哪套地产收租");
        dialog.setContentText("颜色（张数 → 租金）:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        int index = labels.indexOf(result.get());
        return Optional.of(colors.get(index));
    }

    private ActionEffectResult resolveDealBreaker(Player player, DealBreaker dealBreaker) {
        Optional<Player> target = promptSelectOpponentWithCompleteSets(player);
        if (target.isEmpty()) {
            showStatus("当前没有玩家拥有可偷的完整地产套组", true);
            return ActionEffectResult.CANCELLED;
        }
        Player opponent = target.get();
        Optional<Color> color = promptSelectCompleteSetOnPlayer(opponent);
        if (color.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        if (tryRespondWithJustSayNo(opponent, player, "Deal Breaker（偷取完整套组）")) {
            return ActionEffectResult.CANCELLED;
        }
        if (!dealBreaker.useOnTarget(player, opponent, color.get())) {
            showStatus("偷取失败", true);
            return ActionEffectResult.FAILED;
        }
        logMessage(player.getName() + " 从 " + opponent.getName() + " 偷走整套 " + color.get());
        return ActionEffectResult.SUCCESS;
    }

    private Optional<Player> promptSelectOpponentWithCompleteSets(Player current) {
        List<Player> valid = new ArrayList<>();
        for (Player p : gameEngine.getPlayers()) {
            if (!p.equals(current) && hasAnyCompleteSet(p)) {
                valid.add(p);
            }
        }
        if (valid.isEmpty()) {
            return Optional.empty();
        }
        ChoiceDialog<Player> dialog = new ChoiceDialog<>(valid.get(0), valid);
        dialog.setTitle("Deal Breaker");
        dialog.setHeaderText("选择要偷取完整套组的玩家");
        dialog.setContentText("仅显示拥有完整套组的玩家:");
        return dialog.showAndWait();
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
                logMessage(player.getName() + " 将万能地产「" + wild.getName()
                        + "」存入银行（" + wild.getBankValueM() + "M）");
                showStatus("万能卡已存入银行 " + wild.getBankValueM() + "M", false);
                completePlayStep(player, wild, true);
                return;
            }
        }

        Optional<Color> color = promptSelectWildColor(wild);
        if (color.isEmpty()) {
            selectedCard = wild;
            showStatus("已取消，万能卡保留在手牌", false);
            updateUI();
            return;
        }

        wild.setChosenColor(color.get());
        player.removeFromHand(wild);
        wild.use(player, gameEngine);
        logMessage(player.getName() + " 打出万能地产「" + wild.getName() + "」作为 " + color.get());
        showStatus("万能地产已作为 " + color.get() + " 放入地产区", false);
        completePlayStep(player, wild, false);
    }

    private Optional<ActionPlayChoice> promptWildPropertyChoice(WildpropertyCard wild) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("万能地产卡");
        alert.setHeaderText(wild.getName() + " — 可存银行 " + wild.getBankValueM() + "M");
        alert.setContentText("选择作为地产打出（并选颜色），或存入银行？");
        ButtonType useBtn = new ButtonType("作为地产打出");
        ButtonType bankBtn = new ButtonType("存入银行 (" + wild.getBankValueM() + "M)");
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(useBtn, bankBtn, cancelBtn);
        Optional<ButtonType> result = alert.showAndWait();
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
        ChoiceDialog<Color> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("万能地产颜色");
        dialog.setHeaderText(wild.getName());
        dialog.setContentText("选择要当作哪种颜色的地产：");
        return dialog.showAndWait();
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
        if (opponents.isEmpty()) {
            return Optional.empty();
        }
        ChoiceDialog<Player> dialog = new ChoiceDialog<>(opponents.get(0), opponents);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText("选择玩家:");
        return dialog.showAndWait();
    }

    private Optional<Color> promptSelectCompleteSetOnPlayer(Player target) {
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (target.hasCompleteSet(color)) {
                options.add(color);
            }
        }
        if (options.isEmpty()) {
            return Optional.empty();
        }
        ChoiceDialog<Color> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("选择套组");
        dialog.setHeaderText(target.getName() + " 的完整套组");
        dialog.setContentText("要偷取哪种颜色？");
        return dialog.showAndWait();
    }

    private Optional<Color> promptSelectOwnCompleteSet(Player player, String title) {
        List<Color> options = new ArrayList<>();
        for (Color color : Color.values()) {
            if (player.hasCompleteSet(color)) {
                options.add(color);
            }
        }
        if (options.isEmpty()) {
            return Optional.empty();
        }
        ChoiceDialog<Color> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText("选择颜色套组:");
        return dialog.showAndWait();
    }

    /** 出满 3 张牌后强制切换到下一名玩家 */
    private void forceEndTurn() {
        Player ending = gameEngine.getCurrentPlayer();
        gameEngine.nextTurn();
        currentPlayer = gameEngine.getCurrentPlayer();
        logMessage(ending.getName() + " 回合结束 → " + currentPlayer.getName() + " 的回合");
        showStatus("已出 3 张牌，自动换到 " + currentPlayer.getName()
                + "。请先抽 2 张牌再出牌", false);
        afterStateChange();
    }
    
    @FXML
    private void onEndTurnClick() {
        if (!isMyActionTurn()) {
            showStatus("还没轮到你", true);
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
        logMessage(player.getName() + " 主动结束回合");
        gameEngine.nextTurn();
        currentPlayer = gameEngine.getCurrentPlayer();
        showStatus("轮到 " + currentPlayer.getName() + "，请先抽 2 张牌", false);
        afterStateChange();
    }
    
    @FXML
    private void onNewGameClick() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("New Game");
        alert.setHeaderText("Start a new game?");
        alert.setContentText("This will reset the current game.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
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
        VBox box = new VBox(5);
        String borderColor = isCurrent ? "#f39c12" : "#bdc3c7";
        box.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5;"
                + " -fx-border-color: " + borderColor + "; -fx-border-width: " + (isCurrent ? "2" : "1") + ";");

        Label nameLabel = new Label(player.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        int handSize = sessionMode == SessionMode.CLIENT && remotePublic != null
                ? handSizeFor(player.getName()) : player.getHand().size();

        Label handCountLabel = new Label("Hand: " + handSize + " cards");
        Label propertyCountLabel = new Label("Properties: " + propertyCountFor(player));
        Label bankLabel = new Label("Bank: " + bankTotalFor(player) + "M");

        box.getChildren().addAll(nameLabel, handCountLabel, propertyCountLabel, bankLabel);
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
            String drawStatus = remotePublic.hasDrawnThisTurn ? "已抽牌" : "未抽牌";
            currentPlayerLabel.setText("Current Player: " + name
                    + " | " + drawStatus
                    + " | 剩余出牌: " + remotePublic.remainingPlays + "/3"
                    + (remotePublic.gameOver ? " | 结束" : ""));
            return;
        }
        if (currentPlayer == null || gameEngine == null) {
            return;
        }
        String drawStatus = gameEngine.hasDrawnThisTurn() ? "已抽牌" : "未抽牌";
        currentPlayerLabel.setText("Current Player: " + currentPlayer.getName()
                + " | " + drawStatus
                + " | 剩余出牌: " + gameEngine.getRemainingPlays() + "/"
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

    /** 手牌单行显示：宽度不够时按比例缩小牌面（含悬停尺寸） */
    private CardView.CardMetrics computeHandMetrics(int cardCount) {
        if (cardCount <= 0) {
            return CardView.HAND;
        }
        double available = 750;
        if (playerHandScroll != null && playerHandScroll.getViewportBounds().getWidth() > 0) {
            available = playerHandScroll.getViewportBounds().getWidth() - 20;
        }
        double gap = 6;
        double total = cardCount * CardView.HAND.slotW() + (cardCount - 1) * gap;
        if (total <= available) {
            return CardView.HAND;
        }
        double factor = Math.max(0.42, available / total);
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
            showStatus("已选中万能地产「" + card.getName() + "」"
                    + (wild.isBankable() ? "（可存银行 " + wild.getBankValueM() + "M）" : "（不可存银行）")
                    + "。出牌时选择颜色或存银行", false);
        } else if (card instanceof RentCard rentCard) {
            showStatus("已选中租金卡「" + card.getName() + "」（银行 " + rentCard.getBankValueM()
                    + "M）。出牌时可收租或存入银行", false);
        } else if (card instanceof ActionCard actionCard) {
            showStatus("已选中行动牌「" + card.getName() + "」（银行 " + actionCard.getBankValueM()
                    + "M）。出牌时可选择：使用效果 或 存入银行", false);
        } else {
            showStatus("已选中: " + card.getName() + "，可点击「出牌」或双击该牌直接打出", false);
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

        for (Card card : getBankCardsForView()) {
            playerBank.getChildren().add(CardView.wrapInSlot(card, false, CardView.COMPACT));
        }

        if (playerBank.getChildren().isEmpty()) {
            Label hint = new Label("（打出金钱牌会放入银行）");
            hint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
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
            Label empty = new Label("（暂无玩家地产）");
            empty.setStyle("-fx-text-fill: #7f8c8d;");
            allPlayersPropertiesPanel.getChildren().add(empty);
            return;
        }

        for (PlayerPublicSnapshot view : views) {
            VBox playerBlock = new VBox(6);
            boolean isTurn = isTurnSeat(view.seat);
            playerBlock.setStyle("-fx-background-color: white; -fx-padding: 8; -fx-background-radius: 6;"
                    + " -fx-border-color: " + (isTurn ? "#f39c12" : "#dcdde1") + "; -fx-border-width: "
                    + (isTurn ? "2" : "1") + ";");

            Label title = new Label(view.name + "  |  手牌 " + view.handSize + " 张");
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            FlowPane props = new FlowPane(8, 8);
            props.setPrefWrapLength(900);
            if (view.properties.isEmpty()) {
                props.getChildren().add(new Label("（无地产）"));
            } else {
                Map<Color, List<Card>> byColor = groupPropertiesByColor(
                        CardSnapshotMapper.fromSnapshots(view.properties));
                for (Map.Entry<Color, List<Card>> entry : byColor.entrySet()) {
                    HBox set = buildPropertyColorSet(entry.getKey(), entry.getValue());
                    props.getChildren().add(set);
                }
            }
            playerBlock.getChildren().addAll(title, props);
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
        HBox colorSet = new HBox(5);
        colorSet.setAlignment(Pos.CENTER_LEFT);
        colorSet.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 4; -fx-padding: 4 6;");
        Label groupLabel = new Label(color + "\n" + cards.size() + "/" + color.getSetSize());
        groupLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px;");
        groupLabel.setMinWidth(48);
        HBox row = new HBox(4);
        for (Card card : cards) {
            row.getChildren().add(CardView.wrapInSlot(card, false, CardView.COMPACT));
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText("Congratulations!");
            alert.setContentText(winner.getName() + " wins the game!");
            alert.showAndWait();
            
            gameStatusText.setText("Game Over - " + winner.getName() + " Wins!");
            logMessage("=== GAME OVER === " + winner.getName() + " wins!");
            
            // 禁用所有按钮
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

    private void startSyncTimer() {
        stopSync();
        syncTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> syncTick()));
        syncTimeline.setCycleCount(Timeline.INDEFINITE);
        syncTimeline.play();
    }

    private void stopSync() {
        if (syncTimeline != null) {
            syncTimeline.stop();
            syncTimeline = null;
        }
    }

    private void syncTick() {
        if (roomFolder == null) {
            return;
        }
        try {
            if (sessionMode == SessionMode.HOST) {
                for (RoomCommand cmd : RoomStorage.drainCommands(roomFolder)) {
                    handleRoomCommand(cmd);
                }
                publishRoomState();
            } else if (sessionMode == SessionMode.CLIENT) {
                pullRemoteStateQuiet();
            }
        } catch (Exception ex) {
            showStatus("同步失败: " + ex.getMessage(), true);
        }
    }

    private void publishRoomState() {
        if (gameEngine == null || roomFolder == null) {
            return;
        }
        try {
            roomLogLines.addAll(collectNewLogLines());
            RoomPublicSnapshot pub = RoomSnapshotBuilder.buildPublic(gameEngine, roomLogLines);
            RoomStorage.writeSnapshots(roomFolder, pub, RoomSnapshotBuilder.buildAllPrivate(gameEngine));
            lastSeenVersion = pub.version;
        } catch (Exception ex) {
            showStatus("保存房间状态失败: " + ex.getMessage(), true);
        }
    }

    private List<String> collectNewLogLines() {
        return new ArrayList<>(roomLogLines);
    }

    private void pullRemoteStateQuiet() {
        try {
            long version = RoomStorage.readVersion(roomFolder);
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
            showStatus("已提交操作，等待同步…", false);
        } catch (IOException e) {
            showStatus("提交失败: " + e.getMessage(), true);
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
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setHeaderText(wild.getName());
                ButtonType asProp = new ButtonType("作为地产");
                ButtonType asBank = new ButtonType("存银行");
                ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(asProp, asBank, cancel);
                Optional<ButtonType> r = alert.showAndWait();
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
                ChoiceDialog<Color> d = new ChoiceDialog<>(colors.get(0), colors);
                Optional<Color> c = d.showAndWait();
                if (c.isPresent()) {
                    submitRoomCommand("PLAY", wild.getInstanceId(), "PROPERTY", c.get().name());
                } else {
                    selectedCard = wild;
                }
            }
            return;
        }
        if (card instanceof ActionCard || card instanceof RentCard) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText(card.getName());
            alert.setContentText("伪联机：行动/租金牌请先存入银行");
            ButtonType bank = new ButtonType("存银行");
            ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(bank, cancel);
            Optional<ButtonType> r = alert.showAndWait();
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
