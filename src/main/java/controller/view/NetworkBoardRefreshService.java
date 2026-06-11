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
 * 由 {@link GameStateDto} 驱动的 {@link GameBoardRefreshService} 实现。
 * <p>
 * 供应器提供最新服务器快照、映射手牌/银行与本地座位，
 * 使控制器在每次同步后刷新棋盘而无需重复渲染代码。
 */
public final class NetworkBoardRefreshService implements GameBoardRefreshService {
    /** 联机模式每回合允许的出牌次数（与服务器规则一致）。 */
    private static final int MAX_PLAYS_PER_TURN = 3;

    /** 自 FXML 绑定的顶部状态栏与回合流程控件。 */
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

    /** 高亮用于弃牌或出牌的手牌；与控制器同步。 */
    private Card selectedCard;
    /** 刷新后重新应用选中时捕获 {@link ui.CardView}。 */
    private BiConsumer<Card, ui.CardView> selectedViewCallback;
    /** 轮到本地玩家时的万能地产改色点击选项。 */
    private Supplier<PublicBoardRenderOptions> boardOptionsSupplier = () -> PublicBoardRenderOptions.none();

    /**
     * @param stateSupplier         权威服务器快照
     * @param handSupplier          本地玩家手牌的映射卡牌
     * @param bankSupplier          本地玩家银行的映射卡牌
     * @param localSeatSupplier     本客户端座位索引
     * @param handSelectionListener 手牌单击/双击处理器
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

    /** 轮到本地玩家时提供万能地产改色选项。 */
    public void setBoardOptionsSupplier(Supplier<PublicBoardRenderOptions> boardOptionsSupplier) {
        this.boardOptionsSupplier = boardOptionsSupplier != null
                ? boardOptionsSupplier
                : () -> PublicBoardRenderOptions.none();
    }

    /** 仅更新回合摘要标签（供联机倒计时滴答使用）。 */
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
