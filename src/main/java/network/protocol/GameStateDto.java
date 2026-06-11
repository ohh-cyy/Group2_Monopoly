package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * 联机对局的可序列化快照，包含在 STATE / GAME_STARTED / PROMPT 中。
 * {@link #myHand} 与 {@link #myBank} 按座位区分；其余字段为共享数据。
 */
public class GameStateDto {
    /** 当前回合玩家的索引。 */
    public int currentPlayerIndex;

    /** 当前玩家本回合是否已抽牌。 */
    public boolean hasDrawnThisTurn;

    /** 本回合剩余可出牌次数（最多 3 次）。 */
    public int remainingPlays;

    /** 已决出胜者时为 true。 */
    public boolean gameOver;

    /** {@link #gameOver} 为 true 时的获胜玩家名称。 */
    public String winnerName;

    /** 获胜后玩家可投票是否再来一局时为 true。 */
    public boolean rematchOpen;

    /** 投票同意再来一局的玩家数。 */
    public int rematchYesCount;

    /** 再来一局所需的全票同意玩家总数。 */
    public int rematchRequired;

    /** 至少一名玩家拒绝再来一局时为 true。 */
    public boolean rematchDeclined;

    /** 本客户端的再来一局投票：null=未投票，true/false=选择。 */
    public Boolean myRematchVote;

    /** 抽牌堆剩余卡牌数。 */
    public int drawPileSize;

    /** 弃牌堆卡牌数。 */
    public int discardPileSize;

    /** 当前回合结束的纪元毫秒时间戳；无计时器时为 0。 */
    public long turnDeadlineEpochMillis;

    /** 回合计时器对所有玩家暂停时为 true。 */
    public boolean gamePaused;

    /** {@link #gamePaused} 为 true 时的剩余秒数。 */
    public int pausedTurnSecondsRemaining;

    /** 完整会话日志行，新行追加在末尾。 */
    public List<String> logLines = new ArrayList<>();

    /** 各座位玩家的公开信息。 */
    public List<PlayerViewDto> players = new ArrayList<>();

    /** 仅接收客户端可见的完整手牌。 */
    public List<CardDto> myHand = new ArrayList<>();

    /** 仅接收客户端可见的完整银行。 */
    public List<CardDto> myBank = new ArrayList<>();
}
