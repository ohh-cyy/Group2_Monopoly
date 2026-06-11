package network.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * 联机对局中一名玩家的公开视图。
 * 对手仅能看到手牌数量，看不到具体手牌。
 */
public class PlayerViewDto {
    /** 从 0 开始的座位索引。 */
    public int seat;

    /** 大厅中选择的显示名称。 */
    public String name;

    /** 手牌数量（内容隐藏）。 */
    public int handSize;

    /** 玩家银行中卡牌的总价值。 */
    public int bankTotal;

    /** 玩家牌面上的明牌地产。 */
    public List<CardDto> properties = new ArrayList<>();
}
