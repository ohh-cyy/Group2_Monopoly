package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import model.card.Card;
import model.enums.Color;
import network.CardMapper;
import network.NetworkCard;
import network.client.NetworkClient;
import network.protocol.GameStateDto;
import network.protocol.MessageTypes;
import network.protocol.PlayerViewDto;
import network.protocol.ServerMessage;
import ui.CardView;

import java.util.*;

/** 联机模式游戏界面：只发命令，根据服务端 STATE 刷新 UI */
public class NetworkGameController {
    @FXML
    private Label currentPlayerLabel;
    @FXML
    private Label gameStatusText;
    @FXML
    private Label statusMessage;
    @FXML
    private VBox playersList;
    @FXML
    private ScrollPane playerHandScroll;
    @FXML
    private HBox playerHand;
    @FXML
    private FlowPane playerProperties;
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

    private NetworkClient client;
    private int localSeat = -1;
    private GameStateDto state;
    private Card selectedCard;
    private List<Card> localHand = new ArrayList<>();
    private List<Card> localBank = new ArrayList<>();
    private List<Card> localProperties = new ArrayList<>();

    @FXML
    public void initialize() {
        if (playerHandScroll != null) {
            playerHandScroll.widthProperty().addListener((obs, o, n) -> updatePlayerHand());
        }
        setupButtonActions();
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
            newGameBtn.setText("断开连接");
            newGameBtn.setOnAction(e -> disconnect());
        }
    }

    private void onServerMessage(ServerMessage message) {
        Platform.runLater(() -> handleMessage(message));
    }

    private void handleMessage(ServerMessage message) {
        if (message == null) {
            return;
        }
        if (MessageTypes.ERROR.equals(message.type)) {
            showStatus(message.message, true);
            return;
        }
        if (message.events != null) {
            for (String event : message.events) {
                logMessage(event);
            }
        }
        if (message.state != null) {
            state = message.state;
            localSeat = state.yourSeat;
            applyLocalPlayerData();
            updateUI();
        }
        if (message.message != null && !message.message.isBlank()
                && !MessageTypes.STATE.equals(message.type)) {
            showStatus(message.message, false);
        }
    }

    private void applyLocalPlayerData() {
        localHand.clear();
        localBank.clear();
        localProperties.clear();
        if (state == null) {
            return;
        }
        for (PlayerViewDto p : state.players) {
            if (p.you) {
                if (p.hand != null) {
                    localHand.addAll(CardMapper.fromDtoList(p.hand));
                }
                if (p.bank != null) {
                    localBank.addAll(CardMapper.fromDtoList(p.bank));
                }
                if (p.properties != null) {
                    localProperties.addAll(CardMapper.fromDtoList(p.properties));
                }
                break;
            }
        }
    }

    public void onDrawCardsClick() {
        if (!isMyTurn()) {
            showStatus("还没轮到你", true);
            return;
        }
        client.draw();
    }

    public void onPlayCardClick() {
        if (!isMyTurn()) {
            showStatus("还没轮到你", true);
            return;
        }
        if (selectedCard == null) {
            showStatus("请先选中一张手牌", true);
            return;
        }
        playSelectedCard();
    }

    private void playSelectedCard() {
        Card card = selectedCard;
        selectedCard = null;

        if (card instanceof NetworkCard nc) {
            String kind = nc.getCardKind();
            if ("WILD_PROPERTY".equals(kind)) {
                playWildCard(nc);
                return;
            }
            if (isActionKind(kind)) {
                playActionOrRent(nc);
                return;
            }
        }

        client.playCard(card.getInstanceId(), "PLAY", null);
    }

    private void playWildCard(NetworkCard wild) {
        if (wild.isBankable()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("万能地产");
            alert.setHeaderText(wild.getName());
            ButtonType asProperty = new ButtonType("作为地产打出");
            ButtonType asBank = new ButtonType("存入银行 (" + wild.getBankValueM() + "M)");
            ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(asProperty, asBank, cancel);
            Optional<ButtonType> choice = alert.showAndWait();
            if (choice.isEmpty() || choice.get() == cancel) {
                selectedCard = wild;
                return;
            }
            if (choice.get() == asBank) {
                client.playCard(wild.getInstanceId(), "BANK", null);
                return;
            }
        }
        List<Color> colors = wild.getAvailableColors();
        if (colors.isEmpty()) {
            showStatus("无法打出该万能卡", true);
            return;
        }
        ChoiceDialog<Color> dialog = new ChoiceDialog<>(colors.get(0), colors);
        dialog.setTitle("选择颜色");
        dialog.setHeaderText("万能地产作为哪种颜色？");
        Optional<Color> color = dialog.showAndWait();
        if (color.isEmpty()) {
            selectedCard = wild;
            return;
        }
        client.playCard(wild.getInstanceId(), "PROPERTY", color.get().name());
    }

    private void playActionOrRent(NetworkCard card) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("行动牌 / 租金卡");
        alert.setHeaderText(card.getName() + " — 银行 " + card.getBankValueM() + "M");
        alert.setContentText("联机 MVP 暂不支持使用效果，可存入银行。");
        ButtonType bank = new ButtonType("存入银行");
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(bank, cancel);
        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isPresent() && choice.get() == bank) {
            client.playCard(card.getInstanceId(), "BANK", null);
        } else {
            selectedCard = card;
        }
    }

    private boolean isActionKind(String kind) {
        return kind != null && !kind.equals("MONEY") && !kind.equals("PROPERTY") && !kind.equals("WILD_PROPERTY");
    }

    public void onEndTurnClick() {
        if (!isMyTurn()) {
            showStatus("还没轮到你", true);
            return;
        }
        client.endTurn();
    }

    private void disconnect() {
        if (client != null) {
            client.disconnect();
        }
        showStatus("已断开连接", true);
    }

    private boolean isMyTurn() {
        return state != null && !state.gameOver && state.currentPlayerIndex == localSeat;
    }

    private void updateUI() {
        updatePlayerInfo();
        updateCurrentPlayerDisplay();
        updatePlayerHand();
        updatePlayerBank();
        updatePlayerProperties();
        updatePileCounts();
        updateButtonStates();
    }

    private void updatePlayerInfo() {
        if (playersList == null || state == null) {
            return;
        }
        playersList.getChildren().clear();
        for (PlayerViewDto p : state.players) {
            VBox box = new VBox(5);
            boolean current = p.seat == state.currentPlayerIndex;
            String border = current ? "#f39c12" : "#bdc3c7";
            box.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 5;"
                    + " -fx-border-color: " + border + "; -fx-border-width: " + (current ? "2" : "1") + ";");
            Label name = new Label(p.name + (p.you ? "（你）" : ""));
            name.setStyle("-fx-font-weight: bold;");
            box.getChildren().addAll(
                    name,
                    new Label("Hand: " + p.handSize),
                    new Label("Properties: " + p.propertyCount),
                    new Label("Bank: " + p.bankTotal + "M")
            );
            playersList.getChildren().add(box);
        }
    }

    private void updateCurrentPlayerDisplay() {
        if (state == null || currentPlayerLabel == null) {
            return;
        }
        PlayerViewDto current = state.players.get(state.currentPlayerIndex);
        String drawStatus = state.hasDrawnThisTurn ? "已抽牌" : "未抽牌";
        currentPlayerLabel.setText("Current: " + current.name
                + " | " + drawStatus
                + " | 剩余出牌: " + state.remainingPlays + "/3"
                + (state.gameOver ? " | 游戏结束" : ""));
    }

    private void updatePlayerHand() {
        if (playerHand == null) {
            return;
        }
        playerHand.getChildren().clear();
        boolean clickable = isMyTurn() && state.remainingPlays > 0;
        CardView.CardMetrics metrics = computeHandMetrics(localHand.size());

        for (Card card : localHand) {
            StackPane slot = CardView.wrapInSlot(card, clickable, metrics);
            CardView cv = CardView.getCardView(slot);
            if (clickable && cv != null) {
                slot.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        selectCard(card, cv);
                        playSelectedCard();
                    } else {
                        selectCard(card, cv);
                    }
                });
            }
            playerHand.getChildren().add(slot);
        }
    }

    private CardView.CardMetrics computeHandMetrics(int count) {
        if (count <= 0) {
            return CardView.HAND;
        }
        double available = 750;
        if (playerHandScroll != null && playerHandScroll.getViewportBounds().getWidth() > 0) {
            available = playerHandScroll.getViewportBounds().getWidth() - 20;
        }
        double total = count * CardView.HAND.slotW() + (count - 1) * 6;
        if (total <= available) {
            return CardView.HAND;
        }
        return CardView.HAND.scaled(Math.max(0.42, available / total));
    }

    private void selectCard(Card card, CardView cv) {
        for (var node : playerHand.getChildren()) {
            if (node instanceof StackPane sp) {
                CardView view = CardView.getCardView(sp);
                if (view != null) {
                    view.setSelected(false);
                }
            }
        }
        selectedCard = card;
        cv.setSelected(true);
        showStatus("已选中: " + card.getName(), false);
        updateButtonStates();
    }

    private void updatePlayerBank() {
        if (playerBank == null) {
            return;
        }
        playerBank.getChildren().clear();
        int total = 0;
        if (state != null) {
            for (PlayerViewDto p : state.players) {
                if (p.you) {
                    total = p.bankTotal;
                    break;
                }
            }
        }
        if (bankTotalLabel != null) {
            bankTotalLabel.setText("Bank total: " + total + "M");
        }
        for (Card card : localBank) {
            playerBank.getChildren().add(CardView.wrapInSlot(card, false, CardView.COMPACT));
        }
    }

    private void updatePlayerProperties() {
        if (playerProperties == null) {
            return;
        }
        playerProperties.getChildren().clear();
        if (localProperties.isEmpty()) {
            Label empty = new Label("（暂无地产）");
            empty.setStyle("-fx-text-fill: #7f8c8d;");
            playerProperties.getChildren().add(empty);
            return;
        }
        Map<Color, List<Card>> byColor = new LinkedHashMap<>();
        for (Card card : localProperties) {
            Color color = card.getColor() != null ? card.getColor() : Color.BROWN;
            byColor.computeIfAbsent(color, k -> new ArrayList<>()).add(card);
        }
        for (Map.Entry<Color, List<Card>> entry : byColor.entrySet()) {
            Color color = entry.getKey();
            List<Card> cards = entry.getValue();
            HBox set = new HBox(5);
            set.setAlignment(Pos.CENTER_LEFT);
            set.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 6; -fx-padding: 4 8;");
            Label label = new Label(color + "\n" + cards.size() + "/" + color.getSetSize());
            label.setMinWidth(52);
            label.setAlignment(Pos.CENTER);
            HBox row = new HBox(4);
            for (Card card : cards) {
                row.getChildren().add(CardView.wrapInSlot(card, false, CardView.COMPACT));
            }
            set.getChildren().addAll(label, row);
            playerProperties.getChildren().add(set);
        }
    }

    private void updatePileCounts() {
        if (gameStatusText == null || state == null) {
            return;
        }
        gameStatusText.setText("Draw pile: " + state.drawPileSize
                + "  |  Discard pile: " + state.discardPileSize);
        if (state.gameOver && state.winnerName != null) {
            gameStatusText.setText("Winner: " + state.winnerName);
        }
    }

    private void updateButtonStates() {
        if (state == null) {
            return;
        }
        boolean myTurn = isMyTurn();
        drawCardBtn.setDisable(!myTurn || state.hasDrawnThisTurn || state.gameOver);
        playCardBtn.setDisable(!myTurn || selectedCard == null || state.remainingPlays <= 0 || state.gameOver);
        endTurnBtn.setDisable(!myTurn || state.gameOver);
    }

    private void showStatus(String text, boolean error) {
        if (statusMessage != null) {
            statusMessage.setText(text);
            statusMessage.setStyle(error ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #ffffff;");
        }
    }

    private void logMessage(String message) {
        if (gameLog != null) {
            gameLog.appendText(message + "\n");
            gameLog.setScrollTop(Double.MAX_VALUE);
        }
    }

    public void attach(NetworkClient client, int seat) {
        this.client = client;
        this.localSeat = seat;
        client.setMessageListener(this::onServerMessage);
        client.requestSync();
    }

}
