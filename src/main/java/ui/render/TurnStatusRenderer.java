package ui.render;

import javafx.scene.control.Label;

/** Updates turn summary and draw/discard pile labels. */
public final class TurnStatusRenderer {
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
