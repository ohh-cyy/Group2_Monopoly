package network.protocol;

import java.util.ArrayList;
import java.util.List;

/** 发给某一客户端的游戏快照 */
public class GameStateDto {
    public int yourSeat;
    public int currentPlayerIndex;
    public boolean hasDrawnThisTurn;
    public int remainingPlays;
    public boolean gameOver;
    public String winnerName;
    public int drawPileSize;
    public int discardPileSize;
    public List<PlayerViewDto> players = new ArrayList<>();
}
