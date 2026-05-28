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
        // 由 GameController 选择目标后调用 collectFrom
    }

    public int collectFrom(Player collector, Player target) {
        if (target == null || target.equals(collector)) {
            return 0;
        }
        return RentPayment.collectUpTo(collector, target, DEBT_AMOUNT);
    }
}
