package controller;

import engine.Deck;
import engine.GameEngine;
import engine.PropertyRules;
import javafx.application.Platform;
import ui.CardView;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import model.card.*;
import model.card.actionCard.*;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;

import java.util.*;

public class GameController {
    @FXML
    private Label currentPlayerLabel;
    
    @FXML
    private Label gameStatusText;
    
    @FXML
    private Label drawPileCount;
    
    @FXML
    private Label discardPileCount;
    
    @FXML
    private Label statusMessage;
    
    @FXML
    private VBox playersList;
    
    @FXML
    private VBox opponentsHands;
    
    @FXML
    private FlowPane playerHand;
    
    @FXML
    private VBox playerProperties;

    @FXML
    private FlowPane playerBank;

    @FXML
    private Label bankTotalLabel;
    
    @FXML
    private StackPane drawPile;
    
    @FXML
    private StackPane discardPile;
    
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
    
    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 168;

    private GameEngine gameEngine;
    private Deck deck;
    private List<Player> players;
    private Player currentPlayer;
    private Card selectedCard;
    private Random random;
    
    @FXML
    public void initialize() {
        random = new Random();
        initializeGame();
    }
    
    private void initializeGame() {
        logMessage("=== Starting New Game ===");
        
        // 创建玩家
        players = new ArrayList<>();
        players.add(new Player("Player 1"));
        players.add(new Player("Player 2"));
        players.add(new Player("Player 3"));
        players.add(new Player("Player 4"));
        
        // 创建卡牌
        List<Card> cardList = createCardDeck();
        
        // 创建牌堆
        deck = new Deck(cardList);
        
        // 创建游戏引擎
        gameEngine = new GameEngine(players, deck);
        
        // 开始游戏（发初始牌）
        gameEngine.startGame();
        
        currentPlayer = gameEngine.getCurrentPlayer();
        
        logMessage("Game initialized with " + players.size() + " players");
        updateUI();
    }
    
