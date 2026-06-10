package ui.render;

import javafx.scene.control.Label;

/**
 * Updates turn summary labels for local and online game boards.
 * <p>
 * Draw and discard pile counts are accepted for API symmetry but are rendered
 * elsewhere via {@link DeckPileRenderer}.
 */
public final class TurnStatusRenderer {
    /**
     * Updates local-game turn labels without a turn timer.
     *
     * @param currentPlayerLabel label that shows whose turn it is
     * @param gameStatusText     secondary status line; cleared while the game is active
     * @param playerName         name of the player whose turn it is
     * @param hasDrawnThisTurn   whether the current player has drawn this turn
     * @param remainingPlays     plays still available after drawing
     * @param drawPileSize       draw pile size (not updated here)
     * @param discardPileSize    discard pile size (not updated here)
     * @param gameOver           when {@code true}, labels switch to game-over text
     */
    public void renderLocal(Label currentPlayerLabel,
                            Label gameStatusText,
                            String playerName,
                            boolean hasDrawnThisTurn,
                            int remainingPlays,
                            int drawPileSize,
                            int discardPileSize,
                            boolean gameOver) {
        renderLocal(currentPlayerLabel, gameStatusText, playerName, hasDrawnThisTurn,
                remainingPlays, drawPileSize, discardPileSize, gameOver, -1);
    }

    /**
     * Updates local-game turn labels with an optional turn timer suffix.
     *
     * @param turnSecondsRemaining seconds left in the turn, or {@code -1} to hide the timer
     */
    public void renderLocal(Label currentPlayerLabel,
                            Label gameStatusText,
                            String playerName,
                            boolean hasDrawnThisTurn,
                            int remainingPlays,
                            int drawPileSize,
                            int discardPileSize,
                            boolean gameOver,
                            int turnSecondsRemaining) {
        if (currentPlayerLabel != null && playerName != null) {
            String timerSuffix = !gameOver && turnSecondsRemaining >= 0
                    ? " | Time: " + turnSecondsRemaining + "s"
                    : "";
            currentPlayerLabel.setText("Current Player: " + playerName + timerSuffix);
        }
        if (gameStatusText != null) {
            gameStatusText.setText(gameOver ? "Game over" : "");
        }
    }

    /**
     * Updates online-game turn labels without a turn timer.
     *
     * @param maxPlaysPerTurn maximum plays allowed after drawing
     * @param winnerName      winner display name shown when {@code gameOver} is {@code true}
     */
    public void renderOnline(Label currentPlayerLabel,
                             Label gameStatusText,
                             String playerName,
                             boolean hasDrawnThisTurn,
                             int remainingPlays,
                             int maxPlaysPerTurn,
                             int drawPileSize,
                             int discardPileSize,
                             boolean gameOver,
                             String winnerName) {
        renderOnline(currentPlayerLabel, gameStatusText, playerName, hasDrawnThisTurn,
                remainingPlays, maxPlaysPerTurn, drawPileSize, discardPileSize,
                gameOver, winnerName, -1);
    }

    /**
     * Updates online-game turn labels with winner text and optional turn timer.
     *
     * @param turnSecondsRemaining seconds left in the turn, or {@code -1} to hide the timer
     */
    public void renderOnline(Label currentPlayerLabel,
                             Label gameStatusText,
                             String playerName,
                             boolean hasDrawnThisTurn,
                             int remainingPlays,
                             int maxPlaysPerTurn,
                             int drawPileSize,
                             int discardPileSize,
                             boolean gameOver,
                             String winnerName,
                             int turnSecondsRemaining) {
        if (currentPlayerLabel != null && playerName != null) {
            String timerSuffix = !gameOver && turnSecondsRemaining >= 0
                    ? " | Time: " + turnSecondsRemaining + "s"
                    : "";
            currentPlayerLabel.setText("Current Player: " + playerName + timerSuffix);
        }
        if (gameStatusText != null) {
            gameStatusText.setText(gameOver ? "Winner: " + winnerName : "");
        }
    }
}
