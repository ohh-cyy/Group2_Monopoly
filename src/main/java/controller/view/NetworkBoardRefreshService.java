package controller.view;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import model.card.Card;
import network.protocol.GameStateDto;
import network.protocol.PlayerViewDto;
import ui.render.BankBarRenderer;
import ui.render.DeckPileRenderer;
import ui.render.HandRenderer;
import ui.render.PersonalBankRenderer;
import ui.render.PlayerBoardView;
import ui.render.PlayerListRenderer;
import ui.render.PublicBoardRenderOptions;
import ui.render.PublicBoardRenderer;
import ui.render.TurnFlowRenderer;
import ui.render.TurnDeadlineClock;
import ui.render.TurnStatusRenderer;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link GameBoardRefreshService} implementation driven by {@link GameStateDto}.
 * <p>
 * Suppliers provide the latest server snapshot, mapped hand/bank, and local seat so
 * the controller can refresh the board after each sync without duplicating render code.
 */
public final class NetworkBoardRefreshService implements GameBoardRefreshService {
    /** Plays allowed per turn in online mode (matches server rules). */
    private static final int MAX_PLAYS_PER_TURN = 3;

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

    private final Supplier<GameStateDto> stateSupplier;
    private final Supplier<List<Card>> handSupplier;
    private final Supplier<List<Card>> bankSupplier;
    private final Supplier<Integer> localSeatSupplier;
    private final HandRenderer.SelectionListener handSelectionListener;

    /** Hand card highlighted for discard or play; mirrored from the controller. */
    private Card selectedCard;
    /** Captures the {@link ui.CardView} when selection is reapplied after refresh. */
    private BiConsumer<Card, ui.CardView> selectedViewCallback;
    /** Wild-property recolor click options when it is the local player's turn. */
    private Supplier<PublicBoardRenderOptions> boardOptionsSupplier = () -> PublicBoardRenderOptions.none();

    /**
     * @param stateSupplier       authoritative server snapshot
     * @param handSupplier        mapped cards in the local player's hand
     * @param bankSupplier        mapped cards in the local player's bank
     * @param localSeatSupplier   this client's seat index
     * @param handSelectionListener click/double-click handler for hand cards
     */
    public NetworkBoardRefreshService(Label currentPlayerLabel,
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
                                        Supplier<GameStateDto> stateSupplier,
                                        Supplier<List<Card>> handSupplier,
                                        Supplier<List<Card>> bankSupplier,
                                        Supplier<Integer> localSeatSupplier,
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
        this.stateSupplier = stateSupplier;
        this.handSupplier = handSupplier;
        this.bankSupplier = bankSupplier;
        this.localSeatSupplier = localSeatSupplier;
        this.handSelectionListener = handSelectionListener;
    }

    /** {@inheritDoc} */
    @Override
    public void setSelectedCard(Card selectedCard) {
        this.selectedCard = selectedCard;
    }

    /** {@inheritDoc} */
    @Override
    public void applySelectionCallback(BiConsumer<Card, ui.CardView> callback) {
        this.selectedViewCallback = callback;
    }

    /** Supplies wild-property recolor options when it is the local player's turn. */
    public void setBoardOptionsSupplier(Supplier<PublicBoardRenderOptions> boardOptionsSupplier) {
        this.boardOptionsSupplier = boardOptionsSupplier != null
                ? boardOptionsSupplier
                : () -> PublicBoardRenderOptions.none();
    }

    /** Updates only the turn summary labels (used by the online countdown ticker). */
    public void refreshTurnStatus() {
        GameStateDto state = stateSupplier.get();
        if (state == null) {
            return;
        }
        refreshLabels(state);
    }

    /** {@inheritDoc} */
    @Override
    public void refreshAll(VBox playersList, Consumer<Double> rowHeightConsumer) {
        GameStateDto state = stateSupplier.get();
        if (state == null) {
            return;
        }
        int localSeat = localSeatSupplier.get();
        List<PlayerBoardView> boardViews = PlayerBoardView.fromDtos(state.players, localSeat);
        playerListRenderer.renderBoardViews(playersList, boardViews, state.currentPlayerIndex);
        double rowHeight = publicBoardRenderer.render(
                publicBoardPanel, allPlayersPropertiesPanel, boardViews, state.currentPlayerIndex,
                boardOptionsSupplier.get());
        if (rowHeightConsumer != null && rowHeight > 0) {
            rowHeightConsumer.accept(rowHeight);
        }
        bankBarRenderer.renderBoardViews(allPlayersBankBar, boardViews, state.currentPlayerIndex);
        refreshHand(state);
        refreshPersonalBank(state);
        refreshLabels(state);
        refreshButtons(state);
    }

    private void refreshHand(GameStateDto state) {
        boolean myTurn = isMyTurn(state);
        boolean handInteractive = myTurn && state != null && !state.gameOver;
        boolean handSelectable = handInteractive && state.hasDrawnThisTurn;
        handRenderer.render(playerHand, playerHandScroll, handSupplier.get(), selectedCard,
                handInteractive, handSelectable, handSelectionListener);
        if (selectedCard != null && selectedViewCallback != null) {
            handRenderer.applySelection(playerHand, selectedCard, selectedViewCallback);
        }
    }

    private void refreshPersonalBank(GameStateDto state) {
        int total = 0;
        for (PlayerViewDto p : state.players) {
            if (p.seat == localSeatSupplier.get()) {
                total = p.bankTotal;
                break;
            }
        }
        personalBankRenderer.render(playerBank, bankTotalLabel, total, bankSupplier.get());
    }

    private void refreshLabels(GameStateDto state) {
        String currentName = "";
        if (state.currentPlayerIndex >= 0 && state.currentPlayerIndex < state.players.size()) {
            currentName = state.players.get(state.currentPlayerIndex).name;
        }
        int turnSeconds = state.gamePaused
                ? state.pausedTurnSecondsRemaining
                : TurnDeadlineClock.secondsRemaining(state.turnDeadlineEpochMillis);
        turnStatusRenderer.renderOnline(
                currentPlayerLabel,
                gameStatusText,
                currentName,
                state.hasDrawnThisTurn,
                state.remainingPlays,
                MAX_PLAYS_PER_TURN,
                state.drawPileSize,
                state.discardPileSize,
                state.gameOver,
                state.winnerName,
                turnSeconds);
        TurnFlowRenderer.render(
                turnFlowLabel,
                playDotsBar,
                state.hasDrawnThisTurn,
                state.remainingPlays,
                MAX_PLAYS_PER_TURN,
                state.gameOver);
        DeckPileRenderer.render(
                deckPilesBar,
                state.drawPileSize,
                state.discardPileSize,
                state.gameOver);
    }

    /** {@inheritDoc} */
    @Override
    public void refreshButtons() {
        GameStateDto state = stateSupplier.get();
        if (state == null) {
            return;
        }
        refreshButtons(state);
    }

    private void refreshButtons(GameStateDto state) {
        if (state.gameOver) {
            disableActionButtons();
            return;
        }
        boolean myTurn = isMyTurn(state);
        if (drawCardBtn != null) {
            drawCardBtn.setDisable(!myTurn || state.hasDrawnThisTurn);
        }
        if (discardCardBtn != null) {
            discardCardBtn.setDisable(!myTurn || !state.hasDrawnThisTurn || selectedCard == null);
        }
        if (endTurnBtn != null) {
            endTurnBtn.setDisable(!myTurn);
        }
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

    private boolean isMyTurn(GameStateDto state) {
        return state != null
                && !state.gameOver
                && state.currentPlayerIndex == localSeatSupplier.get();
    }
}
