package sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 仅对应座位玩家可见 */
public class PlayerPrivateSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public int seat;
    public List<CardSnapshot> hand = new ArrayList<>();
    public List<CardSnapshot> bank = new ArrayList<>();
}
