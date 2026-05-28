package sync;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoomPublicSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public long version;
    public int currentPlayerIndex;
    public boolean hasDrawnThisTurn;
    public int remainingPlays;
    public boolean gameOver;
    public String winnerName;
    public int drawPileSize;
    public int discardPileSize;
    public List<PlayerPublicSnapshot> players = new ArrayList<>();
    public List<String> logLines = new ArrayList<>();
}
