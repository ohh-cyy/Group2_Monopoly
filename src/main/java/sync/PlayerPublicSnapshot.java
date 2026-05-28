package sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayerPublicSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public int seat;
    public String name;
    public int handSize;
    public int bankTotal;
    public List<CardSnapshot> properties = new ArrayList<>();
}
