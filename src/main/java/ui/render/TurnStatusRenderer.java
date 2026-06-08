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
        if (currentPlayerLabel != null && playerName != null) {
            String drawStatus = hasDrawnThisTurn ? "Drew cards" : "Hasn't drawn";
            currentPlayerLabel.setText("Current Player: " + playerName
                    + " | " + drawStatus
                    + " | Remaining plays: " + remainingPlays + "/"
                    + GameEngine.MAX_PLAYS_PER_TURN);
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
        if (currentPlayerLabel != null && playerName != null) {
            String drawStatus = hasDrawnThisTurn ? "Drew cards" : "Hasn't drawn";
            currentPlayerLabel.setText("Current Player: " + playerName
                    + " | " + drawStatus
                    + " | Remaining plays: " + remainingPlays + "/" + maxPlaysPerTurn);
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
