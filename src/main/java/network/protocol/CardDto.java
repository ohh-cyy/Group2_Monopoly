package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link model.card.Card} 的网络表示。
 * {@link #cardKind} 用于区分具体卡牌类型，供 {@link network.CardMapper} 使用。
 */
public class CardDto {
    /** 卡牌实例唯一 id，用于出牌/弃牌/支付命令。 */
    public String id;

    /** 卡面显示名称。 */
    public String name;
    /** 卡牌简短规则或描述文本。 */
    public String description;

    /** {@link model.enums.CardType} 名称：PROPERTY、MONEY 或 ACTION。 */
    public String type;

    /** 细粒度种类，如 MONEY、PROPERTY、RENT、SLY_DEAL。 */
    public String cardKind;

    /** 当前或已选定的地产颜色名称。 */
    public String color;

    /** 金钱卡面值。 */
    public Integer money;

    /** 地产购买价格（如适用）。 */
    public Integer price;

    /** 行动卡或万能地产的银行存款价值。 */
    public Integer bankValue;

    /** 万能地产：放置前可选的合法颜色。 */
    public List<String> wildColors = new ArrayList<>();

    /** 万能地产：是否可存入银行。 */
    public Boolean bankable;

    /** 租金卡：是否为全色租金变体。 */
    public Boolean allColorsRent;

    /** 租金卡：双色租金适用的颜色名称。 */
    public List<String> rentColors = new ArrayList<>();
}
