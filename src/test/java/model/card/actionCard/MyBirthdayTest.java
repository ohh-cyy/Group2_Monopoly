package model.card.actionCard;

import engine.GameEngine;
import model.card.PropertyCard;
import model.enums.CardType;
import model.enums.Color;
import model.player.Player;
import org.junit.jupiter.api.Test;

import static model.card.actionCard.ActionCardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

class MyBirthdayTest {

    @Test
    void collectFromEveryoneCollectsTwoMillionFromEachOpponent() {
        Player birthdayPlayer = player("Birthday Player");
        Player firstOpponent = player("First Opponent");
        Player secondOpponent = player("Second Opponent");
        PropertyCard property = property("Pall Mall", Color.PINK, 1);
        firstOpponent.addBank(money(2));
        secondOpponent.addBank(money(1));
        secondOpponent.addProperty(property);
        GameEngine game = game(birthdayPlayer, firstOpponent, secondOpponent);
        MyBirthday birthday = new MyBirthday("It's My Birthday", "Collect 2M from everyone", CardType.ACTION);

        int collected = birthday.collectFromEveryone(birthdayPlayer, game);

        assertEquals(4, collected);
        assertEquals(3, birthdayPlayer.getBankTotalValue());
        assertTrue(birthdayPlayer.getAllProperties().contains(property));
        assertEquals(0, firstOpponent.getBankTotalValue());
        assertEquals(0, secondOpponent.getBankTotalValue());
        assertFalse(secondOpponent.getAllProperties().contains(property));
    }

    @Test
    void collectFromEveryoneSkipsBirthdayPlayer() {
        Player birthdayPlayer = player("Birthday Player");
        Player opponent = player("Opponent");
        birthdayPlayer.addBank(money(2));
        opponent.addBank(money(2));
        MyBirthday birthday = new MyBirthday("It's My Birthday", "Collect 2M from everyone", CardType.ACTION);

        int collected = birthday.collectFromEveryone(birthdayPlayer, game(birthdayPlayer, opponent));

        assertEquals(2, collected);
        assertEquals(4, birthdayPlayer.getBankTotalValue());
        assertEquals(0, opponent.getBankTotalValue());
    }

    @Test
    void useDelegatesToCollectFromEveryone() {
        Player birthdayPlayer = player("Birthday Player");
        Player opponent = player("Opponent");
        opponent.addBank(money(2));
        GameEngine game = game(birthdayPlayer, opponent);
        MyBirthday birthday = new MyBirthday("It's My Birthday", "Collect 2M from everyone", CardType.ACTION);

        birthday.use(birthdayPlayer, game);

        assertEquals(2, birthdayPlayer.getBankTotalValue());
        assertEquals(0, opponent.getBankTotalValue());
    }

    @Test
    void collectFromEveryoneReturnsZeroForNullInputs() {
        Player player = player("Player");
        MyBirthday birthday = new MyBirthday("It's My Birthday", "Collect 2M from everyone", CardType.ACTION);

        assertEquals(0, birthday.collectFromEveryone(null, game(player, player("Opponent"))));
        assertEquals(0, birthday.collectFromEveryone(player, null));
    }
}
