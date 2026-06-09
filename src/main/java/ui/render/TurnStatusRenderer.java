package ui.render;

import engine.GameEngine;
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
            String drawStatus = hasDrawnThisTurn ? "Drew cards" : "Hasn't drawn";
            String timerSuffix = !gameOver && turnSecondsRemaining >= 0
                    ? " | Time: " + turnSecondsRemaining + "s"
                    : "";
            currentPlayerLabel.setText("Current Player: " + playerName
                    + " | " + drawStatus
                    + " | Remaining plays: " + remainingPlays + "/"
                    + GameEngine.MAX_PLAYS_PER_TURN
                    + timerSuffix);
        }
        if (gameStatusText != null && !gameOver) {
            gameStatusText.setText("Draw pile: " + drawPileSize
                    + "  |  Discard pile: " + discardPileSize);
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
            String drawStatus = hasDrawnThisTurn ? "Drew cards" : "Hasn't drawn";
            String timerSuffix = !gameOver && turnSecondsRemaining >= 0
                    ? " | Time: " + turnSecondsRemaining + "s"
                    : "";
            currentPlayerLabel.setText("Current Player: " + playerName
                    + " | " + drawStatus
                    + " | Remaining plays: " + remainingPlays + "/" + maxPlaysPerTurn
                    + timerSuffix);
        }
        if (gameStatusText != null) {
            if (gameOver) {
                gameStatusText.setText("Draw pile: " + drawPileSize
                        + "  |  Discard pile: " + discardPileSize
                        + "  |  Winner: " + winnerName);
            } else {
                gameStatusText.setText("Draw pile: " + drawPileSize
                        + "  |  Discard pile: " + discardPileSize);
            }
        }
    }
}
