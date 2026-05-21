package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class DebtCollector extends ActionCard {

    private static final int DEBT_AMOUNT = 5;

    public DebtCollector(String name, String description, CardType type) {
        super(name, description, type, 3);
    }

    @Override
    public void use(Player player, GameEngine game) {
        collectFrom(player, game.getDefaultDefender(player));
    }

    public void collectFrom(Player player, Player target) {
        if (target == null || target.equals(player)) {
            return;
        }
        int paid = Math.min(DEBT_AMOUNT, target.getMoney());
        target.removeMoney(paid);
        player.addMoney(paid);
    }
}
