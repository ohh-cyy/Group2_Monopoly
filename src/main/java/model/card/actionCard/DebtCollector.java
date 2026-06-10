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
     * Collects 5M from one chosen opponent. If the opponent cannot pay the full
     * amount, bank cards are taken first, then properties.
     *
     * @param collector player receiving the payment
     * @param target    opponent who owes the debt
     * @return amount actually collected in millions (M)
     */
    public int collectFrom(Player collector, Player target) {
        if (collector == null || target == null || target.equals(collector)) {
            return 0;
        }
        return RentPayment.collectUpTo(collector, target, DEBT_AMOUNT);
    }
}
