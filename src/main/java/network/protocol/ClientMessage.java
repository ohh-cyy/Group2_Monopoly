package network.protocol;

/**
 * 游戏客户端发往服务端的出站消息。
 * 仅需设置与 {@link #type} 相关的字段。
 */
public class ClientMessage {
    /** 命令名称；见 {@link MessageTypes}。 */
    public String type;

    /** JOIN：大厅输入的显示名称。 */
    public String playerName;

    /** JOIN：本客户端是否启动了内嵌主机服务端。 */
    public boolean host;

    /** 出牌、弃牌或万能地产改色命令的卡牌实例 id。 */
    public String cardId;

    /** PLAY_CARD 模式：PLAY、BANK、EFFECT、PROPERTY 或 DOUBLE_RENT。 */
    public String mode;

    /** 地产或租金颜色名称（枚举 {@code Color} 的字符串形式）。 */
    public String color;

    /** 偷牌、讨债、成交破坏等行动的目标对手座位。 */
    public Integer targetSeat;

    /** 偷牌或强制交易命令的目标地产卡牌 id。 */
    public String targetCardId;

    /** 第二张卡牌 id，如与 Double the Rent 搭配的租金卡。 */
    public String secondCardId;

    /** RESPOND：从 {@link InteractionPromptDto#promptId} 复制的 id。 */
    public String promptId;

    /** RESPOND（JUST_SAY_NO）：true 表示打出 Just Say No 卡。 */
    public Boolean useJustSayNo;

    /** RESPOND（PAYMENT）：用于支付的银行卡或地产实例 id。 */
    public String paymentCardId;

    /** REMATCH_VOTE：true 表示再来一局，false 表示拒绝。 */
    public Boolean acceptRematch;

    /** SEND_EMOJI：表情文本；须在 {@link EmojiCatalog} 中。 */
    public String emoji;
}
