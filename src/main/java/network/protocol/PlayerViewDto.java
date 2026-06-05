package network.protocol;

import java.util.ArrayList;
import java.util.List;

public class PlayerViewDto {
    public int seat;
    public String name;
    public int handSize;
    public int bankTotal;
    public List<CardDto> properties = new ArrayList<>();
}
