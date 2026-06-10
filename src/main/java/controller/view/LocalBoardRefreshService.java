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
import ui.render.DeckPileRenderer;
import ui.render.HandRenderer;
import ui.render.PersonalBankRenderer;
import ui.render.PlayerBoardView;
import ui.render.PlayerListRenderer;
import ui.render.PublicBoardRenderOptions;
import ui.render.PublicBoardRenderer;
import ui.render.TurnFlowRenderer;
import ui.render.TurnStatusRenderer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * {@link GameBoardRefreshService} implementation driven by {@link GameEngine}.
 * <p>
 * Reads live engine and player objects via suppliers so the UI stays in sync with
 * local hot-seat state without network DTO mapping.
 */
public final class LocalBoardRefreshService implements GameBoardRefreshService {
    /** Top status bar and turn-flow widgets bound from FXML. */
    private final Label currentPlayerLabel;
    private final Label gameStatusText;
    private final Label turnFlowLabel;
    private final HBox playDotsBar;
    private final HBox deckPilesBar;
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

    /** Hand card highlighted for discard or play; mirrored from the controller. */
    private Card selectedCard;
    /** Captures the {@link CardView} when selection is reapplied after refresh. */
    private BiConsumer<Card, CardView> selectedViewCallback;
    /** Wild-property recolor click options when the current player may act. */
    private Supplier<PublicBoardRenderOptions> boardOptionsSupplier = () -> PublicBoardRenderOptions.none();
    /** Remaining local turn seconds for the status bar (-1 to hide). */
    private IntSupplier turnSecondsSupplier = () -> -1;

    /**
     * @param engineSupplier          live {@link GameEngine}
     * @param currentTurnSeatSupplier index of the player whose turn it is
     * @param boardViewsSupplier      prebuilt {@link ui.render.PlayerBoardView} list for the public board
     * @param handSelectionListener   click/double-click handler for hand cards
     */
    public LocalBoardRefreshService(Label currentPlayerLabel,
                                    Label gameStatusText,
                                    Label turnFlowLabel,
                                    HBox playDotsBar,
                                    HBox deckPilesBar,
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
        this.turnFlowLabel = turnFlowLabel;
        this.playDotsBar = playDotsBar;
        this.deckPilesBar = deckPilesBar;
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

    /** {@inheritDoc} */
    @Override
    public void setSelectedCard(Card selectedCard) {
        this.selectedCard = selectedCard;
    }

    /** {@inheritDoc} */
    @Override
    public void applySelectionCallback(BiConsumer<Card, CardView> callback) {
        this.selectedViewCallback = callback;
    }

    /** Supplies wild-property recolor options when the current player may act. */
    public void setBoardOptionsSupplier(Supplier<PublicBoardRenderOptions> boardOptionsSupplier) {
        this.boardOptionsSupplier = boardOptionsSupplier != null
                ? boardOptionsSupplier
                : () -> PublicBoardRenderOptions.none();
    }

    /** Supplies remaining turn seconds for the local countdown display (-1 to hide). */
    public void setTurnSecondsSupplier(IntSupplier turnSecondsSupplier) {
        this.turnSecondsSupplier = turnSecondsSupplier != null ? turnSecondsSupplier : () -> -1;
    }

    /** Updates turn labels and play dots without rebuilding the hand (avoids breaking card animations). */
    public void refreshTurnStatusOnly() {
        refreshTurnStatus(engineSupplier.get(), currentPlayerSupplier.get());
    }

    /** {@inheritDoc} */
    @Override
    public void refreshAll(VBox playersList, Consumer<Double> rowHeightConsumer) {
        GameEngine engine = engineSupplier.get();
        Player currentPlayer = currentPlayerSupplier.get();
        playerListRenderer.render(playersList, playersSupplier.get(),
                engine != null ? engine.getCurrentPlayer() : null);
        refreshTurnStatus(engine, currentPlayer);
        double rowHeight = publicBoardRenderer.render(
                publicBoardPanel, allPlayersPropertiesPanel,
                boardViewsSupplier.get(), currentTurnSeatSupplier.getAsInt(),
                boardOptionsSupplier.get());
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
                engine.isGameOver(),
                turnSecondsSupplier.getAsInt());
        TurnFlowRenderer.render(
                turnFlowLabel,
                playDotsBar,
                engine.hasDrawnThisTurn(),
                engine.getRemainingPlays(),
                TurnFlowRenderer.defaultMaxPlays(),
                engine.isGameOver());
        DeckPileRenderer.render(
                deckPilesBar,
                engine.getDeck().size(),
                engine.getDiscardPile().size(),
                engine.isGameOver());
    }

    /** {@inheritDoc} */
    @Override
    public void disableActionButtons() {
        if (drawCardBtn != null) {
            drawCardBtn.setDisable(true);
        }
        if (discardCardBtn != null) {
            discardCardBtn.setDisable(true);
        }
        if (endTurnBtn != null) {
            endTurnBtn.setDisable(true);
        }
    }

    private void refreshHand(GameEngine engine, Player currentPlayer) {
        if (currentPlayer == null) {
            if (playerHand != null) {
                playerHand.getChildren().clear();
            }
            return;
        }
        boolean handInteractive = engine != null && !engine.isGameOver();
        boolean handSelectable = handInteractive && engine.hasDrawnThisTurn();
        handRenderer.render(playerHand, playerHandScroll, handSupplier.get(), selectedCard,
                handInteractive, handSelectable, handSelectionListener);
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

    /** {@inheritDoc} */
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
