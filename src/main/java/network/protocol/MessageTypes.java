package network.protocol;

/**
 * 客户端与服务端消息 {@code type} 字段的字符串常量。
 * 线上协议为纯 JSON 对象，每行一条。
 */
public final class MessageTypes {
    private MessageTypes() {
    }

    // --- 客户端 -> 服务端命令 ---

    /** 携带显示名称进入大厅。 */
    public static final String JOIN = "JOIN";
    /** 主机请求开始比赛（仅大厅阶段）。 */
    public static final String START_GAME = "START_GAME";
    /** 当前玩家在回合开始时抽两张牌。 */
    public static final String DRAW = "DRAW";
    /** 打出、存入银行或应用行动/地产卡。 */
    public static final String PLAY_CARD = "PLAY_CARD";
    /** 更改牌面上已有万能地产的颜色。 */
    public static final String RECOLOR_WILD = "RECOLOR_WILD";
    /** 从手牌弃掉一张卡（如手牌上限）。 */
    public static final String DISCARD_CARD = "DISCARD_CARD";
    /** 结束当前玩家的回合。 */
    public static final String END_TURN = "END_TURN";
    /** 请求刷新本客户端的按座位 STATE 快照。 */
    public static final String SYNC = "SYNC";
    /** 断开与服务端的连接。 */
    public static final String LEAVE = "LEAVE";
    /** 应答服务端 PROMPT（Just Say No 或支付）。 */
    public static final String RESPOND = "RESPOND";
    /** 获胜后投票是否再来一局。 */
    public static final String REMATCH_VOTE = "REMATCH_VOTE";
    /** 向所有玩家广播表情反应。 */
    public static final String SEND_EMOJI = "SEND_EMOJI";
    /** 暂停所有玩家共享的回合计时器。 */
    public static final String PAUSE_GAME = "PAUSE_GAME";
    /** 暂停后恢复共享回合计时器。 */
    public static final String RESUME_GAME = "RESUME_GAME";

    // --- 服务端 -> 客户端响应与事件 ---

    /** 命令成功（常与 STATE 广播一起发送）。 */
    public static final String OK = "OK";
    /** 命令被拒绝；{@link ServerMessage#text} 说明原因。 */
    public static final String ERROR = "ERROR";
    /** 大厅加入成功；包含分配的座位。 */
    public static final String JOINED = "JOINED";
    /** 更新后的大厅玩家列表广播。 */
    public static final String LOBBY = "LOBBY";
    /** 比赛开始；包含初始 {@link GameStateDto}。 */
    public static final String GAME_STARTED = "GAME_STARTED";
    /** 本客户端座位的权威游戏快照。 */
    public static final String STATE = "STATE";
    /** 服务端需要一名客户端输入（JSN 或支付）。 */
    public static final String PROMPT = "PROMPT";
    /** 其他玩家的表情反应。 */
    public static final String EMOJI = "EMOJI";
}
