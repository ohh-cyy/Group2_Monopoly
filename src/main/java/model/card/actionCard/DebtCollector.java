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

    @Override
    public void use(Player player, GameEngine game) {
        // The controller chooses the target player and then calls collectFrom.
    }

    /**
     * Collects 5M from one chosen opponent. If the opponent cannot pay the full
     * amount, the method collects as much as possible.
     */
    public int collectFrom(Player collector, Player target) {
        if (collector == null || target == null || target.equals(collector)) {
            return 0;
        }
        return RentPayment.collectUpTo(collector, target, DEBT_AMOUNT);
    }
}
