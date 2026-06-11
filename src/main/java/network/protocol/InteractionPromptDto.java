package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端驱动的交互提示，通过 PROMPT 消息发送给一名客户端。
 * 客户端须以 RESPOND 回复，并携带相同的 {@link #promptId}。
 */
public class InteractionPromptDto {
    /** 本提示的唯一 id；RESPOND 消息中必填。 */
    public String promptId;

    /** JUST_SAY_NO 或 PAYMENT；见 {@link network.server.PendingActionResolution}。 */
    public String promptType;

    /** 须应答本提示的座位。 */
    public int responderSeat = -1;

    /** 发起行动的玩家座位。 */
    public int attackerSeat = -1;

    /** 发起行动的玩家的显示名称。 */
    public String attackerName;

    /** 客户端对话框中显示的简短描述。 */
    public String actionName;

    /** Just Say No 反制链深度（0 表示首次应答）。 */
    public int responseDepth;

    /** 支付提示的应付总额。 */
    public int amountDue;

    /** 部分支付后仍须支付的金额。 */
    public int remainingDue;

    /** 应答者可选择用于支付的卡牌。 */
    public List<CardDto> payableCards = new ArrayList<>();
}
