package controller.view;

import engine.GameEngine;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.card.Card;
import model.player.Player;
import ui.CardView;
import ui.render.BankBarRenderer;
import ui.render.HandRenderer;
import ui.render.PersonalBankRenderer;
import ui.render.PlayerBoardView;
import ui.render.PlayerListRenderer;
import ui.render.PublicBoardRenderer;
import ui.render.TurnStatusRenderer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Refreshes all local-game board widgets from engine state. */
public final class LocalBoardRefreshService implements GameBoardRefreshService {
    private final Label currentPlayerLabel;
    private final Label gameStatusText;
    private final VBox publicBoardPanel;
    private final VBox allPlayersPropertiesPanel;
    private final HBox allPlayersBankBar;
    private final ScrollPane playerHandScroll;
    private final HBox playerHand;
    private final FlowPane playerBank;
    private final Label bankTotalLabel;
    private final Button drawCardBtn;
    private final Button discardCardBtn;
    private final Button endTurnBtn;

    private final HandRenderer handRenderer;
    private final PlayerListRenderer playerListRenderer;
    private final PublicBoardRenderer publicBoardRenderer;
    private final BankBarRenderer bankBarRenderer;
    private final PersonalBankRenderer personalBankRenderer;
    private final TurnStatusRenderer turnStatusRenderer;

    private final Supplier<GameEngine> engineSupplier;
    private final Supplier<List<Player>> playersSupplier;
    private final Supplier<Player> currentPlayerSupplier;
    private final Supplier<List<Card>> handSupplier;
    private final IntSupplier currentTurnSeatSupplier;
    private final Supplier<List<PlayerBoardView>> boardViewsSupplier;
    private final HandRenderer.SelectionListener handSelectionListener;

    private Card selectedCard;
    private BiConsumer<Card, CardView> selectedViewCallback;

    public LocalBoardRefreshService(Label currentPlayerLabel,
                                    Label gameStatusText,
                                    VBox publicBoardPanel,
                                    VBox allPlayersPropertiesPanel,
                                    HBox allPlayersBankBar,
                                    ScrollPane playerHandScroll,
                                    HBox playerHand,
                                    FlowPane playerBank,
                                    Label bankTotalLabel,
                                    Button drawCardBtn,
                                    Button discardCardBtn,
                                    Button endTurnBtn,
                                    HandRenderer handRenderer,
                                    PlayerListRenderer playerListRenderer,
                                    PublicBoardRenderer publicBoardRenderer,
                                    BankBarRenderer bankBarRenderer,
                                    Supplier<GameEngine> engineSupplier,
                                    Supplier<List<Player>> playersSupplier,
                                    Supplier<Player> currentPlayerSupplier,
                                    Supplier<List<Card>> handSupplier,
                                    IntSupplier currentTurnSeatSupplier,
                                    Supplier<List<PlayerBoardView>> boardViewsSupplier,
                                    HandRenderer.SelectionListener handSelectionListener) {
        this.currentPlayerLabel = currentPlayerLabel;
        this.gameStatusText = gameStatusText;
        this.publicBoardPanel = publicBoardPanel;
        this.allPlayersPropertiesPanel = allPlayersPropertiesPanel;
        this.allPlayersBankBar = allPlayersBankBar;
        this.playerHandScroll = playerHandScroll;
        this.playerHand = playerHand;
        this.playerBank = playerBank;
        this.bankTotalLabel = bankTotalLabel;
        this.drawCardBtn = drawCardBtn;
        this.discardCardBtn = discardCardBtn;
        this.endTurnBtn = endTurnBtn;
        this.handRenderer = handRenderer;
        this.playerListRenderer = playerListRenderer;
        this.publicBoardRenderer = publicBoardRenderer;
        this.bankBarRenderer = bankBarRenderer;
        this.personalBankRenderer = new PersonalBankRenderer();
        this.turnStatusRenderer = new TurnStatusRenderer();
        this.engineSupplier = engineSupplier;
        this.playersSupplier = playersSupplier;
        this.currentPlayerSupplier = currentPlayerSupplier;
        this.handSupplier = handSupplier;
        this.currentTurnSeatSupplier = currentTurnSeatSupplier;
        this.boardViewsSupplier = boardViewsSupplier;
        this.handSelectionListener = handSelectionListener;
    }

    @Override
    public void setSelectedCard(Card selectedCard) {
        this.selectedCard = selectedCard;
    }

    @Override
    public void applySelectionCallback(BiConsumer<Card, CardView> callback) {
        this.selectedViewCallback = callback;
    }

    @Override
    public void refreshAll(VBox playersList, java.util.function.Consumer<Double> rowHeightConsumer) {
        GameEngine engine = engineSupplier.get();
        Player currentPlayer = currentPlayerSupplier.get();
        playerListRenderer.render(playersList, playersSupplier.get(),
                engine != null ? engine.getCurrentPlayer() : null);
        refreshTurnStatus(engine, currentPlayer);
        double rowHeight = publicBoardRenderer.render(
                publicBoardPanel, allPlayersPropertiesPanel,
                boardViewsSupplier.get(), currentTurnSeatSupplier.getAsInt());
        if (rowHeightConsumer != null && rowHeight > 0) {
            rowHeightConsumer.accept(rowHeight);
        }
        if (engine != null) {
            bankBarRenderer.render(allPlayersBankBar, engine.getPlayers(), currentTurnSeatSupplier.getAsInt());
        }
        refreshHand(engine, currentPlayer);
        refreshPersonalBank(currentPlayer);
        refreshButtonStates(engine);
    }

    private void refreshTurnStatus(GameEngine engine, Player currentPlayer) {
        if (engine == null || currentPlayer == null) {
            return;
        }
        turnStatusRenderer.renderLocal(
                currentPlayerLabel,
                gameStatusText,
                currentPlayer.getName(),
                engine.hasDrawnThisTurn(),
                engine.getRemainingPlays(),
                engine.getDeck().size(),
                engine.getDiscardPile().size(),
                engine.isGameOver());
    }

    private void refreshHand(GameEngine engine, Player currentPlayer) {
        if (currentPlayer == null) {
            if (playerHand != null) {
                playerHand.getChildren().clear();
            }
            return;
        }
        boolean canSelect = engine != null && !engine.isGameOver() && engine.hasDrawnThisTurn();
        boolean canPlay = canSelect && engine.canPlayCard();
        handRenderer.render(playerHand, playerHandScroll, handSupplier.get(), selectedCard,
                canSelect, canPlay, handSelectionListener);
        if (selectedCard != null && selectedViewCallback != null) {
            handRenderer.applySelection(playerHand, selectedCard, selectedViewCallback);
        }
    }

    private void refreshPersonalBank(Player currentPlayer) {
        if (currentPlayer == null) {
            return;
        }
        personalBankRenderer.render(
                playerBank,
                bankTotalLabel,
                currentPlayer.getBankTotalValue(),
                currentPlayer.getBank());
    }

    @Override
    public void refreshButtons() {
        refreshButtonStates(engineSupplier.get());
    }

    private void refreshButtonStates(GameEngine engine) {
        if (engine == null) {
            return;
        }
        boolean myTurn = !engine.isGameOver();
        if (drawCardBtn != null) {
            drawCardBtn.setDisable(!myTurn || !engine.canDrawCards());
        }
        if (discardCardBtn != null) {
            discardCardBtn.setDisable(!myTurn || !engine.hasDrawnThisTurn() || selectedCard == null);
        }
        if (endTurnBtn != null) {
            endTurnBtn.setDisable(!myTurn);
        }
    }
}
