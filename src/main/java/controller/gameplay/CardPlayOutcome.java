package controller.gameplay;

/**
 * 本地模式尝试出牌后的不可变结果。
 * <p>
 * 由 {@link LocalCardPlayService} 返回，供 {@link controller.GameController}
 * 执行动画、记录日志与计数出牌，无需重新推导效果细节。
 */
public final class CardPlayOutcome {
    /** 出牌是否成功、失败、取消或被阻挡。 */
    public final ActionEffectResult result;
    /** 卡牌存入银行而非使用效果时为 {@code true}。 */
    public final boolean depositedToBank;
    /** 须消耗第二次出牌时为 {@code true}（如 Double the Rent）。 */
    public final boolean consumesExtraPlay;

    /**
     * @param result            效果解析状态
     * @param depositedToBank   是否存入银行而非使用
     * @param consumesExtraPlay 是否应消耗第二次出牌计数
     */
    public CardPlayOutcome(ActionEffectResult result, boolean depositedToBank, boolean consumesExtraPlay) {
        this.result = result;
        this.depositedToBank = depositedToBank;
        this.consumesExtraPlay = consumesExtraPlay;
    }

    /** 玩家关闭出牌对话框时的便捷结果。 */
    public static CardPlayOutcome cancelled() {
        return new CardPlayOutcome(ActionEffectResult.CANCELLED, false, false);
    }
}
