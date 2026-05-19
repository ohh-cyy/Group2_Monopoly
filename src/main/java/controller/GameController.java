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
import ui.CardView;
import model.card.*;
import model.card.actionCard.SimpleActionCard;
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
            cards.add(new SimpleActionCard("Pass Go", "Draw 2 extra cards"));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new SimpleActionCard("It's My Birthday", "Collect 2M from each player"));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new SimpleActionCard("Double The Rent", "Double rent amount"));
        }
        cards.add(new SimpleActionCard("Deal Breaker", "Steal a complete property set"));
        for (int i = 0; i < 3; i++) {
            cards.add(new SimpleActionCard("Just Say No", "Cancel any action card"));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new SimpleActionCard("Sly Deal", "Steal a single property"));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new SimpleActionCard("Forced Deal", "Swap properties with a player"));
        }
        for (int i = 0; i < 2; i++) {
            cards.add(new SimpleActionCard("Debt Collector", "Collect 5M from a player"));
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
            CardView cardView = new CardView(card, true);
            cardView.setOnMouseClicked(event -> selectCard(card, cardView));
            playerHand.getChildren().add(cardView);
        }
    }
    
    private void selectCard(Card card, CardView cardView) {
        // 清除之前的选择
        for (javafx.scene.Node node : playerHand.getChildren()) {
            if (node instanceof CardView) {
                ((CardView) node).setSelected(false);
            }
        }
        
        selectedCard = card;
        cardView.setSelected(true);
        
        showStatus("Selected: " + card.getName(), false);
    }
    
    private void updatePlayerProperties() {
        playerProperties.getChildren().clear();
        
        if (currentPlayer == null) return;
        
        for (PropertyCard property : currentPlayer.getProperties()) {
            CardView cardView = new CardView(property, false);
            playerProperties.getChildren().add(cardView);
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
