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

    /** 每人收2m*/
    @Override
    public void use(Player player, GameEngine game) {
        collectFromEveryone(player, game);
    }

    /**
     * 每人收2m，如果玩家无法支付，则先支付银行卡片，然后支付property
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
