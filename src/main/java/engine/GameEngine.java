package engine;

import model.card.Card;
import model.card.PropertyCard;
import model.enums.Color;
import model.player.Player;

import java.util.*;

public class GameEngine {
    private List<Player> players;
    private Deck deck;
    private int currentPlayerIndex;
    private boolean gameOver = false;

    public GameEngine(List<Player> players, Deck deck) {
        this.players = players;
        this.deck = deck;
        this.currentPlayerIndex = 0;
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

    // 一回合
    public void playTurn() {
        Player player = getCurrentPlayer();

        System.out.println("Current Player: " + player.getName());

        // 1. 抽2张牌
        for (int i = 0; i < 2; i++) {
            player.draw(deck.draw());
        }

        // 2. 出牌逻辑（尚未完成）
        if (!player.getHand().isEmpty()) {
            Card card = player.getHand().get(0);

            System.out.println(player.getName() + " uses " + card.getName());

            card.use(player, this); // 调用Controller

            player.removeFromHand(card);
        }

        // 3. 检查胜利
        if (checkWin(player)) {
            System.out.println(player.getName() + " wins!");
            gameOver = true;
            return;
        }

        // 4. 下一回合
        nextTurn();
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
            colorCount.put(color, colorCount.getOrDefault(color, 0) + 1);
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

    public boolean isGameOver() {
        return gameOver;
    }
}