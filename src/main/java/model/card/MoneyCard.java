package model.card;

import engine.GameEngine;
import model.enums.CardType;
import model.player.Player;

public class MoneyCard extends Card {
    private final int money;

    public MoneyCard(String name, String description, int money) {
        super(name, description, CardType.MONEY);
        this.money = money;
    }

    public int getMoney() {
        return money;
    }

    @Override
    public void use(Player player, GameEngine game) {
        System.out.println(player.getName() + " used money card: " + getName());
        System.out.println("Card effect: " + getDescription());
        System.out.println("Amount affected: " + money + " (Note: No money system implemented in Player class yet, printing effect only)");

        // Removed all calls to getMoney()/changeMoney()/playerBankrupt(), no dependency on extra Player methods
    }
}