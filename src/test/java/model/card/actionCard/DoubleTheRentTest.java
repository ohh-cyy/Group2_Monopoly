package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static model.card.actionCard.ActionCardTestSupport.game;
import static model.card.actionCard.ActionCardTestSupport.player;
import static org.junit.jupiter.api.Assertions.*;

class DoubleTheRentTest {

    @Test
    void activateForNextRentSetsGameRentDoubledFlag() {
        Player player = player("Player");
        GameEngine game = game(player, player("Opponent"));
        DoubleTheRent doubleTheRent = new DoubleTheRent("Double The Rent", "Double next rent", CardType.ACTION);

        assertTrue(doubleTheRent.activateForNextRent(game));
        assertTrue(game.isRentDoubled());
    }

    @Test
    void activateForNextRentCannotStackBeforeRentIsPlayed() {
        Player player = player("Player");
        GameEngine game = game(player, player("Opponent"));
        DoubleTheRent doubleTheRent = new DoubleTheRent("Double The Rent", "Double next rent", CardType.ACTION);

        assertTrue(doubleTheRent.activateForNextRent(game));
        assertFalse(doubleTheRent.activateForNextRent(game));
        assertTrue(game.isRentDoubled());
    }

    @Test
    void useActivatesDoubleRent() {
        Player player = player("Player");
        GameEngine game = game(player, player("Opponent"));
        DoubleTheRent doubleTheRent = new DoubleTheRent("Double The Rent", "Double next rent", CardType.ACTION);

        doubleTheRent.use(player, game);

        assertTrue(game.isRentDoubled());
    }

    @Test
    void activateForNextRentRejectsNullGame() {
        DoubleTheRent doubleTheRent = new DoubleTheRent("Double The Rent", "Double next rent", CardType.ACTION);

        assertFalse(doubleTheRent.activateForNextRent(null));
        assertDoesNotThrow(() -> doubleTheRent.use(player("Player"), null));
    }
}
