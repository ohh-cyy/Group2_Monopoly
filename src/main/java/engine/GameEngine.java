package engine;

import model.player.Player;
import model.card.Card;

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

        // 2. 简化：自动出1张牌（后面UI再改）
        if (!player.getHand().isEmpty()) {
            Card card = player.getHand().get(0);

            System.out.println(player.getName() + " uses " + card.getName());

            card.use(player, this); // ⭐调用Controller

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
        return player.getProperties().size() >= 3;
    }

    public boolean isGameOver() {
        return gameOver;
    }
}