package controller.view;

import javafx.scene.layout.VBox;
import model.card.Card;
import ui.CardView;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 将共享棋盘控件与游戏状态同步的抽象接口。
 * <p>
 * 由 {@link LocalBoardRefreshService} 与 {@link NetworkBoardRefreshService} 实现，
 * 使控制器刷新 UI 时无需按模式分支。
 */
public interface GameBoardRefreshService {
    /** 记录高亮手牌，供渲染器与按钮启用逻辑使用。 */
    void setSelectedCard(Card selectedCard);

    /** 注册回调，在重新应用选中时捕获 {@link CardView}。 */
    void applySelectionCallback(BiConsumer<Card, CardView> callback);

    /**
     * 完整棋盘重绘：玩家列表、地产、手牌、银行、标签与按钮。
     *
     * @param rowHeightConsumer 可选回调，传入最高地产行高度
     */
    void refreshAll(VBox playersList, Consumer<Double> rowHeightConsumer);

    /** 仅重新计算摸牌/弃牌/结束回合按钮的禁用状态。 */
    void refreshButtons();

    /** 禁用所有回合操作按钮（如游戏结束后）。 */
    default void disableActionButtons() {
        // 本地游戏在刷新时仍由回合规则管理按钮。
    }
}