    private List<Card> createCardDeck() {
        List<Card> cards = new ArrayList<>();
        
        // 地产卡 - 棕色
        cards.add(new PropertyCard("Old Kent Road", "Brown property worth 1M", model.enums.Color.BROWN, 1));
        cards.add(new PropertyCard("Whitechapel", "Brown property worth 1M", model.enums.Color.BROWN, 1));
        
        // 地产卡 - 浅蓝色
        cards.add(new PropertyCard("The Angel Islington", "Light Blue property worth 1M", model.enums.Color.LIGHT_BLUE, 1));
        cards.add(new PropertyCard("Euston Road", "Light Blue property worth 1M", model.enums.Color.LIGHT_BLUE, 1));
        cards.add(new PropertyCard("Pentonville Road", "Light Blue property worth 2M", model.enums.Color.LIGHT_BLUE, 2));
        
        // 地产卡 - 粉色
        cards.add(new PropertyCard("Pall Mall", "Pink property worth 2M", model.enums.Color.PINK, 2));
        cards.add(new PropertyCard("Whitehall", "Pink property worth 2M", model.enums.Color.PINK, 2));
        cards.add(new PropertyCard("Northumberland Avenue", "Pink property worth 2M", model.enums.Color.PINK, 2));
        
        // 地产卡 - 橙色
        cards.add(new PropertyCard("Bow Street", "Orange property worth 2M", model.enums.Color.ORANGE, 2));
        cards.add(new PropertyCard("Marlborough Street", "Orange property worth 2M", model.enums.Color.ORANGE, 2));
        cards.add(new PropertyCard("Vine Street", "Orange property worth 3M", model.enums.Color.ORANGE, 3));
        
        // 地产卡 - 红色
        cards.add(new PropertyCard("Strand", "Red property worth 3M", model.enums.Color.RED, 3));
        cards.add(new PropertyCard("Fleet Street", "Red property worth 3M", model.enums.Color.RED, 3));
        cards.add(new PropertyCard("Trafalgar Square", "Red property worth 3M", model.enums.Color.RED, 3));
        
        // 地产卡 - 黄色
        cards.add(new PropertyCard("Leicester Square", "Yellow property worth 3M", model.enums.Color.YELLOW, 3));
        cards.add(new PropertyCard("Coventry Street", "Yellow property worth 3M", model.enums.Color.YELLOW, 3));
        cards.add(new PropertyCard("Piccadilly", "Yellow property worth 4M", model.enums.Color.YELLOW, 4));
        
        // 地产卡 - 绿色
        cards.add(new PropertyCard("Regent Street", "Green property worth 4M", model.enums.Color.GREEN, 4));
        cards.add(new PropertyCard("Oxford Street", "Green property worth 4M", model.enums.Color.GREEN, 4));
        cards.add(new PropertyCard("Bond Street", "Green property worth 4M", model.enums.Color.GREEN, 4));
        
        // 地产卡 - 深蓝色
        cards.add(new PropertyCard("Park Lane", "Dark Blue property worth 4M", model.enums.Color.DARK_BLUE, 4));
        cards.add(new PropertyCard("Mayfair", "Dark Blue property worth 4M", model.enums.Color.DARK_BLUE, 4));
        
        // 金钱卡
        for (int i = 0; i < 6; i++) {
            cards.add(new MoneyCard("1M Banknote", "Worth 1 million", 1));
        }
        for (int i = 0; i < 5; i++) {
            cards.add(new MoneyCard("2M Banknote", "Worth 2 million", 2));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new MoneyCard("3M Banknote", "Worth 3 million", 3));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new MoneyCard("4M Banknote", "Worth 4 million", 4));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new MoneyCard("5M Banknote", "Worth 5 million", 5));
        }
        cards.add(new MoneyCard("10M Banknote", "Worth 10 million", 10));
        
        // 行动卡
        for (int i = 0; i < 10; i++) {
            cards.add(new PassGoCard("Pass Go","Draw extra two card", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new MyBirthday("My Birthday", "Everyone pays you 2M (property if short)", CardType.ACTION));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new DoubleTheRent("Double The Rent", "Next Rent card charges double", CardType.ACTION));
        }
        for(int i = 0; i < 2; i++) {
            cards.add(new DealBreaker("Deal Breaker", "Steal a complete property set from any player", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new JustSayNo("Just Say No", "Cancel an action played against you", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new SlyDeal("Sly Deal", "Steal one property (not from a complete set)", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new ForcedDeal("Forced Deal", "Swap one property with a player (not from their complete set)", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new DebtCollector("Debt Collector", "Collect 5M from any player", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new Hotel("Hotel", "Add hotel to a complete property set", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new House("House", "Add house to a complete property set", CardType.ACTION));
        }

        //property wild card
        cards.add(new WildpropertyCard("Dark Blue/Green","Wild property",4,List.of(Color.DARK_BLUE,Color.GREEN),true));
        cards.add(new WildpropertyCard("Light Blue/Green","Wild property",1,List.of(Color.LIGHT_BLUE,Color.BROWN),true));
        for(int i = 0; i < 2; i++) {
            cards.add(new WildpropertyCard("All Color","Wild property",0,List.of(Color.values()),false));
        }
        for(int i = 0; i < 2; i++) {
            cards.add(new WildpropertyCard("Orange/Pink","Wild property",2,List.of(Color.ORANGE,Color.PINK),true));
        }
        cards.add(new WildpropertyCard("Green/Black","Wild property",4,List.of(Color.GREEN,Color.BLACK),true));
        cards.add(new WildpropertyCard("Light_Blue/Black","Wild property",4,List.of(Color.LIGHT_BLUE,Color.BLACK),true));
        cards.add(new WildpropertyCard("Light_Green/Black","Wild property",2,List.of(Color.LIGHT_GREEN,Color.BLACK),true));
        for (int i = 0; i < 2; i++) {
            cards.add(new WildpropertyCard("Yellow/Red","Wild property",3,List.of(Color.YELLOW,Color.RED),true));
        }

        // 租金卡
        for (int i = 0; i < 3; i++) {
            cards.add(RentCard.allColors());
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.DARK_BLUE, Color.GREEN));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.BROWN, Color.LIGHT_BLUE));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.PINK, Color.ORANGE));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.BLACK, Color.LIGHT_GREEN));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(RentCard.dual(Color.RED, Color.YELLOW));
        }

        return cards;
    }
    
    @FXML
    private void onDrawPileClick(MouseEvent event) {
        if (!isCurrentPlayerTurn()) {
            showStatus("Not your turn!", true);
            return;
        }
        
        drawCardsFromPile();
    }
    
    @FXML
    private void onDrawCardsClick() {
        if (!isCurrentPlayerTurn()) {
            showStatus("Not your turn!", true);
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
        updateUI();
    }
    
    @FXML
    private void onPlayCardClick() {
        if (!isCurrentPlayerTurn()) {
            showStatus("Not your turn!", true);
            return;
        }
        
        if (selectedCard == null) {
            showStatus("请先点击手牌选中一张牌，再点「出牌」", true);
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
        updateUI();
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
        updateUI();
    }
    
    @FXML
    private void onEndTurnClick() {
        if (!isCurrentPlayerTurn()) {
            showStatus("Not your turn!", true);
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
        updateUI();
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
    
    private boolean isCurrentPlayerTurn() {
        return currentPlayer != null && 
               gameEngine != null && 
               !gameEngine.isGameOver();
    }
    
    private void updateUI() {
        Platform.runLater(() -> {
            if (gameEngine != null) {
                currentPlayer = gameEngine.getCurrentPlayer();
            }
            updatePlayerInfo();
            updateCurrentPlayerDisplay();
            updatePlayerHand();
            updatePlayerBank();
            updatePlayerProperties();
            updateOpponentsHands();
            updatePileCounts();
            updateButtonStates();
        });
    }
    
    private void updatePlayerInfo() {
        playersList.getChildren().clear();
        
        for (Player player : players) {
            VBox playerBox = createPlayerInfoBox(player);
            playersList.getChildren().add(playerBox);
        }
    }
    
    private VBox createPlayerInfoBox(Player player) {
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 5;");
        
        boolean isCurrentPlayer = player.equals(gameEngine.getCurrentPlayer());
        String borderColor = isCurrentPlayer ? "#f39c12" : "#bdc3c7";
        box.setStyle(box.getStyle() + " -fx-border-color: " + borderColor + "; -fx-border-width: " + (isCurrentPlayer ? "2" : "1") + ";");
        
        Label nameLabel = new Label(player.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label handCountLabel = new Label("Hand: " + player.getHand().size() + " cards");
        Label propertyCountLabel = new Label("Properties: " + player.getAllProperties().size());
        Label bankLabel = new Label("Bank: " + player.getBankTotalValue() + "M");

        box.getChildren().addAll(nameLabel, handCountLabel, propertyCountLabel, bankLabel);
        
        return box;
    }
    
    private void updateCurrentPlayerDisplay() {
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
        playerHand.getChildren().clear();
        
        if (currentPlayer == null) return;
        
        boolean handClickable = gameEngine.canPlayCard();
        for (Card card : currentPlayer.getHand()) {
            CardView cardView = new CardView(card, handClickable);
            if (handClickable) {
                cardView.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        selectCard(card, cardView);
                        playSelectedCard();
                    } else {
                        selectCard(card, cardView);
                    }
                });
            }
            playerHand.getChildren().add(cardView);
        }
    }
    
    private void selectCard(Card card, CardView cardView) {
        for (javafx.scene.Node node : playerHand.getChildren()) {
            if (node instanceof CardView cv) {
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

        if (currentPlayer == null) {
            return;
        }

        int total = currentPlayer.getBankTotalValue();
        if (bankTotalLabel != null) {
            bankTotalLabel.setText("Bank total: " + total + "M");
        }

        for (Card card : currentPlayer.getBank()) {
            playerBank.getChildren().add(new CardView(card, false));
        }

        if (playerBank.getChildren().isEmpty()) {
            Label hint = new Label("（打出金钱牌会放入银行）");
            hint.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            playerBank.getChildren().add(hint);
        }
    }

    private void updatePlayerProperties() {
        playerProperties.getChildren().clear();

        if (currentPlayer == null) {
            return;
        }

        List<PropertyCard> all = currentPlayer.getAllProperties();
        if (all.isEmpty()) {
            Label empty = new Label("（暂无地产，打出地产牌会按颜色分组显示在这里）");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            playerProperties.getChildren().add(empty);
            return;
        }

        Map<Color, List<PropertyCard>> byColor = new LinkedHashMap<>();
        for (PropertyCard property : all) {
            Color color = property.getColor() != null ? property.getColor() : Color.BROWN;
            byColor.computeIfAbsent(color, k -> new ArrayList<>()).add(property);
        }

        for (Map.Entry<Color, List<PropertyCard>> entry : byColor.entrySet()) {
            Color color = entry.getKey();
            List<PropertyCard> cards = entry.getValue();

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 4 0;");

            int required = color.getSetSize();
            Label groupLabel = new Label(color + " (" + cards.size() + "/" + required + ")");
            groupLabel.setStyle("-fx-font-weight: bold; -fx-min-width: 120;");

            FlowPane groupPane = new FlowPane(8, 8);
            groupPane.setPrefWrapLength(600);
            for (PropertyCard property : cards) {
                CardView cardView = new CardView(property, false);
                groupPane.getChildren().add(cardView);
            }

            row.getChildren().addAll(groupLabel, groupPane);
            playerProperties.getChildren().add(row);
        }
    }

    private void updateOpponentsHands() {
        opponentsHands.getChildren().clear();
        
        for (Player player : players) {
            if (!player.equals(currentPlayer)) {
                Label label = new Label(player.getName() + ": " + player.getHand().size() + " cards");
                label.setStyle("-fx-padding: 5;");
                opponentsHands.getChildren().add(label);
            }
        }
    }
    
    private void updatePileCounts() {
        drawPileCount.setText("Draw Pile");
        discardPileCount.setText("Discard Pile");
    }
    
    private void updateButtonStates() {
        boolean active = isCurrentPlayerTurn();
        boolean canDraw = active && gameEngine.canDrawCards();
        boolean canPlayCard = active && gameEngine.canPlayCard();

        drawCardBtn.setDisable(!canDraw);
        playCardBtn.setDisable(!canPlayCard || selectedCard == null);
        endTurnBtn.setDisable(!active);
    }
    
    private StackPane createCardPane(Card card, boolean clickable) {
        StackPane pane = new StackPane();
        pane.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        pane.setCursor(clickable ? Cursor.HAND : Cursor.DEFAULT);
        
        // 根据卡牌类型设置背景色
        String backgroundColor = getCardBackgroundColor(card);
        String textColor = getCardTextColor(card);
        
        pane.setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: #2c3e50;" +
            "-fx-border-width: 2;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
        );
        
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 5;");
        
        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-font-size: 11px; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(CARD_WIDTH - 16);
        
        Label typeLabel = new Label(card.getType().toString());
        typeLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 9px; -fx-opacity: 0.8;");

        if (card instanceof RentCard rentCard) {
            Label bankLabel = new Label("Bank " + rentCard.getBankValueM() + "M");
            bankLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 9px;");
            String colorsText = rentCard.isAllColors()
                    ? "All colors"
                    : String.join(" / ", Arrays.stream(rentCard.getApplicableColors())
                    .map(Color::name).toArray(String[]::new));
            Label colorsLabel = new Label(colorsText);
            colorsLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 8px; -fx-wrap-text: true;");
            colorsLabel.setMaxWidth(CARD_WIDTH - 16);
            colorsLabel.setWrapText(true);
            content.getChildren().addAll(nameLabel, colorsLabel, bankLabel, typeLabel);
        } else if (card instanceof ActionCard actionCard) {
            Label bankLabel = new Label("Bank " + actionCard.getBankValueM() + "M");
            bankLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 9px;");
            content.getChildren().addAll(nameLabel, bankLabel, typeLabel);
        } else {
            content.getChildren().addAll(nameLabel, typeLabel);
        }
        
        pane.getChildren().add(content);
        
        // 添加悬停效果
        if (clickable) {
            pane.setOnMouseEntered(e -> pane.setStyle(pane.getStyle() + "-fx-scale-x: 1.05; -fx-scale-y: 1.05;"));
            pane.setOnMouseExited(e -> pane.setStyle(pane.getStyle().replace("-fx-scale-x: 1.05; -fx-scale-y: 1.05;", "")));
        }
        
        return pane;
    }
    
    private StackPane createPropertyCardPane(PropertyCard property) {
        StackPane pane = new StackPane();
        pane.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        
        String backgroundColor = getPropertyColorHex(property.getColor());
        
        pane.setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: #2c3e50;" +
            "-fx-border-width: 2;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 2);"
        );
        
        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 5;");
        
        Label nameLabel = new Label(property.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(CARD_WIDTH - 16);
        nameLabel.setWrapText(true);
        
        Label valueLabel = new Label("Value " + property.getPrice() + "M");
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");

        String rentText = property.getRentDisplay();
        if (!rentText.isEmpty()) {
            Label rentLabel = new Label("Rent " + rentText);
            rentLabel.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 9px; -fx-wrap-text: true;");
            rentLabel.setMaxWidth(CARD_WIDTH - 16);
            rentLabel.setWrapText(true);
            content.getChildren().addAll(nameLabel, valueLabel, rentLabel);
        } else {
            content.getChildren().addAll(nameLabel, valueLabel);
        }
        
        pane.getChildren().add(content);
        
        return pane;
    }
    
    private String getCardBackgroundColor(Card card) {
        switch (card.getType()) {
            case PROPERTY:
                if (card instanceof PropertyCard) {
                    return getPropertyColorHex(((PropertyCard) card).getColor());
                }
                return "#95a5a6";
            case MONEY:
                return "#27ae60";
            case ACTION:
                return "#e74c3c";
            default:
                return "#95a5a6";
        }
    }
    
    private String getCardTextColor(Card card) {
        return "white";
    }
    
    private String getPropertyColorHex(model.enums.Color color) {
        if (color == null) return "#95a5a6";
        
        switch (color) {
            case BROWN:
                return "#8B4513";
            case LIGHT_BLUE:
                return "#87CEEB";
            case PINK:
                return "#FF69B4";
            case ORANGE:
                return "#FFA500";
            case RED:
                return "#DC143C";
            case YELLOW:
                return "#FFD700";
            case GREEN:
                return "#228B22";
            case DARK_BLUE:
                return "#00008B";
            case BLACK:
                return "#2c3e50";
            case LIGHT_GREEN:
                return "#90EE90";
            default:
                return "#95a5a6";
        }
    }

    
    private void logMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            gameLog.appendText("[" + timestamp + "] " + message + "\n");
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
}
