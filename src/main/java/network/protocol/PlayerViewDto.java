package network.protocol;

import java.util.ArrayList;
import java.util.List;

/** 某玩家在客户端可见的信息（手牌仅本人完整） */
public class PlayerViewDto {
    public int seat;
    public String name;
    public int handSize;
    public int bankTotal;
    public int propertyCount;
    public boolean you;
    public List<CardDto> hand;
    public List<CardDto> bank = new ArrayList<>();
    public List<CardDto> properties = new ArrayList<>();
}
