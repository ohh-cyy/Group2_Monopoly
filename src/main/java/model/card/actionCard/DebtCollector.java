package model.card.actionCard;

import engine.GameEngine;
import engine.RentPayment;
import model.enums.CardType;
import model.player.Player;

/**
 * Action card that collects a large debt payment from a single opponent.
 * The target pays {@link #DEBT_AMOUNT} million (M) if able.
 */
public class DebtCollector extends ActionCard {

    /** Amount collected from the chosen opponent in millions (M). */
    public static final int DEBT_AMOUNT = 5;

    /**
     * Creates a Debt Collector card with a randomly generated instance id.
     *
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public DebtCollector(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    /**
     * Creates a Debt Collector card with an explicit instance id.
     *
     * @param instanceId  unique card instance id
     * @param name        display name
     * @param description rules text
     * @param type        card category (always {@link CardType#ACTION})
     */
    public DebtCollector(String instanceId, String name, String description, CardType type) {
        super(instanceId, name, description, type, 3);
    }

    /**
     * Empty by design — the controller picks the target via UI and calls
     * {@link #collectFrom}.
     */
    @Override
    public void use(Player player, GameEngine game) {
    }

    /**
     * 向一个人收5m，如果玩家无法支付，则先支付银行卡片，然后支付property
     */
    public int collectFrom(Player collector, Player target) {
        if (collector == null || target == null || target.equals(collector)) {
            return 0;
        }
        return RentPayment.collectUpTo(collector, target, DEBT_AMOUNT);
    }
}
