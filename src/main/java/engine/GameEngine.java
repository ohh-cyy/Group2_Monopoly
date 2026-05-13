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
    private Player winner;

    public GameEngine(List<Player> players, Deck deck) {
        this.players = players;
        this.deck = deck;
        this.discardPile = new DiscardPile();
        this.currentPlayerIndex = 0;
    }

    public void startGame() {
        int initialHandSize = RuleBook.getInitialHandSize();
        for (Player p : players) {
            for (int i = 0; i < initialHandSize; i++) {
                if (!deck.isEmpty()) {
                    p.draw(deck.draw());
                }
            }
        }
    }

    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public void playTurn() {
        if (gameOver) {
            return;
        }

        Player player = getCurrentPlayer();
        System.out.println("=== " + player.getName() + "'s Turn ===");

        // 1. 抽牌阶段
        int cardsToDraw = RuleBook.getCardsPerTurn();
        for (int i = 0; i < cardsToDraw; i++) {
            if (!deck.isEmpty()) {
                Card drawnCard = deck.draw();
                player.draw(drawnCard);
                System.out.println(player.getName() + " drew: " + drawnCard.getName());
            } else {
                reshuffleFromDiscard();
                if (!deck.isEmpty()) {
                    player.draw(deck.draw());
                }
            }
        }

        // 2. 出牌阶段（简化版 - 自动出第一张牌）
        if (!player.getHand().isEmpty()) {
            Card card = player.getHand().get(0);
            System.out.println(player.getName() + " plays: " + card.getName());
            
            card.use(player, this);
            player.removeFromHand(card);
        }

        // 3. 检查胜利条件
        if (checkWin(player)) {
            winner = player;
            gameOver = true;
            System.out.println("=== GAME OVER === " + player.getName() + " wins!");
            return;
        }

        // 4. 下一回合
        nextTurn();
    }

    public void nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }

    public boolean checkWin(Player player) {
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
            int required = RuleBook.getRequiredCountForColor(color);

            if (count >= required) {
                completeSets++;
            }
        }

        return completeSets >= RuleBook.getWinningSetCount();
    }

    private void reshuffleFromDiscard() {
        if (discardPile.isEmpty()) {
            System.out.println("Both deck and discard pile are empty!");
            return;
        }

        List<Card> discardedCards = discardPile.getAllCards();
        discardPile.clear();
        
        deck.reshuffle(discardedCards);
        System.out.println("Reshuffled " + discardedCards.size() + " cards from discard pile");
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Player getWinner() {
        return winner;
    }

    public DiscardPile getDiscardPile() {
        return discardPile;
    }

    public Deck getDeck() {
        return deck;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
}