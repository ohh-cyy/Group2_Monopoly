package controller.view;

import engine.GameEngine;
import model.card.Card;
import model.player.Player;
import sync.PlayerPublicSnapshot;

import java.util.List;

/**
 * Read-only data needed to refresh the game screen.
 */
public record GameViewState(
        GameEngine gameEngine,
        Player currentPlayer,
        List<Card> handCards,
        List<Card> bankCards,
        int bankTotal,
        List<PlayerPublicSnapshot> publicPlayers,
        int currentTurnSeat,
        String currentTurnName,
        boolean clientMode,
        boolean hostMode,
        boolean gameOver,
        String winnerName,
        boolean hasDrawnThisTurn,
        int remainingPlays,
        int maxPlaysPerTurn,
        int drawPileSize,
        int discardPileSize,
        boolean canDraw,
        boolean canPlayCard,
        boolean canEndTurn,
        boolean handClickable,
        Card selectedCard
) {
}
