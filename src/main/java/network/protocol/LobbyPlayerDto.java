package network.protocol;

/**
 * 赛前大厅玩家列表中的一条记录。
 */
public class LobbyPlayerDto {
    public int seat;
    public String name;

    /** 该玩家是否可以开始比赛。 */
    public boolean host;

    /** JOIN 成功完成后为 true。 */
    public boolean joined;
}
