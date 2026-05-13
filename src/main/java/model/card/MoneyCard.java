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
        player.addMoney(money);
        game.getDiscardPile().addCard(this);
    }
}