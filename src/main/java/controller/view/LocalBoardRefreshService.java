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
 * 由 {@link GameEngine} 驱动的 {@link GameBoardRefreshService} 实现。
 * <p>
 * 通过供应器读取实时引擎与玩家对象，使 UI 与本地热座状态同步，
 * 无需网络 DTO 映射。
 */
public final class LocalBoardRefreshService implements GameBoardRefreshService {
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

    private final Supplier<GameEngine> engineSupplier;
    private final Supplier<List<Player>> playersSupplier;
    private final Supplier<Player> currentPlayerSupplier;
    private final Supplier<List<Card>> handSupplier;
    private final IntSupplier currentTurnSeatSupplier;
    private final Supplier<List<PlayerBoardView>> boardViewsSupplier;
    private final HandRenderer.SelectionListener handSelectionListener;

    /** 高亮用于弃牌或出牌的手牌；与控制器同步。 */
    private Card selectedCard;
    /** 刷新后重新应用选中时捕获 {@link CardView}。 */
    private BiConsumer<Card, CardView> selectedViewCallback;
    /** 当前玩家可操作时的万能地产改色点击选项。 */
    private Supplier<PublicBoardRenderOptions> boardOptionsSupplier = () -> PublicBoardRenderOptions.none();
    /** 状态栏显示的本地回合剩余秒数（-1 表示隐藏）。 */
    private IntSupplier turnSecondsSupplier = () -> -1;

    /**
     * @param engineSupplier          实时 {@link GameEngine}
     * @param currentTurnSeatSupplier 当前回合玩家索引
     * @param boardViewsSupplier      公共棋盘预构建的 {@link ui.render.PlayerBoardView} 列表
     * @param handSelectionListener   手牌单击/双击处理器
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

    /** 当前玩家可操作时提供万能地产改色选项。 */
    public void setBoardOptionsSupplier(Supplier<PublicBoardRenderOptions> boardOptionsSupplier) {
        this.boardOptionsSupplier = boardOptionsSupplier != null
                ? boardOptionsSupplier
                : () -> PublicBoardRenderOptions.none();
    }

    /** 提供本地倒计时显示的剩余回合秒数（-1 表示隐藏）。 */
    public void setTurnSecondsSupplier(IntSupplier turnSecondsSupplier) {
        this.turnSecondsSupplier = turnSecondsSupplier != null ? turnSecondsSupplier : () -> -1;
    }

    /** 仅更新回合标签与出牌点，不重建手牌（避免打断卡牌动画）。 */
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
