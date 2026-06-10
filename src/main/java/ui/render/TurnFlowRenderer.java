package ui.render;

import engine.GameEngine;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Renders turn-flow hint text and a remaining-play dot indicator.
 * <p>
 * Hint copy walks the player through draw, play, and end-turn phases;
 * dots reflect how many of the three plays have been used.
 */
public final class TurnFlowRenderer {
    private static final int WIN_SETS = 3;

    private TurnFlowRenderer() {
    }

    /**
     * Updates turn-flow hint text and the remaining-play dot indicator.
     *
     * @param maxPlaysPerTurn maximum plays allowed after drawing
     */
    public static void render(Label turnFlowLabel,
                              HBox playDotsBar,
                              boolean hasDrawnThisTurn,
                              int remainingPlays,
                              int maxPlaysPerTurn,
                              boolean gameOver) {
        if (turnFlowLabel != null) {
            turnFlowLabel.setText(buildFlowText(hasDrawnThisTurn, remainingPlays, gameOver));
        }
        renderPlayDots(playDotsBar, hasDrawnThisTurn, remainingPlays, maxPlaysPerTurn, gameOver);
    }

    private static String buildFlowText(boolean hasDrawnThisTurn, int remainingPlays, boolean gameOver) {
        if (gameOver) {
            return "Game over";
        }
        if (!hasDrawnThisTurn) {
            return "① Draw 2 cards   →   ② Play up to 3   →   ③ End turn";
        }
        if (remainingPlays > 0) {
            return "② Play up to " + remainingPlays + " more   →   ③ End turn";
        }
        return "③ End turn (discard down to 7 if needed)";
    }

    private static void renderPlayDots(HBox playDotsBar,
                                       boolean hasDrawnThisTurn,
                                       int remainingPlays,
                                       int maxPlaysPerTurn,
                                       boolean gameOver) {
        if (playDotsBar == null) {
            return;
        }
        playDotsBar.getChildren().clear();
        playDotsBar.setAlignment(Pos.CENTER_RIGHT);

        Label prefix = new Label("Plays ");
        prefix.getStyleClass().add("play-dots-prefix");
        playDotsBar.getChildren().add(prefix);

        if (gameOver || !hasDrawnThisTurn) {
            Label idle = new Label("—");
            idle.getStyleClass().add("play-dot-count");
            playDotsBar.getChildren().add(idle);
            return;
        }

        int max = Math.max(1, maxPlaysPerTurn);
        int used = Math.max(0, max - remainingPlays);
        for (int i = 0; i < max; i++) {
            Label dot = new Label(i < used ? "●" : "○");
            dot.getStyleClass().add(i < used ? "play-dot-used" : "play-dot-open");
            playDotsBar.getChildren().add(dot);
        }
        Label count = new Label(" " + used + "/" + max);
        count.getStyleClass().add("play-dot-count");
        playDotsBar.getChildren().add(count);

        Label winHint = new Label("   |   Win: " + WIN_SETS + " full sets");
        winHint.getStyleClass().add("play-dots-prefix");
        playDotsBar.getChildren().add(winHint);
    }

    /** Returns the default maximum plays per turn from the game engine. */
    public static int defaultMaxPlays() {
        return GameEngine.MAX_PLAYS_PER_TURN;
    }
}
