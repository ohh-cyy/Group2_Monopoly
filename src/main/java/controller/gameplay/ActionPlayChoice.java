package controller.gameplay;

/**
 * 打出行动卡或可存银行的万能地产卡时玩家的决策。
 * <p>
 * 在效果解析或存银行前由 {@link StandardCardPlayPrompts} 呈现。
 */
public enum ActionPlayChoice {
    /** 执行卡牌的游戏效果（收租、偷窃等）。 */
    USE_EFFECT,
    /** 跳过效果，按 M 值将卡牌存入玩家银行。 */
    DEPOSIT_BANK
}
