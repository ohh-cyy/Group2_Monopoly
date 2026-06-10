package model.card.actionCard;

import engine.GameEngine;
import engine.RentPayment;
import model.enums.CardType;
import model.player.Player;

/**
 * Action card that collects a small gift from every opponent.
 * Each other player pays {@link #GIFT_AMOUNT} million (M).
 */
public class MyBirthday extends ActionCard {

    /** Amount collected from each opponent in millions (M). */
    public static final int GIFT_AMOUNT = 2;

    /**
     * Creates a My Birthday card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public MyBirthday(String name, String description, CardType type) {
        super(name, description, type, 2);
    }

    /**
     * Creates a My Birthday card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
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
     * @param birthdayPlayer player celebrating their birthday (collector)
     * @param game           game engine providing the player list
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
