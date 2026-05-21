package controller;

import engine.Deck;
import engine.GameEngine;
import javafx.application.Platform;
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
            cards.add(new MyBirthday("My birthday","Collect 2M from each player",CardType.ACTION));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new DoubleTheRent("Double The Rent","Double rent amount",CardType.ACTION));
        }
        for(int i = 0; i < 2; i++) {
            cards.add(new DealBreaker("Deal Breaker", "Steal a complete property set",CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new JustSayNo("Just Say No", "Cancel any action card",CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new SlyDeal("Sly Deal", "Steal a single property",CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new ForcedDeal("Forced Deal", "Swap properties with a player", CardType.ACTION));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new DebtCollector("Debt Collector", "Collect 5M from a player",CardType.ACTION));
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
        if (actionCard instanceof DealBreaker dealBreaker) {
            return resolveDealBreaker(player, dealBreaker);
        }
        if (actionCard instanceof DebtCollector debtCollector) {
            Optional<Player> target = promptSelectOpponent(player, "选择要收取 5M 的玩家");
            if (target.isEmpty()) {
                return ActionEffectResult.CANCELLED;
            }
            debtCollector.collectFrom(player, target.get());
            return ActionEffectResult.SUCCESS;
        }
        if (actionCard instanceof SlyDeal slyDeal) {
            Optional<Player> target = promptSelectOpponent(player, "选择要偷取地产的玩家");
            if (target.isEmpty()) {
                return ActionEffectResult.CANCELLED;
            }
            return slyDeal.stealOneProperty(player, target.get())
                    ? ActionEffectResult.SUCCESS
                    : ActionEffectResult.FAILED;
        }
        if (actionCard instanceof ForcedDeal forcedDeal) {
            Optional<Player> target = promptSelectOpponent(player, "选择要交换地产的玩家");
            if (target.isEmpty()) {
                return ActionEffectResult.CANCELLED;
            }
            return forcedDeal.swapOneProperty(player, target.get())
                    ? ActionEffectResult.SUCCESS
                    : ActionEffectResult.FAILED;
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

        actionCard.use(player, gameEngine);
        return ActionEffectResult.SUCCESS;
    }

    private ActionEffectResult resolveDealBreaker(Player player, DealBreaker dealBreaker) {
        Optional<Player> target = promptSelectOpponent(player, "Deal Breaker：选择要偷取套组的玩家");
        if (target.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        Player opponent = target.get();
        if (!hasAnyCompleteSet(opponent)) {
            showStatus(opponent.getName() + " 没有可偷的完整地产套组", true);
            return ActionEffectResult.FAILED;
        }
        Optional<Color> color = promptSelectCompleteSetOnPlayer(opponent);
        if (color.isEmpty()) {
            return ActionEffectResult.CANCELLED;
        }
        return dealBreaker.useOnTarget(player, opponent, color.get())
                ? ActionEffectResult.SUCCESS
                : ActionEffectResult.FAILED;
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
            StackPane cardPane = createCardPane(card, handClickable);
            if (handClickable) {
                cardPane.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        selectCard(card, cardPane);
                        playSelectedCard();
                    } else {
                        selectCard(card, cardPane);
                    }
                });
            }
            playerHand.getChildren().add(cardPane);
        }
    }
    
    private void selectCard(Card card, StackPane cardPane) {
        // 清除之前的选择
        for (javafx.scene.Node node : playerHand.getChildren()) {
            if (node instanceof StackPane) {
                node.setStyle(node.getStyle().replace("-fx-border-color: #f39c12; -fx-border-width: 3;", ""));
            }
        }
        
        selectedCard = card;
        cardPane.setStyle(cardPane.getStyle() + "-fx-border-color: #f39c12; -fx-border-width: 3;");
        
        if (card instanceof ActionCard actionCard) {
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
            playerBank.getChildren().add(createBankCardPane(card));
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
                groupPane.getChildren().add(createPropertyCardPane(property));
            }

            row.getChildren().addAll(groupLabel, groupPane);
            playerProperties.getChildren().add(row);
        }
    }

    private StackPane createBankCardPane(Card card) {
        StackPane pane = new StackPane();
        pane.setPrefSize(90, 120);

        pane.setStyle(
            "-fx-background-color: #27ae60;" +
            "-fx-border-color: #1e8449;" +
            "-fx-border-width: 2;" +
            "-fx-background-radius: 8;" +
            "-fx-border-radius: 8;"
        );

        VBox content = new VBox(5);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-padding: 5;");

        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(80);

        if (card instanceof MoneyCard moneyCard) {
            Label valueLabel = new Label(moneyCard.getMoney() + "M");
            valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            content.getChildren().addAll(nameLabel, valueLabel);
        } else if (card instanceof ActionCard actionCard) {
            Label valueLabel = new Label(actionCard.getBankValueM() + "M");
            valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
            content.getChildren().addAll(nameLabel, valueLabel);
        } else {
            content.getChildren().add(nameLabel);
        }

        pane.getChildren().add(content);
        return pane;
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
        pane.setPrefSize(100, 140);
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
        nameLabel.setMaxWidth(90);
        
        Label typeLabel = new Label(card.getType().toString());
        typeLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 9px; -fx-opacity: 0.8;");

        if (card instanceof ActionCard actionCard) {
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
        pane.setPrefSize(100, 140);
        
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
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(90);
        
        Label valueLabel = new Label(property.getPrice() + "M");
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        content.getChildren().addAll(nameLabel, valueLabel);
        
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
