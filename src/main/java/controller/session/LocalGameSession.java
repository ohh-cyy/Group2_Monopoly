package controller.session;

import engine.Deck;
import engine.DeckFactory;
import engine.GameEngine;
import model.card.Card;
import model.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地热座游戏对 {@link GameEngine} 的轻量门面。
 * <p>
 * 持有玩家列表与引擎构建，避免 {@link controller.GameController}
 * 在 UI 层散落牌组初始化与回合变更逻辑。
 */
public final class LocalGameSession {
    private GameEngine gameEngine;
    private List<Player> players = List.of();

    /**
     * 创建玩家，洗牌，开始游戏
     */
    public void startNewGame(List<String> playerNames) {
        players = new ArrayList<>();
        for (String name : playerNames) {
            players.add(new Player(name));
        }
        List<Card> cardList = DeckFactory.createFullDeck();
        Deck deck = new Deck(cardList);
        gameEngine = new GameEngine(players, deck);
        gameEngine.startGame();
    }

    /** 返回实时引擎；{@link #startNewGame} 之前为 {@code null}。 */
    public GameEngine getEngine() {
        return gameEngine;
    }

    /** 游戏开始时创建的座位的不可变视图。 */
    public List<Player> getPlayers() {
        return players;
    }

    /** 当前回合持有者；无进行中的游戏时为 {@code null}。 */
    public Player getCurrentPlayer() {
        return gameEngine != null ? gameEngine.getCurrentPlayer() : null;
    }

    /** 存在未结束的游戏时为 {@code true}。 */
    public boolean isActive() {
        return gameEngine != null && !gameEngine.isGameOver();
    }

    /** 若回合允许，为当前玩家摸两张牌。 */
    public boolean drawForCurrentPlayer() {
        return gameEngine != null && gameEngine.drawCardsForCurrentPlayer();
    }

    /** 从指定玩家手牌移除一张牌并放入弃牌堆。 */
    public boolean discardFromHand(Player player, Card card) {
        return gameEngine != null && gameEngine.discardFromHand(player, card);
    }

    /** 玩家是否可主动结束回合（手牌数量在限制内等）。 */
    public boolean canEndTurn(Player player) {
        return gameEngine != null && gameEngine.canEndTurn(player);
    }

    /** 推进到下一座位并重置每回合摸牌/出牌计数。 */
    public void nextTurn() {
        if (gameEngine != null) {
            gameEngine.nextTurn();
        }
    }

    /** 检查玩家的标准三完整套组胜利条件。 */
    public boolean checkWin(Player player) {
        return gameEngine != null && gameEngine.checkWin(player);
    }

    /** 强制设置或清除引擎上的游戏结束标志。 */
    public void setGameOver(boolean gameOver) {
        if (gameEngine != null) {
            gameEngine.setGameOver(gameOver);
        }
    }

    /** 消耗每回合允许的三次出牌之一。 */
    public void recordCardPlayed() {
        if (gameEngine != null) {
            gameEngine.recordCardPlayed();
        }
    }
}
