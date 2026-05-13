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
    private FlowPane playerProperties;
    
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
        for (int i = 0; i < 5; i++) {
            cards.add(new MoneyCard("1M Banknote", "Worth 1 million", 1));
        }
        for (int i = 0; i < 5; i++) {
            cards.add(new MoneyCard("2M Banknote", "Worth 2 million", 2));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new MoneyCard("3M Banknote", "Worth 3 million", 3));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new MoneyCard("4M Banknote", "Worth 4 million", 4));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new MoneyCard("5M Banknote", "Worth 5 million", 5));
        }
        
        // 行动卡
        for (int i = 0; i < 3; i++) {
            cards.add(new ActionCard("Pass Go", "Draw 2 extra cards"));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new ActionCard("It's My Birthday", "Collect 2M from each player"));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new ActionCard("Double The Rent", "Double rent amount"));
        }
        cards.add(new ActionCard("Deal Breaker", "Steal a complete property set"));
        for (int i = 0; i < 3; i++) {
            cards.add(new ActionCard("Just Say No", "Cancel any action card"));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new ActionCard("Sly Deal", "Steal a single property"));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new ActionCard("Forced Deal", "Swap properties with a player"));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new ActionCard("Debt Collector", "Collect 5M from a player"));
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
        try {
            Player player = gameEngine.getCurrentPlayer();
            
            // 抽2张牌
            for (int i = 0; i < 2; i++) {
                Card card = deck.draw();
                player.draw(card);
                logMessage(player.getName() + " drew a card");
            }
            
            showStatus("Drew 2 cards successfully", false);
            updateUI();
        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), true);
            logMessage("Error drawing cards: " + e.getMessage());
        }
    }
    
    @FXML
    private void onPlayCardClick() {
        if (!isCurrentPlayerTurn()) {
            showStatus("Not your turn!", true);
            return;
        }
        
        if (selectedCard == null) {
            showStatus("Please select a card first!", true);
            return;
        }
        
        playSelectedCard();
    }
    
    private void playSelectedCard() {
        try {
            Player player = gameEngine.getCurrentPlayer();
            
            logMessage(player.getName() + " played: " + selectedCard.getName());
            
            // 使用卡牌
            selectedCard.use(player, gameEngine);
            
            // 从手牌中移除
            player.removeFromHand(selectedCard);
            
            showStatus("Played: " + selectedCard.getName(), false);
            selectedCard = null;
            
            // 检查胜利条件
            if (gameEngine.checkWin(player)) {
                showGameOver(player);
                return;
            }
            
            updateUI();
        } catch (Exception e) {
            showStatus("Error playing card: " + e.getMessage(), true);
            logMessage("Error playing card: " + e.getMessage());
        }
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
        logMessage(player.getName() + " ended their turn");
        
        gameEngine.nextTurn();
        currentPlayer = gameEngine.getCurrentPlayer();
        
        showStatus(currentPlayer.getName() + "'s turn now", false);
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
            updatePlayerInfo();
            updateCurrentPlayerDisplay();
            updatePlayerHand();
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
        Label propertyCountLabel = new Label("Properties: " + player.getProperties().size());
        
        box.getChildren().addAll(nameLabel, handCountLabel, propertyCountLabel);
        
        return box;
    }
    
    private void updateCurrentPlayerDisplay() {
        if (currentPlayer != null) {
            currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());
        }
    }
    
    private void updatePlayerHand() {
        playerHand.getChildren().clear();
        
        if (currentPlayer == null) return;
        
        for (Card card : currentPlayer.getHand()) {
            StackPane cardPane = createCardPane(card, true);
            cardPane.setOnMouseClicked(event -> selectCard(card, cardPane));
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
        
        showStatus("Selected: " + card.getName(), false);
    }
    
    private void updatePlayerProperties() {
        playerProperties.getChildren().clear();
        
        if (currentPlayer == null) return;
        
        for (PropertyCard property : currentPlayer.getProperties()) {
            StackPane cardPane = createPropertyCardPane(property);
            playerProperties.getChildren().add(cardPane);
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
        boolean canPlay = isCurrentPlayerTurn();
        drawCardBtn.setDisable(!canPlay);
        playCardBtn.setDisable(!canPlay || selectedCard == null);
        endTurnBtn.setDisable(!canPlay);
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
        
        content.getChildren().addAll(nameLabel, typeLabel);
        
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
