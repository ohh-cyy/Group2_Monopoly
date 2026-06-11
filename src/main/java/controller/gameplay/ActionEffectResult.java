package controller.gameplay;

/**
 * 本地游戏中行动卡效果解析的结果。
 * <p>
 * 由 {@link CardPlayOutcome} 与 {@link LocalCardPlayService} 消费，
 * 用于决定日志、弃牌行为及是否消耗额外出牌。
 */
public enum ActionEffectResult {
    /** 效果已生效（或部分生效，如收租时部分被阻挡）。 */
    SUCCESS,
    /** 目标或前置条件无效；卡牌仍可能被弃掉。 */
    FAILED,
    /** 玩家取消了对话框；卡牌保留在手牌。 */
    CANCELLED,
    /** 效果被 Just Say No 响应链终止。 */
    BLOCKED
}
