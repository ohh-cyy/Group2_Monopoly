package model.card.actionCard;

import engine.GameEngine;
import engine.RentPayment;
import model.enums.CardType;
import model.player.Player;

public class MyBirthday extends ActionCard {

    public static final int GIFT_AMOUNT = 2;

    public MyBirthday(String name, String description, CardType type) {
        super(name, description, type, 2);
    }

    public MyBirthday(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 2);
    }

    /** Collects 2M from every other player immediately. */
    @Override
    public void use(Player player, GameEngine game) {
        collectFromEveryone(player, game);
    }

    /**
     * Collects 2M from every other player. If a player cannot pay the full
     * amount, bank cards are taken first, then properties.
     *
     * @return the total amount collected across all opponents
     */
    public int collectFromEveryone(Player birthdayPlayer, GameEngine game) {
        if (birthdayPlayer == null || game == null) {
            return 0;
        }

        int total = 0;
        for (Player other : game.getPlayers()) {
            if (other.equals(birthdayPlayer)) {
                continue;
            }
            total += RentPayment.collectUpTo(birthdayPlayer, other, GIFT_AMOUNT);
        }
        return total;
    }
}
