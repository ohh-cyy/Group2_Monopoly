package model.card.actionCard;

import engine.GameEngine;
import engine.RentPayment;
import model.enums.CardType;
import model.player.Player;

public class DebtCollector extends ActionCard {

    public static final int DEBT_AMOUNT = 5;

    public DebtCollector(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

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
     */
    public int collectFrom(Player collector, Player target) {
        if (collector == null || target == null || target.equals(collector)) {
            return 0;
        }
        return RentPayment.collectUpTo(collector, target, DEBT_AMOUNT);
    }
}
