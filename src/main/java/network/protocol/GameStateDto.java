package network.protocol;

import java.util.ArrayList;
import java.util.List;

public class GameStateDto {
    public int currentPlayerIndex;
    public boolean hasDrawnThisTurn;
    public int remainingPlays;
    public boolean gameOver;
    public String winnerName;
    public int drawPileSize;
    public int discardPileSize;
    public List<String> logLines = new ArrayList<>();
    public List<PlayerViewDto> players = new ArrayList<>();
    public List<CardDto> myHand = new ArrayList<>();
    public List<CardDto> myBank = new ArrayList<>();
}
