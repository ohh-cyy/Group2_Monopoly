package model.card.actionCard;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class DebtCollector extends ActionCard {

    public DebtCollector(String name, String description, CardType ACTION) {
        super(name, description, ACTION);
    }

    @Override
    public void use(Player player, GameEngine game) {
        Player target = game.getDefaultDefender(player);
        
        int debtAmount = 5;
        
        if (target.hasEnoughMoney(debtAmount)) {
            target.removeMoney(debtAmount);
            player.addMoney(debtAmount);
            System.out.println(player.getName() + " used " + getName() +
                " From " + target.getName() + " collected " + debtAmount + " $!");
        } else {
            int availableMoney = target.getMoney();
            target.removeMoney(availableMoney);
            player.addMoney(availableMoney);
            System.out.println(player.getName() + " used " + getName() +
                " From " + target.getName() + " collected " + availableMoney + " $!");
        }
    }
}
