package network.protocol;

/** 客户端发送的 JSON 消息 */
public class ClientMessage {
    public String type;
    public String playerName;
    /** 出牌目标卡牌 instanceId */
    public String cardId;
    /** PLAY | BANK | PROPERTY */
    public String mode;
    /** 万能地产选色，如 RED */
    public String color;
}
