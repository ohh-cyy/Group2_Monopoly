package controller.dialog;

import model.card.Card;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 本地与网络控制器共用的手牌上限弃牌提示。
 * <p>
 * 构建一致的标题/摘要文本，并将 UI 渲染委托给调用方提供的
 * {@link Function}，以便各模式使用各自的 {@link controller.dialog.GameDialogService}。
 */
public final class HandDiscardDialogService {
    /** 工具类，禁止实例化。 */
    private HandDiscardDialogService() {
    }

    /**
     * 手牌超限时要求玩家弃掉一张牌。
     *
     * @param promptHandler 渲染对话框并返回所选卡牌
     * @param hand          当前手牌（防御性复制）
     * @param excess        本次选择后仍需弃牌的数量
     * @param endingTurn    提示文案是否提及结束回合
     * @return 校验后的选择；取消或无效时为空
     */
    public static Optional<Card> promptDiscardOne(
            Function<DiscardPrompt, Optional<Card>> promptHandler,
            List<Card> hand,
            int excess,
            boolean endingTurn) {
        Objects.requireNonNull(promptHandler, "promptHandler");
        if (hand == null || hand.isEmpty() || excess <= 0) {
            return Optional.empty();
        }

        List<Card> availableCards = List.copyOf(hand);
        String suffix = endingTurn
                ? " before your turn can end"
                : "";
        DiscardPrompt prompt = new DiscardPrompt(
                "Hand Limit",
                "You have too many cards in hand",
                "Choose a card to discard (" + excess + " more required" + suffix + "):",
                availableCards);

        Optional<Card> selected = promptHandler.apply(prompt);
        if (selected == null || selected.isEmpty() || !availableCards.contains(selected.get())) {
            return Optional.empty();
        }
        return selected;
    }

    /**
     * 传递给 UI 层用于单次弃牌选择的不可变数据包。
     *
     * @param title  对话框窗口标题
     * @param header 粗体摘要行
     * @param prompt 说明性正文
     * @param hand   可选卡牌（防御性复制）
     */
    public record DiscardPrompt(String title, String header, String prompt, List<Card> hand) {
        public DiscardPrompt {
            hand = List.copyOf(hand);
        }
    }
}
