package engine;

import model.card.Card;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.*;

public class GameEngine {
    private List<Player> players;
    private Deck deck;
    private DiscardPile discardPile;
    private int currentPlayerIndex;
    private boolean gameOver = false;

    public GameEngine(List<Player> players, Deck deck) {
        this.players = players;
        this.deck = deck;
        this.currentPlayerIndex = 0;
        discardPile = new DiscardPile();
    }

    // 开始游戏
    public void startGame() {
        // 每人发5张牌
        for (Player p : players) {
            for (int i = 0; i < 5; i++) {
                p.draw(deck.draw());
            }
        }
    }

    // 当前玩家
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public void drawCard(Card card) {

    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    // 胜利条件（先简单）
    public boolean checkWin(Player player) {
        // key: 颜色, value: 该颜色的卡数量
        Map<Color, Integer> colorCount = new HashMap<>();

        for (PropertyCard card : player.getProperties()) {
            Color color = card.getColor();
            if (color != null) {
                colorCount.put(color, colorCount.getOrDefault(color, 0) + 1);
            }
        }

        int completeSets = 0;

        for (Color color : colorCount.keySet()) {
            int count = colorCount.get(color);

            // 👉 每种颜色需要的数量（示例）
            int required = getRequiredCount(color);

            if (count >= required) {
                completeSets++;
            }
        }

        return completeSets >= 3;
    }

    private int getRequiredCount(Color color) {
        // 根据不同颜色返回需要的地产数量
        switch (color) {
            case BROWN:
            case DARK_BLUE:
                return 2;
            default:
                return 3;
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Player getDefaultDefender(Player attacker) {
        int index = players.indexOf(attacker);
        int defenderIndex = (index + 1) % players.size();
        return players.get(defenderIndex);

    }
    
    public List<Player> getPlayers() {
        return players;
    }
    
    public DiscardPile getDiscardPile() {
        return discardPile;
    }
    public Deck getDeck() {
        return deck;
    }
}