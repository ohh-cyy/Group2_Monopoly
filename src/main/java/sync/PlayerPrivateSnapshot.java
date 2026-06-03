package sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Private data visible only to the matching seat. */
public class PlayerPrivateSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public int seat;
    public List<CardSnapshot> hand = new ArrayList<>();
    public List<CardSnapshot> bank = new ArrayList<>();
}
