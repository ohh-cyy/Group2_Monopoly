package controller.view;

import engine.GameEngine;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import model.card.Card;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;
import sync.CardSnapshotMapper;
import sync.PlayerPublicSnapshot;
import ui.CardView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Renders the game screen from a plain view state.
 */
public class GameViewRenderer {
    private final Label currentPlayerLabel;
    private final Label gameStatusText;
    private final VBox playersList;
    private final TilePane allPlayersPropertiesPanel;
    private final Label turnBankLabel;
    private final ScrollPane playerHandScroll;
    private final HBox playerHand;
    private final FlowPane playerBank;
    private final Label bankTotalLabel;
    private final Button drawCardBtn;
    private final Button playCardBtn;
    private final Button endTurnBtn;
    private final BiConsumer<Card, CardView> selectCard;
    private final Consumer<Card> playCard;
    private Image avatarImage;

    public GameViewRenderer(Label currentPlayerLabel,
                            Label gameStatusText,
                            VBox playersList,
                            TilePane allPlayersPropertiesPanel,
                            Label turnBankLabel,
                            ScrollPane playerHandScroll,
                            HBox playerHand,
                            FlowPane playerBank,
                            Label bankTotalLabel,
                            Button drawCardBtn,
                            Button playCardBtn,
                            Button endTurnBtn,
                            BiConsumer<Card, CardView> selectCard,
                            Consumer<Card> playCard) {
        this.currentPlayerLabel = currentPlayerLabel;
        this.gameStatusText = gameStatusText;
        this.playersList = playersList;
        this.allPlayersPropertiesPanel = allPlayersPropertiesPanel;
        this.turnBankLabel = turnBankLabel;
        this.playerHandScroll = playerHandScroll;
        this.playerHand = playerHand;
        this.playerBank = playerBank;
        this.bankTotalLabel = bankTotalLabel;
        this.drawCardBtn = drawCardBtn;
        this.playCardBtn = playCardBtn;
        this.endTurnBtn = endTurnBtn;
        this.selectCard = selectCard;
        this.playCard = playCard;
    }

    public void setAvatarImage(Image avatarImage) {
        this.avatarImage = avatarImage;
    }

    public void render(GameViewState state) {
        updatePlayerInfo(state);
        updateCurrentPlayerDisplay(state);
        updateAllPlayersProperties(state);
        updateTurnBankLabel(state);
        updatePlayerHand(state);
        updatePlayerBank(state);
        updatePileCounts(state);
        updateButtonStates(state);
    }

    private void updatePlayerInfo(GameViewState state) {
        if (playersList == null) {
            return;
        }
        playersList.getChildren().clear();

        if (state.clientMode()) {
            for (PlayerPublicSnapshot view : state.publicPlayers()) {
                Player stub = new Player(view.name);
                playersList.getChildren().add(createPlayerInfoBox(stub, view.handSize, view.bankTotal,
                        view.properties.size(), view.seat == state.currentTurnSeat()));
            }
            return;
        }

        GameEngine gameEngine = state.gameEngine();
        if (gameEngine == null) {
            return;
        }
        for (Player player : gameEngine.getPlayers()) {
            boolean current = player.equals(gameEngine.getCurrentPlayer());
            playersList.getChildren().add(createPlayerInfoBox(player, player.getHand().size(),
                    player.getBankTotalValue(), player.getAllProperties().size(), current));
        }
    }

    private VBox createPlayerInfoBox(Player player, int handSize, int bankTotal,
                                     int propertyCount, boolean isCurrent) {
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

        Label handCountLabel = new Label("Hand: " + handSize + " cards");
        Label propertyCountLabel = new Label("Properties: " + propertyCount);
        Label bankLabel = new Label("Bank: " + bankTotal + "M");

        box.getChildren().addAll(header, handCountLabel, propertyCountLabel, bankLabel);
        return box;
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

    private void updateCurrentPlayerDisplay(GameViewState state) {
        if (currentPlayerLabel == null) {
            return;
        }
        String drawStatus = state.hasDrawnThisTurn() ? "Drew" : "Not drawn";
        currentPlayerLabel.setText("Current Player: " + state.currentTurnName()
                + " | " + drawStatus
                + " | Plays left: " + state.remainingPlays() + "/" + state.maxPlaysPerTurn()
                + (state.gameOver() ? " | Finished" : ""));
    }

    private void updatePlayerHand(GameViewState state) {
        if (playerHand == null) {
            return;
        }
        playerHand.getChildren().clear();

        List<Card> hand = state.handCards();
        CardView.CardMetrics metrics = computeHandMetrics(hand.size());
        for (Card card : hand) {
            StackPane slot = CardView.wrapInSlot(card, state.handClickable(), metrics);
            CardView cardView = CardView.getCardView(slot);
            if (state.selectedCard() != null && state.selectedCard().equals(card) && cardView != null) {
                cardView.setSelected(true);
            }
            if (state.handClickable() && cardView != null) {
                slot.setOnMouseClicked(event -> {
                    selectCard.accept(card, cardView);
                    if (event.getClickCount() == 2) {
                        playCard.accept(card);
                    }
                });
            }
            playerHand.getChildren().add(slot);
        }
    }

    /** Keeps hand cards in one row by scaling the card size when space is tight. */
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

    public void clearHandSelection() {
        if (playerHand == null) {
            return;
        }
        for (javafx.scene.Node node : playerHand.getChildren()) {
            CardView cardView = node instanceof StackPane slot ? CardView.getCardView(slot) : null;
            if (cardView != null) {
                cardView.setSelected(false);
            }
        }
    }

    private void updatePlayerBank(GameViewState state) {
        if (playerBank == null) {
            return;
        }
        playerBank.getChildren().clear();

        if (bankTotalLabel != null) {
            bankTotalLabel.setText(state.bankTotal() + "M");
        }

        for (Card card : state.bankCards()) {
            StackPane slot = CardView.wrapInSlot(card, false, CardView.COMPACT);
            slot.getStyleClass().add("bank-card-slot");
            playerBank.getChildren().add(slot);
        }

        if (playerBank.getChildren().isEmpty()) {
            Label hint = new Label("(Money cards, action cards, and received payments go into the bank)");
            hint.setStyle("-fx-text-fill: #476272; -fx-font-size: 12px; -fx-wrap-text: true;");
            playerBank.getChildren().add(hint);
        }
    }

    private void updateAllPlayersProperties(GameViewState state) {
        if (allPlayersPropertiesPanel == null) {
            return;
        }
        allPlayersPropertiesPanel.getChildren().clear();

        List<PlayerPublicSnapshot> views = state.publicPlayers();
        if (views.isEmpty()) {
            Label empty = new Label("(No player properties)");
            empty.setStyle("-fx-text-fill: #7f8c8d;");
            allPlayersPropertiesPanel.getChildren().add(empty);
            return;
        }

        for (PlayerPublicSnapshot view : views) {
            VBox playerBlock = new VBox(8);
            playerBlock.setMaxWidth(Double.MAX_VALUE);
            boolean isTurn = view.seat == state.currentTurnSeat();
            playerBlock.getStyleClass().add("player-public-block");
            if (isTurn) {
                playerBlock.getStyleClass().add("player-public-block-current");
            }

            HBox titleRow = new HBox(9);
            titleRow.setAlignment(Pos.CENTER_LEFT);
            ImageView avatar = createAvatarView(38);
            Label title = new Label((isTurn ? "▶ " : "") + view.name
                    + "  |  Hand " + view.handSize + " card(s)  |  Bank " + view.bankTotal + "M");
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
            byColor.computeIfAbsent(color, key -> new ArrayList<>()).add(card);
        }
        return byColor;
    }

    private void updateTurnBankLabel(GameViewState state) {
        if (turnBankLabel == null) {
            return;
        }
        turnBankLabel.setText(state.currentTurnName() + ": " + currentTurnBankTotal(state) + "M");
    }

    private int currentTurnBankTotal(GameViewState state) {
        for (PlayerPublicSnapshot player : state.publicPlayers()) {
            if (player.seat == state.currentTurnSeat()) {
                return player.bankTotal;
            }
        }
        return 0;
    }

    private void updatePileCounts(GameViewState state) {
        if (gameStatusText == null) {
            return;
        }
        if (state.gameOver() && state.winnerName() != null) {
            gameStatusText.setText("Winner: " + state.winnerName());
            return;
        }
        gameStatusText.setText("Draw pile: " + state.drawPileSize()
                + "  |  Discard pile: " + state.discardPileSize());
    }

    private void updateButtonStates(GameViewState state) {
        if (drawCardBtn != null) {
            drawCardBtn.setDisable(!state.canDraw());
        }
        if (playCardBtn != null) {
            playCardBtn.setDisable(!state.canPlayCard() || state.selectedCard() == null);
        }
        if (endTurnBtn != null) {
            endTurnBtn.setDisable(!state.canEndTurn());
        }
    }
}
